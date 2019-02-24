---
layout: docs
number: 3
title: Counter
---

# {{page.title}}

Counter metric, to track counts, running totals, or events.

If your use case can go up or down consider using a [[Gauge]] instead.
Use the `rate()` function in Prometheus to calculate the rate of increase of a Counter.
By convention, the names of Counters are suffixed by `_total`.

Imports

```tut:silent
import io.chrisdavenport.epimetheus._
import cats.effect._
import shapeless._
```

An Example Counter without Labels:

```tut:book
val noLabelsExample = {
  for {
    cr <- CollectorRegistry.build[IO]
    successCounter <- Counter.noLabels(cr, "example_success_total", "Example Counter of Success")
    failureCounter <- Counter.noLabels(cr, "example_failure_total", "Example Counter of Failure")
    _ <- IO(println("Action Here")).guaranteeCase{
      case ExitCase.Completed => successCounter.inc
      case _ => failureCounter.inc
    }
    out <- cr.write004
  } yield yield out
}

noLabelsExample.unsafeRunSync
```

An Example of a Counter with Labels:

```tut:book
val labelledExample = {
  for {
    cr <- CollectorRegistry.build[IO]
    counter <- Counter.labelled(cr, "example_total", "Example Counter", Sized("foo"), {s: String => Sized(s)})
    _ <- counter.label("bar").inc
    _ <- counter.label("baz").inc
    out <- cr.write004
  } yield out
}

labelledExample.unsafeRunSync
```

An Example of a Counter backed algebra.

```tut:book
sealed trait Foo; case object Bar extends Foo; case object Baz extends Foo;

def fooLabel(f: Foo) = {
  f match {
    case Bar => Sized("bar")
    case Baz => Sized("baz")
  }
}

trait FooAlg[F[_]]{
  def bar: F[Unit]
  def baz: F[Unit]
}; object FooAlg {
  def impl[F[_]: Sync](c: Counter.UnlabelledCounter[F, Foo]) = new FooAlg[F]{
    def bar: F[Unit] = c.label(Bar).inc
    def baz: F[Unit] = c.label(Baz).inc
  }
}

val fooAgebraExample = {
  for {
    cr <- CollectorRegistry.build[IO]
    counter <- Counter.labelled(cr, "example_total", "Example Counter", Sized("foo"), fooLabel)
    foo = FooAlg.impl(counter)
    _ <- foo.bar
    _ <- foo.bar
    _ <- foo.baz
    out <- cr.write004
  } yield out
}

fooAgebraExample.unsafeRunSync
```

We force labels to always match the same size. This will fail to compile.

```tut:nofail
def incorrectlySized[F[_]: Sync](cr: CollectorRegistry[F]) = {
  Counter.labelled(cr, "fail", "Example Failure", Sized("color", "method"), {s: String => Sized(s)})
}
```