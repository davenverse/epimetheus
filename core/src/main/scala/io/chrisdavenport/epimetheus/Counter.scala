package io.chrisdavenport.epimetheus

import cats._
import cats.implicits._
import cats.effect._
import io.prometheus.metrics.core.datapoints.CounterDataPoint
import io.prometheus.metrics.core.metrics.{Counter => JCounter}

import scala.annotation.tailrec

/** Counter metric, to track counts, running totals, or events.
  *
  * If your use case can go up or down consider using a [[Gauge]] instead.
  * Use the `rate()` function in Prometheus to calculate the rate of increase of a Counter.
  * By convention, the names of Counters are suffixed by `_total`.
  *
  * An Example Counter without Labels:
  * {{{
  *   for {
  *     pr <- PrometheusRegistry.build[IO]
  *     successCounter <- Counter.noLabels(pr, "example_success_total", "Example Counter of Success")
  *     failureCounter <- Counter.noLabels(Cr, "example_failure_total", "Example Counter of Failure")
  *     _ <- IO(println("Action Here")).guaranteeCase{
  *       case ExitCase.Completed => successCounter.inc
  *       case _ => failureCounter.inc
  *     }
  *   } yield ()
  * }}}
  *
  * An Example of a Counter with Labels:
  * {{{
  *   for {
  *     pr <- PrometheusRegistry.build[IO]
  *     counter <- Counter.labelled(pr, "example_total", "Example Counter", Sized("foo"), {s: String => Sized(s)})
  *     _ <- counter.label("bar").inc
  *     _ <- counter.label("baz").inc
  *   } yield ()
  * }}}
  */
sealed abstract class Counter[F[_]] {

  /** Increment the value of this [[Counter]] by 1.
    */
  def inc: F[Unit]

  /** Increment the value of this counter by the provided value.
    *
    * @param d The value to increase the [[Counter]] by.
    */
  def incBy(d: Double): F[Unit]

  def mapK[G[_]](fk: F ~> G): Counter[G] =
    new Counter.MapKCounter[F, G](this, fk)

  private[epimetheus] def asJava: F[JCounter]
}

/** Counter Constructors, and Unsafe Counter Access
  */
object Counter {

  /** Constructor for a Counter with no labels.
    *
    * @param pr PrometheusRegistry this [[Counter]] will be registered with
    * @param name The name of the Counter -  By convention, the names of Counters are suffixed by `_total`.
    * @param help The help string of the metric
    */
  def noLabels[F[_]: Sync](
      pr: PrometheusRegistry[F],
      name: Name,
      help: String
  ): F[Counter[F]] = for {
    c <- Sync[F].delay(JCounter.builder().name(name.getName).help(help))
    out <- Sync[F].delay(c.register(PrometheusRegistry.Unsafe.asJava(pr)))
  } yield new NoLabelsCounter[F](out)

  /** Constructor for a labelled [[Counter]].
    *
    * This generates a specific number of labels via `Sized`, in combination with a function
    * to generate an equally `Sized` set of labels from some type. Values are applied by position.
    *
    * This counter needs to have a label applied to the [[UnlabelledCounter]] in order to
    * be measureable or recorded.
    *
    * @param pr PrometheusRegistry this [[Counter]] will be registred with
    * @param name The name of the Counter -  By convention, the names of Counters are suffixed by `_total`.
    * @param help The help string of the metric
    * @param labels The name of the labels to be applied to this metric
    * @param f Function to take some value provided in the future to generate an equally sized list
    *  of strings as the list of labels. These are assigned to labels by position.
    */
  def labelled[F[_]: Sync, A, N <: Nat](
      pr: PrometheusRegistry[F],
      name: Name,
      help: String,
      labels: Sized[IndexedSeq[Label], N],
      f: A => Sized[IndexedSeq[String], N]
  ): F[UnlabelledCounter[F, A]] = for {
    c <- Sync[F].delay(
      JCounter
        .builder()
        .name(name.getName)
        .help(help)
        .labelNames(labels.unsized.map(_.getLabel): _*)
    )
    out <- Sync[F].delay(c.register(PrometheusRegistry.Unsafe.asJava(pr)))
  } yield new UnlabelledCounterImpl[F, A](out, f.andThen(_.unsized))

  private final class NoLabelsCounter[F[_]: Sync] private[Counter] (
      private[Counter] val underlying: JCounter
  ) extends Counter[F] {
    override def inc: F[Unit] = Sync[F].delay(underlying.inc())
    override def incBy(d: Double): F[Unit] = Sync[F].delay(underlying.inc(d))

    override private[epimetheus] def asJava: F[JCounter] = underlying.pure[F]
  }

  private final class LabelledCounter[F[_]: Sync] private[Counter] (
      private[Counter] val underlying: JCounter,
      private[Counter] val underlyingDataPoint: CounterDataPoint
  ) extends Counter[F] {
    def inc: F[Unit] = Sync[F].delay(underlyingDataPoint.inc)
    def incBy(d: Double): F[Unit] = Sync[F].delay(underlyingDataPoint.inc(d))

    override private[epimetheus] def asJava: F[JCounter] = underlying.pure[F]
  }

  private final class MapKCounter[F[_], G[_]](
      private[Counter] val base: Counter[F],
      fk: F ~> G
  ) extends Counter[G] {
    def inc: G[Unit] = fk(base.inc)
    def incBy(d: Double): G[Unit] = fk(base.incBy(d))

    override private[epimetheus] def asJava: G[JCounter] = fk(base.asJava)
  }

  /** Generic Unlabeled Counter
    *
    * It is necessary to apply a value of type `A` to this
    * counter to be able to take any measurements.
    */
  sealed trait UnlabelledCounter[F[_], A] {
    def label(a: A): Counter[F]
    def mapK[G[_]](fk: F ~> G): UnlabelledCounter[G, A] =
      new MapKUnlabelledCounter[F, G, A](this, fk)
  }

  private final class UnlabelledCounterImpl[F[_]: Sync, A] private[Counter] (
      private[Counter] val underlying: JCounter,
      private val f: A => IndexedSeq[String]
  ) extends UnlabelledCounter[F, A] {
    def label(a: A): Counter[F] =
      new LabelledCounter(underlying, underlying.labelValues(f(a): _*))
  }

  private final class MapKUnlabelledCounter[F[_], G[_], A](
      private[Counter] val base: UnlabelledCounter[F, A],
      fk: F ~> G
  ) extends UnlabelledCounter[G, A] {
    def label(a: A): Counter[G] = base.label(a).mapK(fk)
  }

  object Unsafe {
    @tailrec
    def asJavaUnlabelled[F[_], A](c: UnlabelledCounter[F, A]): JCounter =
      c match {
        case m: MapKUnlabelledCounter[f, _, a] => asJavaUnlabelled(m.base)
        case m: UnlabelledCounterImpl[_, _]    => m.underlying
      }
    def asJava[F[_]](c: Counter[F]): F[JCounter] = c.asJava
    def fromJava[F[_]: Sync](c: JCounter): Counter[F] =
      new LabelledCounter(c, c)
    def fromJavaUnlabelled[F[_]: Sync](c: JCounter): Counter[F] =
      new NoLabelsCounter(c)
  }
}
