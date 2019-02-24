---
layout: docs
number: 4
title: Gauge
---

# {{page.title}}

Gauge metric, to report instantaneous values.

Gauges can go both up and down.

Labelled versions can be aggregated and processed together much more easily in the Prometheus
server than individual metrics for each labelset.

An Example of a Gauge with no labels:

```tut:book
val noLabelsGaugeExample = {
  for {
    cr <- CollectorRegistry.build[IO]
    gauge <- Gauge.noLabels(cr, "gauge_total", "Example Gauge")
    _ <- gauge.inc
    _ <- gauge.inc
    _ <- gauge.dec
    currentMetrics <- cr.write004
  } yield currentMetrics
}

noLabelsGaugeExample.unsafeRunSync
```

An Example of a Gauge with labels:

```tut:book
val labelledGaugeExample = {
  for {
    cr <- CollectorRegistry.build[IO]
    gauge <- Gauge.labelled(cr, "gauge_total", "Example Gauge", Sized("foo"), {s: String => Sized(s)})
    _ <- gauge.label("bar").inc
    _ <- gauge.label("baz").inc
    _ <- gauge.label("bar").inc
    _ <- gauge.label("baz").inc
    _ <- gauge.label("bar").dec
    currentMetrics <- cr.write004
  } yield currentMetrics
}

labelledGaugeExample.unsafeRunSync
```