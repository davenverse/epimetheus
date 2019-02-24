---
layout: docs
number: 5
title: Histogram
---

# {{page.title}}

Histogram metric, to track distributions of events.

Note: Each bucket is one timeseries. Many buckets and/or many dimensions with labels
can produce large amount of time series, that may cause performance problems.

The default buckets are intended to cover a typical web/rpc request from milliseconds to seconds.

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

And Example of a Histogram with no labels:

```tut:book
val noLabelsHistogramExample = {
  for {
    cr <- CollectorRegistry.build[IO]
    h <- Histogram.noLabels(cr, "example_histogram", "Example Histogram")
    _ <- h.observe(0.2)
    _ <- h.timed(T.sleep(1.second), SECONDS)
    currentMetrics <- cr.write004
    _ <- IO(println(currentMetrics))
  } yield ()
}

noLabelsHistogramExample.unsafeRunSync
```

An Example of a Histogram with labels:

```tut:book
val labelledHistogramExample = {
  for {
    cr <- CollectorRegistry.build[IO]
    h <- Histogram.labelled(cr, "example_histogram", "Example Histogram", Sized("foo"), {s: String => Sized(s)})
    _ <- h.label("bar").observe(0.2)
    _ <- h.label("baz").timed(T.sleep(1.second), SECONDS)
    currentMetrics <- cr.write004
    _ <- IO(println(currentMetrics))
  } yield ()
}

labelledHistogramExample.unsafeRunSync
```