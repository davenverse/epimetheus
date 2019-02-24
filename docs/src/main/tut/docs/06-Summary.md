---
layout: docs
number: 6
title: Summary
---

# {{page.title}}


Imports

```tut:silent
import io.chrisdavenport.epimetheus._
import cats.effect._
import shapeless._

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

implicit val CS: ContextShift[IO] = IO.contextShift(global)
implicit val T: Timer[IO] = IO.timer(global)
```

And Example of a Summary with no labels:

```tut:book
val noLabelsSummaryExample = {
  for {
    cr <- CollectorRegistry.build[IO]
    s <- Summary.noLabels(cr, "example_summary", "Example Summary", Summary.quantile(0.5,0.05))
    _ <- s.observe(0.1)
    _ <- s.observe(0.2)
    _ <- s.observe(1.0)
    currentMetrics <- cr.write004
    _ <- IO(println(currentMetrics))
  } yield ()
}

noLabelsSummaryExample.unsafeRunSync
```

An Example of a Summary with labels:

```tut:book
val noLabelsSummaryExample = {
  for {
    cr <- CollectorRegistry.build[IO]
    s <- Summary.labelled(cr, "example_summary", "Example Summary",
      Sized("foo"), {s: String => Sized(s)},
      Summary.quantile(0.5,0.05)
    )
    _ <- s.label("bar").observe(0.1)
    _ <- s.label("baz").observe(0.2)
    _ <- s.label("baz").observe(1.0)
    currentMetrics <- cr.write004
    _ <- IO(println(currentMetrics))
  } yield ()
}

noLabelsSummaryExample.unsafeRunSync
```