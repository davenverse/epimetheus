package io.chrisdavenport.epimetheus

import cats.implicits._
import cats.effect._
import io.prometheus.client.{Gauge => JGauge}
import shapeless._

/**
 * Gauge metric, to report instantaneous values.
 * 
 * Gauges can go both up and down.
 * 
 * An Example With No Labels:
 * {{{
 *  for {
 *    cr <- CollectorRegistry.build
 *    gauge <- Gauge.noLabels(cr, "gauge_value", "Gauge Help")
 *    _ <- gauge.inc
 *    _ <- gauge.dec
 *  } yield ()
 * }}}
 * 
 * An Example With Labels:
 * {{{
 *  for {
 *    cr <- CollectorRegistry.build
 *    gauge <- Gauge.labelled(cr, "gauge_value", "Gauge Help", Sized("foo"), {s: String => Sized(s)})
 *    _ <- gauge.label("bar").inc
 *    _ <- gauge.label("bar").dec
 *    _ <- gauge.label("baz").inc
 *    _ <- gauge.label("baz").dec
 * }}}
 * 
 * These can be aggregated and processed together much more easily in the Prometheus
 * server than individual metrics for each labelset.
 */
sealed abstract class Gauge[F[_]]{
  def get: F[Double]

  def dec: F[Unit]
  def decBy(d: Double): F[Unit]
  
  def inc: F[Unit]
  def incBy(d: Double): F[Unit]

  def set(d: Double): F[Unit]
}

object Gauge {

  /**
   * Constructor for a [[Gauge]] with no labels.
   * 
   * @param cr CollectorRegistry this [[Gauge]] will be registered with
   * @param name The name of the Gauge
   * @param help The help string of the metric
   */
  def noLabels[F[_]: Sync](cr: CollectorRegistry[F], name: String, help: String): F[Gauge[F]] = for {
    c <- Sync[F].delay(JGauge.build().name(name).help(help))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new NoLabelsGauge[F](out)

  /**
   * Constructor for a labelled [[Gauge]].
   * 
   * This generates a specific number of labels via `Sized`, in combination with a function
   * to generate an equally `Sized` set of labels from some type. Values are applied by position.
   * 
   * This counter needs to have a label applied to the [[UnlabelledGauge]] in order to 
   * be measureable or recorded.
   * 
   * @param cr CollectorRegistry this [[Gauge]] will be registred with
   * @param name The name of the [[Gauge]].
   * @param help The help string of the metric
   * @param labels The name of the labels to be applied to this metric
   * @param f Function to take some value provided in the future to generate an equally sized list
   *  of strings as the list of labels. These are assigned to labels by position.
   */
  def labelled[F[_]: Sync, A, N <: Nat](
    cr: CollectorRegistry[F], 
    name: String, 
    help: String, 
    labels: Sized[IndexedSeq[String], N], 
    f: A => Sized[IndexedSeq[String], N]
  ): F[UnlabelledGauge[F, A]] = for {
      c <- Sync[F].delay(JGauge.build().name(name).help(help).labelNames(labels:_*))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new UnlabelledGauge[F, A](out, f.andThen(_.unsized))


  private final class NoLabelsGauge[F[_]: Sync] private[Gauge] (
    private val underlying: JGauge
  ) extends Gauge[F] {
    def get: F[Double] = Sync[F].delay(underlying.get())
  
    def dec: F[Unit] = Sync[F].delay(underlying.dec())
    def decBy(d: Double): F[Unit] = Sync[F].delay(underlying.dec(d))
    
    def inc: F[Unit] = Sync[F].delay(underlying.inc())
    def incBy(d: Double): F[Unit] = Sync[F].delay(underlying.inc(d))
  
    def set(d: Double): F[Unit] = Sync[F].delay(underlying.set(d))
  }
  
  private final class LabelledGauge[F[_]: Sync] private[Gauge] (
    private val underlying: JGauge.Child
  ) extends Gauge[F] {
    def get: F[Double] = Sync[F].delay(underlying.get())
  
    def dec: F[Unit] = Sync[F].delay(underlying.dec())
    def decBy(d: Double): F[Unit] = Sync[F].delay(underlying.dec(d))
    
    def inc: F[Unit] = Sync[F].delay(underlying.inc())
    def incBy(d: Double): F[Unit] = Sync[F].delay(underlying.inc(d))
  
    def set(d: Double): F[Unit] = Sync[F].delay(underlying.set(d))
  }



  /**
   * Generic Unlabeled Gauge
   * 
   * It is necessary to apply a value of type `A` to this
   * gauge to be able to take any measurements.
   */
  final class UnlabelledGauge[F[_]: Sync, A] private[epimetheus](
    private[Gauge] val underlying: JGauge, 
    private val f: A => IndexedSeq[String]
  ) {
    def label(a: A): Gauge[F] =
      new LabelledGauge[F](underlying.labels(f(a):_*))
  }
  object Unsafe {
    def asJavaUnlabelled[F[_], A](g: UnlabelledGauge[F, A]): JGauge = g.underlying
  }
}