package io.chrisdavenport.epimetheus

import cats.implicits._
import cats.effect._
import cats.implicits._
import io.prometheus.client.{Histogram => JHistogram}
import scala.concurrent.duration.TimeUnit

final class Histogram[F[_]: Sync: Clock] private (private val h: JHistogram.Child){
  def observe(d: Double): F[Unit] = Sync[F].delay(h.observe(d))
  def timed[A](fa: F[A], unit: TimeUnit): F[A] = for {
    start <- Clock[F].monotonic(unit)
    out <- Sync[F].guarantee(fa)(Clock[F].monotonic(unit).flatMap(now => observe((now - start).toDouble)))
  } yield out
}

object Histogram {

  def buildBuckets[F[_]: Sync: Clock](cr: CollectorRegistry, name: String, help: String, buckets: Double*): F[Histogram[F]] = for {
    c <- Sync[F].delay(JHistogram.build().name(name).help(help).buckets(buckets:_*))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new SafeUnlabelledHistogram[F, Unit](out, _ => List.empty).label(())


  /**
   * Labels and the string returned by f MUST have the same size
   *  or else `label` will fail
   * FUTURE IMPROVEMENT: Size these lists to make this safe.
   */
  def construct[F[_]: Sync: Clock, A](
    cr: CollectorRegistry, 
    name: String, 
    help: String, 
    labels: List[String], 
    f: A => List[String],
    buckets: Double*
  ): F[UnlabelledHistogram[F, A]] = for {
      c <- Sync[F].delay(JHistogram.build().name(name).help(help).labelNames(labels:_*).buckets(buckets:_*))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new UnlabelledHistogram[F, A](out, f)


  /**
   * Generic Unlabeled Histogram
   * 
   * Unsafe Conversion as labels may not align so `label` operation
   * can fail
   */
  final class UnlabelledHistogram[F[_]: Sync: Clock, A] private[epimetheus](
    private val c: JHistogram, 
    private val f: A => List[String]
  ) {
    def label(a: A): F[Histogram[F]] =
      Sync[F].delay(c.labels(f(a):_*)).map(new Histogram[F](_))
  }

  final class SafeUnlabelledHistogram[F[_]: Sync: Clock, A] private[epimetheus](
    private val c: JHistogram, 
    private val f: A => List[String]
  ) {
    def label(a: A): Histogram[F] = new Histogram[F](c.labels(f(a):_*))
  }

}