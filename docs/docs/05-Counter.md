# Counter

Counter metric, to track counts, running totals, or events.

If your use case can go up or down consider using a `Gauge` instead.
Use the `rate()` function in Prometheus to calculate the rate of increase of a Counter.
By convention, the names of Counters are suffixed by `_total`. This suffix is added to metric's name by underlying prometheus-metrics library, do not add it yourself.

A `Counter` is a cumulative metric that represents a single monotonically increasing counter whose value can only increase or be reset to zero on restart. For example, you can use a counter to represent the number of requests served, tasks completed, or errors.

Do not use a counter to expose a value that can decrease. For example, do not use a counter for the number of currently running processes; instead use a gauge.

Imports

```scala mdoc:silent
import io.chrisdavenport.epimetheus._
import cats.effect._

import cats.effect.unsafe.implicits.global
```

An Example Counter without Labels:

```scala mdoc
val noLabelsExample = {
  for {
    pr <- PrometheusRegistry.build[IO]
    successCounter <- Counter.noLabels(
      pr,
      Name("example_success"),
      "Example Counter of Success"
    )
    failureCounter <- Counter.noLabels(
      pr,
      Name("example_failure"),
      "Example Counter of Failure"
    )
    _ <- IO(println("Action Here")).guaranteeCase{
      case Outcome.Succeeded(_) => successCounter.inc
      case _ => failureCounter.inc
    }
    out <- pr.write004
  } yield out
}

noLabelsExample.unsafeRunSync()
```

An Example of a Counter with Labels:

```scala mdoc
val labelledExample = {
  for {
    pr <- PrometheusRegistry.build[IO]
    counter <- Counter.labelled(
      pr,
      Name("example"),
      "Example Counter",
      Sized(Label("foo")),
      {s: String => Sized(s)}
    )
    _ <- counter.label("bar").inc
    _ <- counter.label("baz").inc
    out <- pr.write004
  } yield out
}

labelledExample.unsafeRunSync()
```

An Example of a Counter backed algebra.

```scala mdoc
sealed trait Foo
case object Bar extends Foo
case object Baz extends Foo

def fooLabel(f: Foo) = {
  f match {
    case Bar => Sized("bar")
    case Baz => Sized("baz")
  }
}

trait FooAlg[F[_]]{
  def bar: F[Unit]
  def baz: F[Unit]
}

object FooAlg {
  def impl[F[_]](c: Counter.UnlabelledCounter[F, Foo]) = new FooAlg[F]{
    def bar: F[Unit] = c.label(Bar).inc
    def baz: F[Unit] = c.label(Baz).inc
  }
}

val fooAgebraExample = {
  for {
    pr <- PrometheusRegistry.build[IO]
    counter <- Counter.labelled(
      pr,
      Name("example"),
      "Example Counter",
      Sized(Label("foo")),
      fooLabel
    )
    foo = FooAlg.impl(counter)
    _ <- foo.bar
    _ <- foo.bar
    _ <- foo.baz
    out <- pr.write004
  } yield out
}

fooAgebraExample.unsafeRunSync()
```

We force labels to always match the same size. This will fail to compile.

```scala
def incorrectlySized[F[_]: Sync](pr: PrometheusRegistry[F]) = {
  Counter.labelled(pr, Name("fail"), "Example Failure", Sized(Label("color"), Name("method")), {s: String => Sized(s)})
}
```
