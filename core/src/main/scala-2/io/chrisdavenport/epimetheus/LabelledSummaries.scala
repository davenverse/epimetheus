package io.chrisdavenport.epimetheus

import cats.implicits._
import cats.effect._
import io.prometheus.client.{Summary => JSummary}
import shapeless._

//import scala.language.experimental.macros
//import scala.reflect.macros.whitebox

trait LabelledSummaries {

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
  def labelled[F[_]: Sync, A, N <: Nat](
    cr: CollectorRegistry[F],
    name: Name,
    help: String,
    labels: Sized[IndexedSeq[Label], N],
    f: A => Sized[IndexedSeq[String], N],
    quantiles: Summary.Quantile*
  ): F[UnlabelledSummary[F, A]] =
    labelledQuantiles(cr, name, help, Summary.defaultMaxAgeSeconds, Summary.defaultAgeBuckets, labels, f, quantiles:_*)

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
  def labelledQuantiles[F[_]: Sync, A, N <: Nat](
    cr: CollectorRegistry[F],
    name: Name,
    help: String,
    maxAgeSeconds: Long,
    ageBuckets: Int,
    labels: Sized[IndexedSeq[Label], N],
    f: A => Sized[IndexedSeq[String], N],
    quantiles: Summary.Quantile*
  ): F[UnlabelledSummary[F, A]] = for {
    c1 <- Sync[F].delay(
      JSummary.build()
        .name(name.getName)
        .help(help)
        .maxAgeSeconds(maxAgeSeconds)
        .ageBuckets(ageBuckets)
        .labelNames(labels.map(_.getLabel):_*)
    )
    c <- Sync[F].delay(quantiles.foldLeft(c1){ case (c, q) => c.quantile(q.quantile, q.error)})
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new Summary.UnlabelledSummaryImpl[F, A](out, f.andThen(_.unsized))
}
