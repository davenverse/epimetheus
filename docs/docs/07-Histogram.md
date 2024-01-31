# Histogram

Histogram metric, to track distributions of events.

Note: Each bucket is one timeseries. Many buckets and/or many dimensions with labels
can produce large amount of time series, that may cause performance problems.

The default buckets are intended to cover a typical web/rpc request from milliseconds to seconds. Defaults are .005, .01, .025, .05, .075, .1, .25, .5, .75, 1, 2.5, 5, 7.5, 10

A `Histogram` samples observations (usually things like request durations or response sizes) and counts them in configurable buckets. It also provides a sum of all observed values.

A histogram with a base metric name of `<basename>` exposes multiple time series during a scrape:

- Cumulative counters for the observation buckets, exposed as `<basename>_bucket{le="<upper inclusive bound>"}`
- The total sum of all observed values, exposed as `<basename>_sum`
- The count of events that have been observed, exposed as `<basename>_count` (identical to `<basename>_bucket{le="+Inf"}` above)

Use the histogram_quantile() function to calculate quantiles from histograms or even aggregations of histograms.

See https://prometheus.io/docs/practices/histograms/ for more information on Histogram and Summary similarities and differences.

Imports

```scala mdoc:silent
import io.chrisdavenport.epimetheus._
import io.chrisdavenport.epimetheus.implicits._
import cats.effect._

import scala.concurrent.duration._

import cats.effect.unsafe.implicits.global
```

And Example of a Histogram with no labels:

```scala mdoc
val noLabelsHistogramExample = {
  for {
    pr <- PrometheusRegistry.build[IO]
    h <- Histogram.noLabels(pr, Name("example_histogram"), "Example Histogram")
    _ <- h.observe(0.2)
    _ <- h.timed(Temporal[IO].sleep(1.second), SECONDS)
    currentMetrics <- pr.write004
    _ <- IO(println(currentMetrics))
  } yield ()
}

noLabelsHistogramExample.unsafeRunSync()
```

An Example of a Histogram with labels:

```scala mdoc
val labelledHistogramExample = {
  for {
    pr <- PrometheusRegistry.build[IO]
    h <- Histogram.labelled(
      pr,
      Name("example_histogram"),
      "Example Histogram",
      Sized(Label("foo")),
      {s: String => Sized(s)}
    )
    _ <- h.label("bar").observe(0.2)
    _ <- h.label("baz").timed(Temporal[IO].sleep(1.second), SECONDS)
    currentMetrics <- pr.write004
    _ <- IO(println(currentMetrics))
  } yield ()
}

labelledHistogramExample.unsafeRunSync()
```
