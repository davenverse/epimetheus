package io.chrisdavenport.epimetheus

import cats._
import cats.implicits._
import cats.effect._
import io.prometheus.client.{Summary => JSummary}
import scala.concurrent.duration._
import shapeless._

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

/**
 * Summary metric, to track the size of events.
 * 
 * The quantiles are calculated over a sliding window of time. There are two options to configure this time window:
 * 
 * maxAgeSeconds: Long -  Set the duration of the time window is, i.e. how long observations are kept before they are discarded.
 * Default is 10 minutes.
 * 
 * ageBuckets: Int - Set the number of buckets used to implement the sliding time window. If your time window is 10 minutes, and you have ageBuckets=5,
 * buckets will be switched every 2 minutes. The value is a trade-off between resources (memory and cpu for maintaining the bucket)
 * and how smooth the time window is moved. Default value is 5.
 * 
 * See https://prometheus.io/docs/practices/histograms/ for more info on quantiles.
 */
sealed abstract class Summary[F[_]]{
  def observe(d: Double): F[Unit]
  def timed[A](fa: F[A], unit: TimeUnit): F[A]
}


object Summary {

  val defaultMaxAgeSeconds = 600L
  val defaultAgeBuckets = 5

  /**
   * Default Constructor for a [[Summary]] with no labels.
   * 
   * maxAgeSeconds is set to [[defaultMaxAgeSeconds]] which is 10 minutes.
   * 
   * ageBuckets is the number of buckets for the sliding time window, set to [[defaultAgeBuckets]] which is 5.
   * 
   * If you want to exert control, use the full constructor [[Summary.noLabelsQuantiles noLabelsQuantiles]]
   * 
   * @param cr CollectorRegistry this [[Summary]] will be registered with
   * @param name The name of the Summary
   * @param help The help string of the metric
   * @param quantiles The measurements to track for specifically over the sliding time window.
   */
  def noLabels[F[_]: Sync: Clock](
    cr: CollectorRegistry[F], 
    name: String, 
    help: String,
    quantiles: Quantile*
  ): F[Summary[F]] = 
    noLabelsQuantiles(cr, name, help, defaultMaxAgeSeconds, defaultAgeBuckets, quantiles:_*)

