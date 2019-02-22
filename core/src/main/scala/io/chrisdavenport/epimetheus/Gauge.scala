package io.chrisdavenport.epimetheus

import cats.implicits._
import cats.effect._
import io.prometheus.client.{Gauge => JGauge}
import shapeless._

final class Gauge[F[_]: Sync] private (private val underlying: JGauge.Child){
  def get: F[Double] = Sync[F].delay(underlying.get())

  def dec: F[Unit] = Sync[F].delay(underlying.dec())
  def decBy(d: Double): F[Unit] = Sync[F].delay(underlying.dec(d))
  
  def inc: F[Unit] = Sync[F].delay(underlying.inc())
  def incBy(d: Double): F[Unit] = Sync[F].delay(underlying.inc(d))

  def set(d: Double): F[Unit] = Sync[F].delay(underlying.set(d))

}
object Gauge {

  def build[F[_]: Sync](cr: CollectorRegistry[F], name: String, help: String): F[Gauge[F]] = for {
    c <- Sync[F].delay(JGauge.build().name(name).help(help))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new UnlabelledGauge[F, Unit](out, _ => IndexedSeq()).label(())

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
      new Gauge[F](underlying.labels(f(a):_*))
  }
  object Unsafe {
    def asJavaLabelled[F[_]](g: Gauge[F]): JGauge.Child = g.underlying
    def asJavaUnlabelled[F[_], A](g: UnlabelledGauge[F, A]): JGauge = g.underlying
  }
}