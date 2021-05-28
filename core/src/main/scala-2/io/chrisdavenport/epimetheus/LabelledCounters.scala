package io.chrisdavenport.epimetheus

import cats.implicits._
import cats.effect._
import io.prometheus.client.{Counter => JCounter}
import shapeless._

trait LabelledCounters {

  /**
   * Constructor for a labelled [[Counter]].
   *
   * This generates a specific number of labels via `Sized`, in combination with a function
   * to generate an equally `Sized` set of labels from some type. Values are applied by position.
   *
   * This counter needs to have a label applied to the [[UnlabelledCounter]] in order to
   * be measureable or recorded.
   *
   * @param cr CollectorRegistry this [[Counter]] will be registred with
   * @param name The name of the Counter -  By convention, the names of Counters are suffixed by `_total`.
   * @param help The help string of the metric
   * @param labels The name of the labels to be applied to this metric
   * @param f Function to take some value provided in the future to generate an equally sized list
   *  of strings as the list of labels. These are assigned to labels by position.
   */
  def labelled[F[_]: Sync, A, N <: Nat](
    cr: CollectorRegistry[F],
    name: Name,
    help: String,
    labels: Sized[IndexedSeq[Label], N],
    f: A => Sized[IndexedSeq[String], N]
  ): F[UnlabelledCounter[F, A]] = for {
    c <- Sync[F].delay(JCounter.build().name(name.getName).help(help).labelNames(labels.map(_.getLabel):_*))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new Counter.UnlabelledCounterImpl[F, A](out, f.andThen(_.unsized))

}
