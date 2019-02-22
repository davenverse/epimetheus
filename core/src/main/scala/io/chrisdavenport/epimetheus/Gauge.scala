package io.chrisdavenport.epimetheus

import cats.implicits._
import cats.effect._
import io.prometheus.client.{Gauge => JGauge}

// import scala.concurrent.duration.MILLISECONDS


final class Gauge[F[_]: Sync] private (private val g: JGauge.Child){
  def dec: F[Unit] = Sync[F].delay(g.dec())
  def decBy(d: Double): F[Unit] = Sync[F].delay(g.dec(d))
  
  def inc: F[Unit] = Sync[F].delay(g.inc())
  def incBy(d: Double): F[Unit] = Sync[F].delay(g.inc(d))

  def set(d: Double): F[Unit] = Sync[F].delay(g.set(d))

}
object Gauge {

  def build[F[_]: Sync](cr: CollectorRegistry, name: String, help: String): F[Gauge[F]] = for {
    c <- Sync[F].delay(JGauge.build().name(name).help(help))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new SafeUnlabelledGauge[F, Unit](out, _ => List.empty).label(())

    /**
   * Labels and the string returned by f MUST have the same size
   *  or else `label` will fail
   * FUTURE IMPROVEMENT: Size these lists to make this safe.
   */
  def construct[F[_]: Sync, A](
    cr: CollectorRegistry, 
    name: String, 
    help: String, 
    labels: List[String], 
    f: A => List[String]
  ): F[UnlabelledGauge[F, A]] = for {
      c <- Sync[F].delay(JGauge.build().name(name).help(help).labelNames(labels:_*))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new UnlabelledGauge[F, A](out, f)


    /**
   * Generic Unlabeled Gauge
   * 
   * Unsafe Conversion as labels may not align so `label` operation
   * can fail
   */
  final class UnlabelledGauge[F[_]: Sync, A] private[epimetheus](
    private val c: JGauge, 
    private val f: A => List[String]
  ) {
    def label(a: A): F[Gauge[F]] =
      Sync[F].delay(c.labels(f(a):_*)).map(new Gauge[F](_))
  }

  final class SafeUnlabelledGauge[F[_]: Sync, A] private[epimetheus](
    private val c: JGauge, 
    private val f: A => List[String]
  ) {
    def label(a: A): Gauge[F] = new Gauge[F](c.labels(f(a):_*))
  }
}