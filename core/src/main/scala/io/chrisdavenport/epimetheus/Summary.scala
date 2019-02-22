package io.chrisdavenport.epimetheus

import cats.implicits._
import cats.effect._
import io.prometheus.client.{Summary => JSummary}
import scala.concurrent.duration._

final class Summary[F[_]: Sync: Clock] private (private val s: JSummary.Child){
  def observe(d: Double): F[Unit] = Sync[F].delay(s.observe(d))
  def timed[A](fa: F[A], unit: TimeUnit): F[A] = for {
    start <- Clock[F].monotonic(unit)
    out <- Sync[F].guarantee(fa)(Clock[F].monotonic(unit).flatMap(now => observe((now - start).toDouble)))
  } yield out
}

object Summary {
  def buildQuantiles[F[_]: Sync: Clock](cr: CollectorRegistry, name: String, help: String, quantiles: (Double, Double)*): F[Summary[F]] = for {
    c1 <- Sync[F].delay(JSummary.build().name(name).help(help))
    c <- Sync[F].delay(quantiles.foldLeft(c1){ case (c, q) => c.quantile(q._1, q._2)})
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new SafeUnlabelledSummary[F, Unit](out, _ => List.empty).label(())


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
    quantiles: (Double, Double)*
  ): F[UnlabelledSummary[F, A]] = for {
    c1 <- Sync[F].delay(JSummary.build().name(name).help(help).labelNames(labels:_*))
    c <- Sync[F].delay(quantiles.foldLeft(c1){ case (c, q) => c.quantile(q._1, q._2)})
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new UnlabelledSummary[F, A](out, f)


  /**
   * Generic Unlabeled Summary
   * 
   * Unsafe Conversion as labels may not align so `label` operation
   * can fail
   */
  final class UnlabelledSummary[F[_]: Sync: Clock, A] private[epimetheus](
    private val c: JSummary, 
    private val f: A => List[String]
  ) {
    def label(a: A): F[Summary[F]] =
      Sync[F].delay(c.labels(f(a):_*)).map(new Summary[F](_))
  }

  final class SafeUnlabelledSummary[F[_]: Sync: Clock, A] private[epimetheus](
    private val c: JSummary, 
    private val f: A => List[String]
  ) {
    def label(a: A): Summary[F] = new Summary[F](c.labels(f(a):_*))
  }

}