package io.chrisdavenport.epimetheus

import cats._
import cats.implicits._
import cats.effect._
import io.prometheus.client.{Gauge => JGauge}
import shapeless._

sealed abstract class Gauge[F[_]]{
  def get: F[Double]

  def dec: F[Unit]
  def decBy(d: Double): F[Unit]
  
  def inc: F[Unit]
  def incBy(d: Double): F[Unit]

  def set(d: Double): F[Unit]
}

object Gauge {

  /**
   * Construct a Guage with No Labels
   */
  def build[F[_]: Sync](cr: CollectorRegistry[F], name: String, help: String): F[Gauge[F]] = for {
    c <- Sync[F].delay(JGauge.build().name(name).help(help))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new NoLabelsGauge[F](out)

  /**
   * Construct a Safe Gauge that guarantees the correct number of labels
   */
  def construct[F[_]: Sync, A, N <: Nat](
    cr: CollectorRegistry[F], 
    name: String, 
    help: String, 
    labels: Sized[IndexedSeq[String], N], 
    f: A => Sized[IndexedSeq[String], N]
  ): F[UnlabelledGauge[F, A]] = for {
      c <- Sync[F].delay(JGauge.build().name(name).help(help).labelNames(labels:_*))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new UnlabelledGauge[F, A](out, f.andThen(_.unsized))


  private final class NoLabelsGauge[F[_]: Sync] private[Gauge] (
    private[Gauge] val underlying: JGauge
  ) extends Gauge[F] {
    def get: F[Double] = Sync[F].delay(underlying.get())
  
    def dec: F[Unit] = Sync[F].delay(underlying.dec())
    def decBy(d: Double): F[Unit] = Sync[F].delay(underlying.dec(d))
    
    def inc: F[Unit] = Sync[F].delay(underlying.inc())
    def incBy(d: Double): F[Unit] = Sync[F].delay(underlying.inc(d))
  
    def set(d: Double): F[Unit] = Sync[F].delay(underlying.set(d))
  }
  
  private final class LabelledGauge[F[_]: Sync] private[Gauge] (
    private val underlying: JGauge.Child
  ) extends Gauge[F] {
    def get: F[Double] = Sync[F].delay(underlying.get())
  
    def dec: F[Unit] = Sync[F].delay(underlying.dec())
    def decBy(d: Double): F[Unit] = Sync[F].delay(underlying.dec(d))
    
    def inc: F[Unit] = Sync[F].delay(underlying.inc())
    def incBy(d: Double): F[Unit] = Sync[F].delay(underlying.inc(d))
  
    def set(d: Double): F[Unit] = Sync[F].delay(underlying.set(d))
  }



  /**
   * Generic Unlabeled Gauge
   * 
   * Unsafe Conversion as labels may not align so `label` operation
   * can fail
   */
  final class UnlabelledGauge[F[_]: Sync, A] private[epimetheus](
    private[Gauge] val underlying: JGauge, 
    private val f: A => IndexedSeq[String]
  ) {
    def label(a: A): Gauge[F] =
      new LabelledGauge[F](underlying.labels(f(a):_*))
  }
  object Unsafe {
    def asJavaUnlabelled[F[_], A](g: UnlabelledGauge[F, A]): JGauge = g.underlying
    def asJava[F[_]: ApplicativeError[?[_], Throwable]](c: Gauge[F]): F[JGauge] = c match {
      case _: LabelledGauge[F] => ApplicativeError[F, Throwable].raiseError(new IllegalArgumentException("Cannot Get Underlying Parent with Labels Applied"))
      case n: NoLabelsGauge[F] => n.underlying.pure[F]
    }
  }
}