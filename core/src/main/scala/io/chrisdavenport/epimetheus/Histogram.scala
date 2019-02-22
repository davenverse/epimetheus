package io.chrisdavenport.epimetheus

import cats.implicits._
import cats.effect._
import cats.implicits._
import io.prometheus.client.{Histogram => JHistogram}
import scala.concurrent.duration.TimeUnit
import shapeless._

sealed abstract class Histogram[F[_]]{
  def observe(d: Double): F[Unit]
  def timed[A](fa: F[A], unit: TimeUnit): F[A]
}

object Histogram {
  // Future Improvements Make Buckets Logic Cleaner

  def buildBuckets[F[_]: Sync: Clock](cr: CollectorRegistry[F], name: String, help: String, buckets: Double*): F[Histogram[F]] = for {
    c <- Sync[F].delay(JHistogram.build().name(name).help(help).buckets(buckets:_*))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new NoLabelsHistogram[F](out)

  def buildLinearBuckets[F[_]: Sync: Clock](cr: CollectorRegistry[F], name: String, help: String, start: Double, factor: Double, count: Int): F[Histogram[F]] = for {
    c <- Sync[F].delay(JHistogram.build().name(name).help(help).linearBuckets(start, factor, count))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new NoLabelsHistogram[F](out)

  def buildExponentialBuckets[F[_]: Sync: Clock](cr: CollectorRegistry[F], name: String, help: String, start: Double, factor: Double, count: Int): F[Histogram[F]] = for {
    c <- Sync[F].delay(JHistogram.build().name(name).help(help).exponentialBuckets(start, factor, count))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new NoLabelsHistogram[F](out)

  def construct[F[_]: Sync: Clock, A, N <: Nat](
    cr: CollectorRegistry[F], 
    name: String, 
    help: String, 
    labels: Sized[IndexedSeq[String], N], 
    f: A => Sized[IndexedSeq[String], N],
    buckets: Double*
  ): F[UnlabelledHistogram[F, A]] = for {
    c <- Sync[F].delay(JHistogram.build().name(name).help(help).labelNames(labels:_*).buckets(buckets:_*))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new UnlabelledHistogram[F, A](out, f.andThen(_.unsized))

  private final class NoLabelsHistogram[F[_]: Sync: Clock] private[Histogram] (
    private val underlying: JHistogram
  ) extends Histogram[F] {
    def observe(d: Double): F[Unit] = Sync[F].delay(underlying.observe(d))
    def timed[A](fa: F[A], unit: TimeUnit): F[A] = 
      Sync[F].bracket(Clock[F].monotonic(unit))
        {_: Long => fa}
        {start: Long => Clock[F].monotonic(unit).flatMap(now => observe((now - start).toDouble))}
  }

  private final class LabelledHistogram[F[_]: Sync: Clock] private[Histogram] (
    private val underlying: JHistogram.Child
  ) extends Histogram[F] {
    def observe(d: Double): F[Unit] = Sync[F].delay(underlying.observe(d))
    def timed[A](fa: F[A], unit: TimeUnit): F[A] = 
      Sync[F].bracket(Clock[F].monotonic(unit))
        {_: Long => fa}
        {start: Long => Clock[F].monotonic(unit).flatMap(now => observe((now - start).toDouble))}
  }

  final class UnlabelledHistogram[F[_]: Sync: Clock, A] private[Histogram] (
    private[Histogram] val underlying: JHistogram, 
    private val f: A => IndexedSeq[String]
  ) {
    def label(a: A): Histogram[F] = 
      new LabelledHistogram[F](underlying.labels(f(a):_*))
  }

  object Unsafe {
    def asJavaUnlabelled[F[_], A](h: UnlabelledHistogram[F, A]): JHistogram = 
      h.underlying
  }

}