# Data Model

Prometheus fundamentally stores all data as time series: streams of timestamped values belonging to the same metric and the same set of labeled dimensions. Besides stored time series, Prometheus may generate temporary derived time series as the result of queries.

[Reference Documentation](https://prometheus.io/docs/concepts/data_model/)

## Metric Names and Labels

Every time series is uniquely identified by its _metric name_ and a set of _key-value pairs_, also known as _labels_.

### Names

The metric name specifies the general feature of a system that is measured (e.g. http_requests_total - the total number of HTTP requests received). It may contain ASCII letters and digits, as well as underscores and colons. It must match the regex `[a-zA-Z_:][a-zA-Z0-9_:]*`.

Note: The colons are reserved for user defined recording rules. They should not be used by exporters or direct instrumentation.

### Labels

Labels enable Prometheus's dimensional data model: any given combination of labels for the same metric name identifies a particular dimensional instantiation of that metric (for example: all HTTP requests that used the method POST to the /api/tracks handler). The query language allows filtering and aggregation based on these dimensions. Changing any label value, including adding or removing a label, will create a new time series.

Label names may contain ASCII letters, numbers, as well as underscores. They must match the regex `[a-zA-Z_][a-zA-Z0-9_]*`. Label names beginning with `__` are reserved for internal use.

Label values may contain any Unicode characters.

### [Best Practices](https://prometheus.io/docs/practices/naming/)
