package io.chrisdavenport.epimetheus

import cats._
import cats.implicits._
import cats.effect._
import io.prometheus.metrics.core.datapoints.GaugeDataPoint
import io.prometheus.metrics.core.metrics.{Gauge => JGauge}

import scala.annotation.tailrec

/** Gauge metric, to report instantaneous values.
  *
  * Gauges can go both up and down.
  *
  * An Example With No Labels:
  * {{{
  *  for {
  *    pr <- PrometheusRegistry.build
  *    gauge <- Gauge.noLabels(pr, "gauge_value", "Gauge Help")
  *    _ <- gauge.inc
  *    _ <- gauge.dec
  *  } yield ()
  * }}}
  *
  * An Example With Labels:
  * {{{
  *  for {
  *    pr <- PrometheusRegistry.build
  *    gauge <- Gauge.labelled(pr, "gauge_value", "Gauge Help", Sized("foo"), {s: String => Sized(s)})
  *    _ <- gauge.label("bar").inc
  *    _ <- gauge.label("bar").dec
  *    _ <- gauge.label("baz").inc
  *    _ <- gauge.label("baz").dec
  * }}}
  *
  * These can be aggregated and processed together much more easily in the Prometheus
  * server than individual metrics for each labelset.
  */
sealed abstract class Gauge[F[_]] {

  /** Decrement the value of this [[Gauge]] by 1.
    */
  def dec: F[Unit]

  /** Decrement the value of this gauge by the provided value.
    *
    * @param d The value to decrease the [[Gauge]] by.
    */
  def decBy(d: Double): F[Unit]

  /** Increment the value of this [[Gauge]] by 1.
    */
  def inc: F[Unit]

  /** Increment the value of this gauge by the provided value.
    *
    * @param d The value to increase the [[Gauge]] by.
    */
  def incBy(d: Double): F[Unit]

  def set(d: Double): F[Unit]

  def mapK[G[_]](fk: F ~> G): Gauge[G] = new Gauge.MapKGauge[F, G](this, fk)

  private[epimetheus] def asJava: F[JGauge]
}

/** Gauge Constructors, and Unsafe Gauge Access
  */
object Gauge {

  // Convenience
  def incIn[F[_], A](g: Gauge[F], fa: F[A])(implicit
      C: MonadCancel[F, _]
  ): F[A] =
    C.bracket(g.inc)(_ => fa)(_ => g.dec)

  def incByIn[F[_], A](g: Gauge[F], fa: F[A], i: Double)(implicit
      C: MonadCancel[F, _]
  ): F[A] =
    C.bracket(g.incBy(i))(_ => fa)(_ => g.decBy(i))

  def decIn[F[_], A](g: Gauge[F], fa: F[A])(implicit
      C: MonadCancel[F, _]
  ): F[A] =
    C.bracket(g.dec)(_ => fa)(_ => g.inc)

  def decByIn[F[_], A](g: Gauge[F], fa: F[A], i: Double)(implicit
      C: MonadCancel[F, _]
  ): F[A] =
    C.bracket(g.decBy(i))(_ => fa)(_ => g.incBy(i))

  // Constructors

  /** Constructor for a [[Gauge]] with no labels.
    *
    * @param pr PrometheusRegistry this [[Gauge]] will be registered with
    * @param name The name of the Gauge
    * @param help The help string of the metric
    */
  def noLabels[F[_]: Sync](
      pr: PrometheusRegistry[F],
      name: Name,
      help: String
  ): F[Gauge[F]] = for {
    c <- Sync[F].delay(JGauge.builder().name(name.getName).help(help))
    out <- Sync[F].delay(c.register(PrometheusRegistry.Unsafe.asJava(pr)))
  } yield new NoLabelsGauge[F](out)

