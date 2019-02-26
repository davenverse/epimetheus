package io.chrisdavenport.epimetheus

import cats._
import cats.implicits._
import cats.effect._
import cats.implicits._
import io.prometheus.client.{Histogram => JHistogram}
import shapeless._

/**
 * Histogram metric, to track distributions of events.
 * 
 * Note: Each bucket is one timeseries. Many buckets and/or many dimensions with labels
 * can produce large amount of time series, that may cause performance problems.
 * 
 * The default buckets are intended to cover a typical web/rpc request from milliseconds to seconds.
 * 
 */
sealed abstract class Histogram[F[_]]{
  
  /**
   * Persist an observation into this [[Histogram]]
   * 
   * @param d The observation to persist
   */
  def observe(d: Double): F[Unit]

}

/**
 * Histogram Constructors, Convenience Methods and Unsafe Histogram Access
 * 
 * Convenience function exposed here will also be exposed as implicit syntax
 * enhancements on the Histogram
 */
object Histogram {

  // Constructors ---------------------------------------------------
  /**
   * Default Buckets
   * 
   * Intended to cover a typical web/rpc request from milliseconds to seconds.
   */
  val defaults = List(.005, .01, .025, .05, .075, .1, .25, .5, .75, 1, 2.5, 5, 7.5, 10)

  /**
   * Constructor for a [[Histogram]] with no labels. Default buckets are [[defaults]]
   * and are intended to cover a typical web/rpc request from milliseconds to seconds.
   * 
   * @param cr CollectorRegistry this [[Histogram]] will be registered with
   * @param name The name of the Histogram
   * @param help The help string of the metric
   */
  def noLabels[F[_]: Sync](
    cr: CollectorRegistry[F],
    name: Name,
    help: String
  ): F[Histogram[F]] = 
    noLabelsBuckets(cr, name, help, defaults:_*)

