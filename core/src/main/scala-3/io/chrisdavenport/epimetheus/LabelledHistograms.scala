package io.chrisdavenport.epimetheus

import cats.effect._
import cats.implicits._
import io.prometheus.client.{Histogram => JHistogram}

trait LabelledHistograms {

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
  def labelled[F[_]: Sync, A, N <: Int](
    cr: CollectorRegistry[F],
    name: Name,
    help: String,
    labels: Sized[Label, N],
    f: A => Sized[String, N]
  ): F[UnlabelledHistogram[F, A]] =
    labelledBuckets(cr, name, help, labels, f, Histogram.defaults:_*)

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
  def labelledBuckets[F[_]: Sync, A, N <: Int](
    cr: CollectorRegistry[F],
    name: Name,
    help: String,
    labels: Sized[Label, N],
    f: A => Sized[String, N],
    buckets: Double*
  ): F[UnlabelledHistogram[F, A]] = for {
    c <- Sync[F].delay(
      JHistogram.build()
        .name(name.getName)
        .help(help)
        .labelNames(labels.unsized.map(_.getLabel):_*)
        .buckets(buckets:_*)
    )
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new Histogram.UnlabelledHistogramImpl[F, A](out, f.andThen(_.unsized))

  def labelledLinearBuckets[F[_]: Sync, A, N <: Int](
    cr: CollectorRegistry[F],
    name: Name,
    help: String,
    labels: Sized[Label, N],
    f: A => Sized[String, N],
    start: Double,
    factor: Double,
    count: Int
  ): F[UnlabelledHistogram[F, A]] = for {
    c <- Sync[F].delay(
      JHistogram.build()
        .name(name.getName)
        .help(help)
        .labelNames(labels.unsized.map(_.getLabel):_*)
        .linearBuckets(start, factor, count)
    )
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new Histogram.UnlabelledHistogramImpl[F, A](out, f.andThen(_.unsized))

  def labelledExponentialBuckets[F[_]: Sync, A, N <: Int](
    cr: CollectorRegistry[F],
    name: Name,
    help: String,
    labels: Sized[Label, N],
    f: A => Sized[String, N],
    start: Double,
    factor: Double,
    count: Int
  ): F[UnlabelledHistogram[F, A]] = for {
    c <- Sync[F].delay(
      JHistogram.build()
        .name(name.getName)
        .help(help)
        .labelNames(labels.unsized.map(_.getLabel):_*)
        .exponentialBuckets(start, factor, count)
    )
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new Histogram.UnlabelledHistogramImpl[F, A](out, f.andThen(_.unsized))
}
