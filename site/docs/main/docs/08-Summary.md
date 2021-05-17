---
layout: docs
number: 8
title: Summary
---

# {{page.title}}

Summary metric, to track the size of events.

The quantiles are calculated over a sliding window of time. There are two options to configure this time window:

- maxAgeSeconds: Long -  The duration of the time window, i.e. how long observations are kept before they are discarded. Default is 10 minutes.

- ageBuckets: Int - The number of buckets used to implement the sliding time window. If your time window is 10 minutes, and you have ageBuckets=5, buckets will be switched every 2 minutes. The value is a trade-off between resources (memory and cpu for maintaining the bucket) and how smooth the time window is moved. Default value is 5.

Similar to a histogram, a `Summary` samples observations (usually things like request durations and response sizes). While it also provides a total count of observations and a sum of all observed values, it calculates configurable quantiles over a sliding time window.

A summary with a base metric name of `<basename>` exposes multiple time series during a scrape:

- Streaming φ-quantiles (0 ≤ φ ≤ 1) of observed events, exposed as `<basename>{quantile="<φ>"}`
- The total sum of all observed values, exposed as `<basename>_sum`
- The count of events that have been observed, exposed as `<basename>_count`

See https://prometheus.io/docs/practices/histograms/ for more info on quantiles.

Imports

```scala mdoc:silent
import io.chrisdavenport.epimetheus._
import io.chrisdavenport.epimetheus.implicits._
import cats.effect._
import shapeless._

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

implicit val CS: ContextShift[IO] = IO.contextShift(global)
implicit val T: Timer[IO] = IO.timer(global)
```

And Example of a Summary with no labels:

```scala mdoc
val noLabelsSummaryExample = {
  for {
    cr <- CollectorRegistry.build[IO]
    s <- Summary.noLabels(
      cr,
      Name("example_summary"),
      "Example Summary",
      Summary.quantile(0.5,0.05)
    )
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

```scala mdoc
val noLabelsSummaryExample = {
  for {
    cr <- CollectorRegistry.build[IO]
    s <- Summary.labelled(
      cr,
      Name("example_summary"),
      "Example Summary",
      Sized(Label("foo")),
      {s: String => Sized(s)},
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