  /** Constructor for a labelled [[Gauge]].
    *
    * This generates a specific number of labels via `Sized`, in combination with a function
    * to generate an equally `Sized` set of labels from some type. Values are applied by position.
    *
    * This counter needs to have a label applied to the [[UnlabelledGauge]] in order to
    * be measureable or recorded.
    *
    * @param pr PrometheusRegistry this [[Gauge]] will be registred with
    * @param name The name of the [[Gauge]].
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
  ): F[UnlabelledGauge[F, A]] = for {
    c <- Sync[F].delay(
      JGauge
        .builder()
        .name(name.getName)
        .help(help)
        .labelNames(labels.unsized.map(_.getLabel): _*)
    )
    out <- Sync[F].delay(c.register(PrometheusRegistry.Unsafe.asJava(pr)))
  } yield new UnlabelledGaugeImpl[F, A](out, f.andThen(_.unsized))

  private final class NoLabelsGauge[F[_]: Sync] private[Gauge] (
      private[Gauge] val underlying: JGauge
  ) extends Gauge[F] {
    def dec: F[Unit] = Sync[F].delay(underlying.dec())
    def decBy(d: Double): F[Unit] = Sync[F].delay(underlying.dec(d))

    def inc: F[Unit] = Sync[F].delay(underlying.inc())
    def incBy(d: Double): F[Unit] = Sync[F].delay(underlying.inc(d))

    def set(d: Double): F[Unit] = Sync[F].delay(underlying.set(d))

    override private[epimetheus] def asJava: F[JGauge] = underlying.pure[F]
  }

  private final class LabelledGauge[F[_]: Sync] private[Gauge] (
      private val underlying: JGauge,
      private val underlyingDataPoint: GaugeDataPoint
  ) extends Gauge[F] {
    def dec: F[Unit] = Sync[F].delay(underlyingDataPoint.dec())
    def decBy(d: Double): F[Unit] = Sync[F].delay(underlyingDataPoint.dec(d))

    def inc: F[Unit] = Sync[F].delay(underlyingDataPoint.inc())
    def incBy(d: Double): F[Unit] = Sync[F].delay(underlyingDataPoint.inc(d))

    def set(d: Double): F[Unit] = Sync[F].delay(underlyingDataPoint.set(d))

    override private[epimetheus] def asJava: F[JGauge] = underlying.pure[F]
  }

  private final class MapKGauge[F[_], G[_]](
      private[Gauge] val base: Gauge[F],
      fk: F ~> G
  ) extends Gauge[G] {
    def dec: G[Unit] = fk(base.dec)
    def decBy(d: Double): G[Unit] = fk(base.decBy(d))

    def inc: G[Unit] = fk(base.inc)
    def incBy(d: Double): G[Unit] = fk(base.incBy(d))

    def set(d: Double): G[Unit] = fk(base.set(d))

    override private[epimetheus] def asJava: G[JGauge] = fk(base.asJava)
  }

  sealed trait UnlabelledGauge[F[_], A] {
    def label(a: A): Gauge[F]
    def mapK[G[_]](fk: F ~> G): UnlabelledGauge[G, A] =
      new MapKUnlabelledGauge[F, G, A](this, fk)
  }

  /** Generic Unlabeled Gauge
    *
    * It is necessary to apply a value of type `A` to this
    * gauge to be able to take any measurements.
    */
  final private[epimetheus] class UnlabelledGaugeImpl[
      F[_]: Sync,
      A
  ] private[epimetheus] (
      private[Gauge] val underlying: JGauge,
      private val f: A => IndexedSeq[String]
  ) extends UnlabelledGauge[F, A] {
    def label(a: A): Gauge[F] =
      new LabelledGauge[F](underlying, underlying.labelValues(f(a): _*))
  }

  final private class MapKUnlabelledGauge[F[_], G[_], A](
      private[Gauge] val base: UnlabelledGauge[F, A],
      fk: F ~> G
  ) extends UnlabelledGauge[G, A] {
    def label(a: A): Gauge[G] = base.label(a).mapK(fk)
  }

  object Unsafe {
    @tailrec
    def asJavaUnlabelled[F[_], A](g: UnlabelledGauge[F, A]): JGauge = g match {
      case x: UnlabelledGaugeImpl[_, _]    => x.underlying
      case x: MapKUnlabelledGauge[f, _, a] => asJavaUnlabelled(x.base)
    }
    def asJava[F[_]](c: Gauge[F]): F[JGauge] = c.asJava
    def fromJava[F[_]: Sync](g: JGauge): Gauge[F] = new LabelledGauge(g, g)
    def fromJavaUnlabelled[F[_]: Sync](g: JGauge): Gauge[F] = new NoLabelsGauge(
      g
    )
  }
}
