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
  } yield new UnlabelledHistogram[F, Unit](out, _ => IndexedSeq()).label(())

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


  final class LabelledHistogram[F[_]: Sync: Clock] private[Histogram] (
    private val underlying: JHistogram.Child
  ) extends Histogram[F] {
    def observe(d: Double): F[Unit] = Sync[F].delay(underlying.observe(d))
    def timed[A](fa: F[A], unit: TimeUnit): F[A] = 
      Sync[F].bracket(Clock[F].monotonic(unit))
        {_: Long => fa}
        {start: Long => Clock[F].monotonic(unit).flatMap(now => observe((now - start).toDouble))}
  }

  final class UnlabelledHistogram[F[_]: Sync: Clock, A] private[Histogram] (
    private val c: JHistogram, 
    private val f: A => IndexedSeq[String]
  ) {
    def label(a: A): Histogram[F] = new LabelledHistogram[F](c.labels(f(a):_*))
  }

}