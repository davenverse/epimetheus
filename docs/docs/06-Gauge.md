# Gauge

Gauge metric, to report instantaneous values.

A `Gauge` is a metric that represents a single numerical value that can arbitrarily go up and down.

Gauges are typically used for measured values like temperatures or current memory usage, but also "counts" that can go up and down, like the number of concurrent requests.

Labelled versions can be aggregated and processed together much more easily in the Prometheus
server than individual metrics for each labelset.

Imports

```scala mdoc:silent
import io.chrisdavenport.epimetheus._
import cats.effect._

import cats.effect.unsafe.implicits.global
```

An Example of a Gauge with no labels:

```scala mdoc
val noLabelsGaugeExample = {
  for {
    pr <- PrometheusRegistry.build[IO]
    gauge <- Gauge.noLabels(pr, Name("gaugetotal"), "Example Gauge")
    _ <- gauge.inc
    _ <- gauge.inc
    _ <- gauge.dec
    currentMetrics <- pr.write004
  } yield currentMetrics
}

noLabelsGaugeExample.unsafeRunSync()
```

An Example of a Gauge with labels:

```scala mdoc
val labelledGaugeExample = {
  for {
    pr <- PrometheusRegistry.build[IO]
    gauge <- Gauge.labelled(
      pr,
      Name("gaugetotal"),
      "Example Gauge",
      Sized(Label("foo")),
      {s: String => Sized(s)}
    )
    _ <- gauge.label("bar").inc
    _ <- gauge.label("baz").inc
    _ <- gauge.label("bar").inc
    _ <- gauge.label("baz").inc
    _ <- gauge.label("bar").dec
    currentMetrics <- pr.write004
  } yield currentMetrics
}

labelledGaugeExample.unsafeRunSync()
```
