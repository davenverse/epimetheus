package io.chrisdavenport.epimetheus

import cats._
import cats.implicits._
import cats.effect._
import io.prometheus.client.{Summary => JSummary}
import scala.concurrent.duration._
import shapeless._

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

sealed abstract class Summary[F[_]]{
  def observe(d: Double): F[Unit]
  def timed[A](fa: F[A], unit: TimeUnit): F[A]
}

object Summary {
  def buildQuantiles[F[_]: Sync: Clock](cr: CollectorRegistry[F], name: String, help: String, quantiles: Quantile*): F[Summary[F]] = for {
    c1 <- Sync[F].delay(JSummary.build().name(name).help(help))
    c <- Sync[F].delay(quantiles.foldLeft(c1){ case (c, q) => c.quantile(q.quantile, q.error)})
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new NoLabelsSummary[F](out)

  /**
   * 
   */
  def constructQuantiles[F[_]: Sync: Clock, A, N <: Nat](
    cr: CollectorRegistry[F], 
    name: String, 
    help: String, 
    labels: Sized[IndexedSeq[String], N], 
    f: A => Sized[IndexedSeq[String], N],
    quantiles: Quantile*
  ): F[UnlabelledSummary[F, A]] = for {
    c1 <- Sync[F].delay(JSummary.build().name(name).help(help).labelNames(labels:_*))
    c <- Sync[F].delay(quantiles.foldLeft(c1){ case (c, q) => c.quantile(q.quantile, q.error)})
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new UnlabelledSummary[F, A](out, f.andThen(_.unsized))

  final private class NoLabelsSummary[F[_]: Sync: Clock] private[Summary] (
    private[Summary] val underlying: JSummary
  ) extends Summary[F] {
    def observe(d: Double): F[Unit] = Sync[F].delay(underlying.observe(d))
    def timed[A](fa: F[A], unit: TimeUnit): F[A] = 
      Sync[F].bracket(Clock[F].monotonic(unit))
        {_ => fa}
        {start: Long => Clock[F].monotonic(unit).flatMap(now => observe((now - start).toDouble))}
  }
  final private class LabelledSummary[F[_]: Sync: Clock] private[Summary] (
    private val underlying: JSummary.Child
  ) extends Summary[F] {
    def observe(d: Double): F[Unit] = Sync[F].delay(underlying.observe(d))
    def timed[A](fa: F[A], unit: TimeUnit): F[A] = 
      Sync[F].bracket(Clock[F].monotonic(unit))
        {_ => fa}
        {start: Long => Clock[F].monotonic(unit).flatMap(now => observe((now - start).toDouble))}
  }

  /**
   * Generic Unlabeled Summary
   * 
   * Unsafe Conversion as labels may not align so `label` operation
   * can fail
   */
  final class UnlabelledSummary[F[_]: Sync: Clock, A] private[epimetheus](
    private[Summary] val underlying: JSummary, 
    private val f: A => IndexedSeq[String]
  ) {
    def label(a: A): Summary[F] =
      new LabelledSummary[F](underlying.labels(f(a):_*))
  }

  final class Quantile private(val quantile: Double, val error: Double)
  object Quantile {
    private class Macros(val c: whitebox.Context) {
      import c.universe._
      def quantileLiteral(quantile: c.Expr[Double], error: c.Expr[Double]): Tree =
        (quantile.tree, error.tree) match {
          case (Literal(Constant(q: Double)), Literal(Constant(e: Double))) =>
              impl(q, e)
              .fold(
                e => c.abort(c.enclosingPosition, e.getMessage),
                _ =>
                  q"_root_.io.chrisdavenport.epimetheus.Summary.Quantile.impl($q, $e).fold(throw _, _root_.scala.Predef.identity)"
              )
          case _ =>
            c.abort(
              c.enclosingPosition,
              s"This method uses a macro to verify that a Quantile literal is a valid Quantile. Use Quantile.impl if you have a dynamic set that you want to parse as a Quantile."
            )
        }
  }

    def impl(quantile: Double, error: Double): Either[IllegalArgumentException, Quantile] = {
      if (quantile < 0.0 || quantile > 1.0) Either.left(new IllegalArgumentException("Quantile " + quantile + " invalid: Expected number between 0.0 and 1.0."))
      else if (error < 0.0 || error > 1.0) Either.left(new IllegalArgumentException("Error " + error + " invalid: Expected number between 0.0 and 1.0."))
      else Either.right(new Quantile(quantile, error))
    }

    def implF[F[_]: ApplicativeError[?[_], Throwable]](quantile: Double, error: Double): F[Quantile] = 
      impl(quantile, error).liftTo[F]

    def quantile(quantile: Double, error: Double): Quantile = macro Macros.quantileLiteral
  }

  object Unsafe {
    def asJavaUnlabelled[F[_], A](g: UnlabelledSummary[F, A]): JSummary = 
      g.underlying
    def asJava[F[_]: ApplicativeError[?[_], Throwable]](c: Summary[F]): F[JSummary] = c match {
      case _: LabelledSummary[F] => ApplicativeError[F, Throwable].raiseError(new IllegalArgumentException("Cannot Get Underlying Parent with Labels Applied"))
      case n: NoLabelsSummary[F] => n.underlying.pure[F]
    }
  }
}