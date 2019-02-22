package io.chrisdavenport.epimetheus

import cats.implicits._
import cats.effect._
import io.prometheus.client.{Counter => JCounter}
import shapeless._

/**
  * Counter - Track counts, running totals, or events.
  *
  * If your use case can go up or down consider using a [[Gauge]] instead.
  *
  * By convention, the names of Counters are suffixed by <code>_total</code>.
  */
sealed abstract class Counter[F[_]]{
  def get: F[Double]
  def inc: F[Unit]
  def incBy(d: Double): F[Unit]
}
object Counter {

  /**
   * Only Valid Constructor for NoLabelsCounter
   */
  def build[F[_]: Sync](cr: CollectorRegistry[F], name: String, help: String): F[Counter[F]] = for {
    c <- Sync[F].delay(JCounter.build().name(name).help(help))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new NoLabelsCounter[F](out)

  /**
   * Only Valid Constructor for UnlabelledCounter
   */
  def construct[F[_]: Sync, A, N <: Nat](
    cr: CollectorRegistry[F], 
    name: String, 
    help: String, 
    labels: Sized[IndexedSeq[String], N], 
    f: A => Sized[IndexedSeq[String], N]
  ): F[UnlabelledCounter[F, A]] = for {
      c <- Sync[F].delay(JCounter.build().name(name).help(help).labelNames(labels:_*))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new UnlabelledCounter[F, A](out, f.andThen(_.unsized))


  private[epimetheus] final class LabelledCounter[F[_]: Sync] private[Counter] (private val underlying: JCounter.Child) extends Counter[F] {
    override def get: F[Double] = Sync[F].delay(underlying.get)
  
    def inc: F[Unit] = Sync[F].delay(underlying.inc)
    def incBy(d: Double): F[Unit] = Sync[F].delay(underlying.inc(d))
  }
  private[epimetheus] final class NoLabelsCounter[F[_]: Sync] private[Counter] (private[Counter] val underlying: JCounter) extends Counter[F] {
    override def get: F[Double] = Sync[F].delay(underlying.get)
  
    override def inc: F[Unit] = Sync[F].delay(underlying.inc)
    override def incBy(d: Double): F[Unit] = Sync[F].delay(underlying.inc(d))
  }

  /**
   * Generic Unlabeled Counter
   * 
   * Unsafe Conversion as labels may not align so `label` operation
   * can fail
   */
  final class UnlabelledCounter[F[_]: Sync, A] private[Counter](
    private[Counter] val underlying: JCounter, 
    private val f: A => IndexedSeq[String]
  ) {
    def label(a: A): Counter[F] =
      new LabelledCounter(underlying.labels(f(a):_*))
  }

  object Unsafe {
    def asJavaNoLabels[F[_]](c: NoLabelsCounter[F]): JCounter = c.underlying
    def asJavaUnlabelled[F[_], A](c: UnlabelledCounter[F, A]): JCounter = c.underlying
  }
}