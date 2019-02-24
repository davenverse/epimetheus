---
layout: docs
number: 2
title: Big Picture
---

# {{page.title}}

**Epimetheus** is a functional API placed over the underyling [Prometheus Java Client](https://github.com/prometheus/client_java).

This underlying library has little to no type-safety and safe interactions, the primary issues are

1. Invalid `Collector` Registration

2. Allowing references to be invalidated.

3. Applying a different number of labels, from the labels on the `Collector`

4. Invalid arguments for constructors. Such as an invalid `Quantile`.

## Generalized Solutions

Across all metrics we solve these problems with a few solutions.

1. Registration is always done in an effect. If two collectors with the same name are registered, it will
fail at registration. If you are building custom groups of Collectors, exposing a prefix that can be altered
is suggested in case users wish to register more than one.

2. We hide all reference invalidation components of the underlying API.

3. We utilize `Sized` from Shapeless to guarantee at compile time that the labels applied to the Metric
are the same size as the labels in the Metric.

4. We provide safe constructors for arguments, such as `Quantile`.

## Data Model

Prometheus fundamentally stores all data as time series: streams of timestamped values belonging to the same metric and the same set of labeled dimensions. Besides stored time series, Prometheus may generate temporary derived time series as the result of queries.

[Reference Documentation](https://prometheus.io/docs/concepts/data_model/)

### Metric Names and Labels

Every time series is uniquely identified by its _metric name_ and a set of _key-value pairs_, also known as _labels_.

#### Names

The metric name specifies the general feature of a system that is measured (e.g. http_requests_total - the total number of HTTP requests received). It may contain ASCII letters and digits, as well as underscores and colons. It must match the regex `[a-zA-Z_:][a-zA-Z0-9_:]*`.

Note: The colons are reserved for user defined recording rules. They should not be used by exporters or direct instrumentation.

#### Labels

Labels enable Prometheus's dimensional data model: any given combination of labels for the same metric name identifies a particular dimensional instantiation of that metric (for example: all HTTP requests that used the method POST to the /api/tracks handler). The query language allows filtering and aggregation based on these dimensions. Changing any label value, including adding or removing a label, will create a new time series.

Label names may contain ASCII letters, numbers, as well as underscores. They must match the regex `[a-zA-Z_][a-zA-Z0-9_]*`. Label names beginning with `__` are reserved for internal use.

Label values may contain any Unicode characters.

#### [Best Practices](https://prometheus.io/docs/practices/naming/)

## Metric Types

We offer four core metric types.

[Reference Documentation](https://prometheus.io/docs/concepts/metric_types/)

### Counter

A `Counter` is a cumulative metric that represents a single monotonically increasing counter whose value can only increase or be reset to zero on restart. For example, you can use a counter to represent the number of requests served, tasks completed, or errors.

Do not use a counter to expose a value that can decrease. For example, do not use a counter for the number of currently running processes; instead use a gauge.

### Gauge

A `Gauge` is a metric that represents a single numerical value that can arbitrarily go up and down.

Gauges are typically used for measured values like temperatures or current memory usage, but also "counts" that can go up and down, like the number of concurrent requests.

### Histogram

A `Histogram` samples observations (usually things like request durations or response sizes) and counts them in configurable buckets. It also provides a sum of all observed values.

A histogram with a base metric name of `<basename>` exposes multiple time series during a scrape:

- Cumulative counters for the observation buckets, exposed as `<basename>_bucket{le="<upper inclusive bound>"}`
- The total sum of all observed values, exposed as `<basename>_sum`
- The count of events that have been observed, exposed as `<basename>_count` (identical to `<basename>_bucket{le="+Inf"}` above)

Use the histogram_quantile() function to calculate quantiles from histograms or even aggregations of histograms.

### Summary

Similar to a histogram, a `Summary` samples observations (usually things like request durations and response sizes). While it also provides a total count of observations and a sum of all observed values, it calculates configurable quantiles over a sliding time window.

A summary with a base metric name of `<basename>` exposes multiple time series during a scrape:

- Streaming φ-quantiles (0 ≤ φ ≤ 1) of observed events, exposed as `<basename>{quantile="<φ>"}`
- The total sum of all observed values, exposed as `<basename>_sum`
- The count of events that have been observed, exposed as `<basename>_count`

### [Histogram and Summary Differences](https://prometheus.io/docs/practices/histograms/)