  /**
   * Constructor for a [[Summary]] with no labels.
   * 
   * maxAgeSeconds is set to [[defaultMaxAgeSeconds]] which is 10 minutes.
   * 
   * ageBuckets is the number of buckets for the sliding time window, set to [[defaultAgeBuckets]] which is 5.
   * 
   * If you want to exert control, use the full constructor [[Summary.noLabelsQuantiles noLabelsQuantiles]]
   * 
   * @param cr CollectorRegistry this [[Summary]] will be registered with
   * @param name The name of the Summary
   * @param help The help string of the metric
   * @param maxAgeSeconds Set the duration of the time window is, 
   *  i.e. how long observations are kept before they are discarded.
   * @param ageBuckets Set the number of buckets used to implement the sliding time window. If your time window is 10 minutes, and you have ageBuckets=5,
   *  buckets will be switched every 2 minutes. The value is a trade-off between resources (memory and cpu for maintaining the bucket)
   *  and how smooth the time window is moved.
   * @param quantiles The measurements to track for specifically over the sliding time window.
   */
  def noLabelsQuantiles[F[_]: Sync: Clock](
    cr: CollectorRegistry[F], 
    name: String, 
    help: String, 
    maxAgeSeconds: Long, 
    ageBuckets: Int, 
    quantiles: Quantile*
  ): F[Summary[F]] = for {
    c1 <- Sync[F].delay(
      JSummary.build()
      .name(name)
      .help(help)
      .maxAgeSeconds(maxAgeSeconds)
      .ageBuckets(ageBuckets)
    )
    c <- Sync[F].delay(quantiles.foldLeft(c1){ case (c, q) => c.quantile(q.quantile, q.error)})
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new NoLabelsSummary[F](out)

  /**
   * Default Constructor for a labelled [[Summary]].
   * 
   * maxAgeSeconds is set to [[defaultMaxAgeSeconds]] which is 10 minutes.
   * 
   * ageBuckets is the number of buckets for the sliding time window, set to [[defaultAgeBuckets]] which is 5.
   * 
   * This generates a specific number of labels via `Sized`, in combination with a function
   * to generate an equally `Sized` set of labels from some type. Values are applied by position.
   * 
   * This counter needs to have a label applied to the [[UnlabelledSummary]] in order to 
   * be measureable or recorded.
   * 
   * @param cr CollectorRegistry this [[Summary]] will be registred with
   * @param name The name of the [[Summary]].
   * @param help The help string of the metric
   * @param labels The name of the labels to be applied to this metric
   * @param f Function to take some value provided in the future to generate an equally sized list
   *  of strings as the list of labels. These are assigned to labels by position.
   * @param quantiles The measurements to track for specifically over the sliding time window.
   */
  def labelled[F[_]: Sync: Clock, A, N <: Nat](
    cr: CollectorRegistry[F], 
    name: String, 
    help: String,
    labels: Sized[IndexedSeq[String], N], 
    f: A => Sized[IndexedSeq[String], N],
    quantiles: Quantile*
  ): F[UnlabelledSummary[F, A]] = 
    labelledQuantiles(cr, name, help, defaultMaxAgeSeconds, defaultAgeBuckets, labels, f, quantiles:_*)

  /**
   * Constructor for a labelled [[Summary]].
   * 
   * maxAgeSeconds is set to [[defaultMaxAgeSeconds]] which is 10 minutes.
   * 
   * ageBuckets is the number of buckets for the sliding time window, set to [[defaultAgeBuckets]] which is 5.
   * 
   * This generates a specific number of labels via `Sized`, in combination with a function
   * to generate an equally `Sized` set of labels from some type. Values are applied by position.
   * 
   * This counter needs to have a label applied to the [[UnlabelledSummary]] in order to 
   * be measureable or recorded.
   * 
   * @param cr CollectorRegistry this [[Summary]] will be registred with
   * @param name The name of the [[Summary]].
   * @param help The help string of the metric
   * @param maxAgeSeconds Set the duration of the time window is, 
   *  i.e. how long observations are kept before they are discarded.
   * @param ageBuckets Set the number of buckets used to implement the sliding time window. 
   *  If your time window is 10 minutes, and you have ageBuckets=5,
   *  buckets will be switched every 2 minutes. 
   *  The value is a trade-off between resources (memory and cpu for maintaining the bucket)
   *  and how smooth the time window is moved.
   * @param labels The name of the labels to be applied to this metric
   * @param f Function to take some value provided in the future to generate an equally sized list
   *  of strings as the list of labels. These are assigned to labels by position.
   * @param quantiles The measurements to track for specifically over the sliding time window.
   */
  def labelledQuantiles[F[_]: Sync: Clock, A, N <: Nat](
    cr: CollectorRegistry[F], 
    name: String, 
    help: String,
    maxAgeSeconds: Long, 
    ageBuckets: Int,
    labels: Sized[IndexedSeq[String], N], 
    f: A => Sized[IndexedSeq[String], N],
    quantiles: Quantile*
  ): F[UnlabelledSummary[F, A]] = for {
    c1 <- Sync[F].delay(
      JSummary.build()
      .name(name)
      .help(help)
      .maxAgeSeconds(maxAgeSeconds)
      .ageBuckets(ageBuckets)
      .labelNames(labels:_*)
    )
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
   * Apply a label to be able to measure events.
   */
  final class UnlabelledSummary[F[_]: Sync: Clock, A] private[epimetheus](
    private[Summary] val underlying: JSummary, 
    private val f: A => IndexedSeq[String]
  ) {
    def label(a: A): Summary[F] =
      new LabelledSummary[F](underlying.labels(f(a):_*))
  }

  /**
   * The percentile and tolerated error to be observed
   * 
   * There is a [[Quantile.impl safe constructor]], and a [[Quantile.quantile macro constructor]] which can
   * statically verify these values if they are known at compile time.
   * 
   * 
   * `Quantile.quantile(0.5, 0.05)` - 50th percentile (= median) with 5% tolerated error
   * 
   * `Quantile.quantile(0.9, 0.01)` - 90th percentile with 1% tolerated error
   * 
   * `Quantile.quantile(0.99, 0.001)` - 99th percentile with 0.1% tolerated error
   */
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
              s"This method uses a macro to verify that a Quantile literal is valid. Use Quantile.impl if you have a dynamic set that you want to parse as a Quantile."
            )
        }
  }

    /**
     * Safe Constructor of a Quantile valid values for both values are greater than 0
     * but less than 1.
     */
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