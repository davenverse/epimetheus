package io.chrisdavenport.epimetheus

import cats.implicits._
import cats.effect._
import io.prometheus.client.{Counter => JCounter}

/**
  * Counter - Track counts, running totals, or events.
  *
  * If your use case can go up or down consider using a [[Gauge]] instead.
  *
  * By convention, the names of Counters are suffixed by <code>_total</code>.
  */
final class Counter[F[_]: Sync] private (private val counter: JCounter.Child) {
  def get: F[Double] = Sync[F].delay(counter.get)
  def inc: F[Unit] = Sync[F].delay(counter.inc)
  def incBy(d: Double): F[Unit] = Sync[F].delay(counter.inc(d))
}

object Counter {

  def build[F[_]: Sync](cr: CollectorRegistry, name: String, help: String): F[Counter[F]] = for {
    c <- Sync[F].delay(JCounter.build().name(name).help(help))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new SafeUnlabelledCounter[F, Unit](out, _ => List.empty).label(())

  def build1[F[_]: Sync](cr: CollectorRegistry, name: String, help: String)(label1: String): F[SafeUnlabelledCounter[F,String]] = 
    for {
      c <- Sync[F].delay(JCounter.build().name(name).help(help).labelNames(label1))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new SafeUnlabelledCounter[F, String](out, {s1: String => List(s1)})

  def build2[F[_]: Sync](cr: CollectorRegistry, name: String, help: String)(label1: String, label2: String): F[SafeUnlabelledCounter[F,(String, String)]] = 
    for {
      c <- Sync[F].delay(JCounter.build().name(name).help(help).labelNames(label1, label2))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new SafeUnlabelledCounter[F, (String, String)](out, {case (s1: String, s2: String) => List(s1,s2)})


  /**
   * Labels and the string returned by f MUST have the same size
   *  or else `label` will fail
   * FUTURE IMPROVEMENT: Size these lists to make this safe.
   */
  def construct[F[_]: Sync, A](
    cr: CollectorRegistry, 
    name: String, 
    help: String, 
    labels: List[String], 
    f: A => List[String]
  ): F[UnlabelledCounter[F, A]] = for {
      c <- Sync[F].delay(JCounter.build().name(name).help(help).labelNames(labels:_*))
    out <- Sync[F].delay(c.register(CollectorRegistry.Unsafe.asJava(cr)))
  } yield new UnlabelledCounter[F, A](out, f)


  /**
   * Generic Unlabeled Counter
   * 
   * Unsafe Conversion as labels may not align so `label` operation
   * can fail
   */
  final class UnlabelledCounter[F[_]: Sync, A] private[Counter](
    private[Counter] val c: JCounter, 
    private val f: A => List[String]
  ) {
    def label(a: A): F[Counter[F]] =
      Sync[F].delay(c.labels(f(a):_*)).map(new Counter(_))
  }

  final class SafeUnlabelledCounter[F[_]: Sync, A] private[Counter](
    private[Counter] val c: JCounter, 
    private val f: A => List[String]
  ) {
    def label(a: A): Counter[F] = new Counter[F](c.labels(f(a):_*))
  }
}