  /**
   * Constructor for a [[Histogram]] with no labels. Default buckets are [[defaults]]
   * and are intended to cover a typical web/rpc request from milliseconds to seconds.
   * 
   * @param cr CollectorRegistry this [[Histogram]] will be registered with
   * @param name The name of the Gauge
   * @param help The help string of the metric
   * @param buckets The buckets to measure observations by.
   */
  def noLabelsBuckets[F[_]: Sync](
    cr: CollectorRegistry[F],
    name: Name,
    help: String,
    buckets: Double*
  ): F[Histogram[F]] = for {
    c <- Sync[F].delay(
      JHistogram.build()
      .name(name.getName)
      .help(help)
      .buckets(buckets:_*)
    )
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new NoLabelsHistogram[F](out)

  def noLabelsLinearBuckets[F[_]: Sync](
    cr: CollectorRegistry[F],
    name: Name,
    help: String,
    start: Double,
    factor: Double,
    count: Int
  ): F[Histogram[F]] = for {
    c <- Sync[F].delay(
      JHistogram.build()
      .name(name.getName)
      .help(help)
      .linearBuckets(start, factor, count)
    )
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new NoLabelsHistogram[F](out)

  def noLabelsExponentialBuckets[F[_]: Sync](
    cr: CollectorRegistry[F],
    name: Name, help: String,
    start: Double,
    factor: Double,
    count: Int
  ): F[Histogram[F]] = for {
    c <- Sync[F].delay(
      JHistogram.build()
      .name(name.getName)
      .help(help)
      .exponentialBuckets(start, factor, count)
    )
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new NoLabelsHistogram[F](out)

  /**
   * Constructor for a labelled [[Histogram]]. Default buckets are [[defaults]]
   * and are intended to cover a typical web/rpc request from milliseconds to seconds.
   * 
   * This generates a specific number of labels via `Sized`, in combination with a function
   * to generate an equally `Sized` set of labels from some type. Values are applied by position.
   * 
   * This counter needs to have a label applied to the [[UnlabelledHistogram]] in order to 
   * be measureable or recorded.
   * 
   * @param cr CollectorRegistry this [[Histogram]] will be registred with
   * @param name The name of the [[Histogram]].
   * @param help The help string of the metric
   * @param labels The name of the labels to be applied to this metric
   * @param f Function to take some value provided in the future to generate an equally sized list
   *  of strings as the list of labels. These are assigned to labels by position.
   */
  def labelled[F[_]: Sync, A, N <: Nat](
    cr: CollectorRegistry[F], 
    name: Name, 
    help: String, 
    labels: Sized[IndexedSeq[Name], N], 
    f: A => Sized[IndexedSeq[String], N]
  ): F[UnlabelledHistogram[F, A]] = 
    labelledBuckets(cr, name, help, labels, f, defaults:_*)

  /**
   * Constructor for a labelled [[Histogram]]. Default buckets are [[defaults]]
   * and are intended to cover a typical web/rpc request from milliseconds to seconds.
   * 
   * This generates a specific number of labels via `Sized`, in combination with a function
   * to generate an equally `Sized` set of labels from some type. Values are applied by position.
   * 
   * This counter needs to have a label applied to the [[UnlabelledHistogram]] in order to 
   * be measureable or recorded.
   * 
   * @param cr CollectorRegistry this [[Histogram]] will be registred with
   * @param name The name of the [[Histogram]].
   * @param help The help string of the metric
   * @param labels The name of the labels to be applied to this metric
   * @param f Function to take some value provided in the future to generate an equally sized list
   *  of strings as the list of labels. These are assigned to labels by position.
   * @param buckets The buckets to measure observations by. 
   */
  def labelledBuckets[F[_]: Sync, A, N <: Nat](
    cr: CollectorRegistry[F], 
    name: Name, 
    help: String, 
    labels: Sized[IndexedSeq[Name], N], 
    f: A => Sized[IndexedSeq[String], N],
    buckets: Double*
  ): F[UnlabelledHistogram[F, A]] = for {
    c <- Sync[F].delay(
      JHistogram.build()
      .name(name.getName)
      .help(help)
      .labelNames(labels.map(_.getName):_*)
      .buckets(buckets:_*)
    )
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new UnlabelledHistogram[F, A](out, f.andThen(_.unsized))

  def labelledLinearBuckets[F[_]: Sync, A, N <: Nat](
    cr: CollectorRegistry[F], 
    name: Name, 
    help: String, 
    labels: Sized[IndexedSeq[Name], N], 
    f: A => Sized[IndexedSeq[String], N],
    start: Double,
    factor: Double,
    count: Int
  ): F[UnlabelledHistogram[F, A]] = for {
    c <- Sync[F].delay(
      JHistogram.build()
      .name(name.getName)
      .help(help)
      .labelNames(labels.map(_.getName):_*)
      .linearBuckets(start, factor, count)
    )
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new UnlabelledHistogram[F, A](out, f.andThen(_.unsized))

  def labelledExponentialBuckets[F[_]: Sync, A, N <: Nat](
    cr: CollectorRegistry[F], 
    name: Name, 
    help: String, 
    labels: Sized[IndexedSeq[Name], N], 
    f: A => Sized[IndexedSeq[String], N],
    start: Double,
    factor: Double,
    count: Int
  ): F[UnlabelledHistogram[F, A]] = for {
    c <- Sync[F].delay(
      JHistogram.build()
      .name(name.getName)
      .help(help)
      .labelNames(labels.map(_.getName):_*)
      .exponentialBuckets(start, factor, count)
    )
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new UnlabelledHistogram[F, A](out, f.andThen(_.unsized))

  private final class NoLabelsHistogram[F[_]: Sync] private[Histogram] (
    private[Histogram] val underlying: JHistogram
  ) extends Histogram[F] {
    def observe(d: Double): F[Unit] = Sync[F].delay(underlying.observe(d))
  
  }

  private final class LabelledHistogram[F[_]: Sync] private[Histogram] (
    private val underlying: JHistogram.Child
  ) extends Histogram[F] {
    def observe(d: Double): F[Unit] = Sync[F].delay(underlying.observe(d))
  }

  /**
   * Generic UnlabelledHistorgram
   * 
   * It is necessary to apply a value of type `A` to this
   * histogram to be able to take any measurements.
   */
  final class UnlabelledHistogram[F[_]: Sync, A] private[Histogram] (
    private[Histogram] val underlying: JHistogram, 
    private val f: A => IndexedSeq[String]
  ) {
    def label(a: A): Histogram[F] = 
      new LabelledHistogram[F](underlying.labels(f(a):_*))
  }

  object Unsafe {
    def asJavaUnlabelled[F[_], A](h: UnlabelledHistogram[F, A]): JHistogram = 
      h.underlying
    def asJava[F[_]: ApplicativeError[?[_], Throwable]](c: Histogram[F]): F[JHistogram] = c match {
      case _: LabelledHistogram[F] => ApplicativeError[F, Throwable].raiseError(new IllegalArgumentException("Cannot Get Underlying Parent with Labels Applied"))
      case n: NoLabelsHistogram[F] => n.underlying.pure[F]
    }
  }

}