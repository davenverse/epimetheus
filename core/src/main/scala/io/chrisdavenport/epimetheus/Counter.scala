package io.chrisdavenport.epimetheus

import cats._
import cats.implicits._
import cats.effect._
import io.prometheus.client.{Counter => JCounter}
import shapeless._

/**
  * Counter metric, to track counts, running totals, or events.
  *
  * If your use case can go up or down consider using a [[Gauge]] instead.
  * Use the `rate()` function in Prometheus to calculate the rate of increase of a Counter.
  * By convention, the names of Counters are suffixed by `_total`.
  *
  * An Example Counter without Labels:
  * {{{
  *   for {
  *     cr <- CollectorRegistry.build[IO]
  *     successCounter <- Counter.noLabels(cr, "example_success_total", "Example Counter of Success")
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
  *     cr <- CollectorRegistry.build[IO]
  *     counter <- Counter.labelled(cr, "example_total", "Example Counter", Sized("foo"), {s: String => Sized(s)})
  *     _ <- counter.label("bar").inc
  *     _ <- counter.label("baz").inc
  *   } yield ()
  * }}}
  */
sealed abstract class Counter[F[_]]{

  /**
   * Access to the current value of this [[Counter]].
   */
  def get: F[Double]
  
  /**
   * Increment the value of this [[Counter]] by 1.
   */
  def inc: F[Unit]

  /**
   * Increment the value of this counter by the provided value.
   * 
   * @param d The value to increase the [[Counter]] by.
   * 
   */
  def incBy(d: Double): F[Unit]
}

/**
 * Counter Constructors, and Unsafe Counter Access
 */
object Counter {

  /**
   * Constructor for a Counter with no labels.
   * 
   * @param cr CollectorRegistry this [[Counter]] will be registered with
   * @param name The name of the Counter -  By convention, the names of Counters are suffixed by `_total`.
   * @param help The help string of the metric
   */
  def noLabels[F[_]: Sync](cr: CollectorRegistry[F], name: Name, help: String): F[Counter[F]] = for {
    c <- Sync[F].delay(JCounter.build().name(name.getName).help(help))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new NoLabelsCounter[F](out)

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
    labels: Sized[IndexedSeq[Name], N], 
    f: A => Sized[IndexedSeq[String], N]
  ): F[UnlabelledCounter[F, A]] = for {
      c <- Sync[F].delay(JCounter.build().name(name.getName).help(help).labelNames(labels.map(_.getName):_*))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new UnlabelledCounter[F, A](out, f.andThen(_.unsized))

  private final class NoLabelsCounter[F[_]: Sync] private[Counter] (private[Counter] val underlying: JCounter) extends Counter[F] {
    override def get: F[Double] = Sync[F].delay(underlying.get)
  
    override def inc: F[Unit] = Sync[F].delay(underlying.inc)
    override def incBy(d: Double): F[Unit] = Sync[F].delay(underlying.inc(d))
  }

  private final class LabelledCounter[F[_]: Sync] private[Counter] (private[Counter] val underlying: JCounter.Child) extends Counter[F] {
    override def get: F[Double] = Sync[F].delay(underlying.get)
  
    def inc: F[Unit] = Sync[F].delay(underlying.inc)
    def incBy(d: Double): F[Unit] = Sync[F].delay(underlying.inc(d))
  }

  /**
   * Generic Unlabeled Counter 
   * 
   * It is necessary to apply a value of type `A` to this
   * counter to be able to take any measurements.
   */
  final class UnlabelledCounter[F[_]: Sync, A] private[Counter](
    private[Counter] val underlying: JCounter, 
    private val f: A => IndexedSeq[String]
  ) {
    def label(a: A): Counter[F] =
      new LabelledCounter(underlying.labels(f(a):_*))
  }

  object Unsafe {
    def asJavaUnlabelled[F[_], A](c: UnlabelledCounter[F, A]): JCounter = c.underlying
    def asJava[F[_]: ApplicativeError[?[_], Throwable]](c: Counter[F]): F[JCounter] = c match {
      case _: LabelledCounter[F] => ApplicativeError[F, Throwable].raiseError(new IllegalArgumentException("Cannot Get Underlying Parent with Labels Applied"))
      case n: NoLabelsCounter[F] => n.underlying.pure[F]
    }
  }
}