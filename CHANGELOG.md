# changelog

This file summarizes **notable** changes for each release, but does not describe internal changes unless they are particularly exciting. This change log is ordered chronologically, so each release contains all changes described below it.

----

## <a name="Unreleased"></a>Unreleased Changes

## <a name="0.2.0"></a>New and Noteworthy for Version 0.2.0

Initial Stable Release of Epimetheus. This exposes a set of tools for safely abstacting over Prometheus metrics.
We start with the epic of epimetheus to walk through all the tools you need to know, where we expose the core mechanics.

A central collection of the shared mutable state of the metrics being aggregated called a `PrometheusRegistry`.

There are 4 metric types Counter, Gauge, Histogram and Summary.

We expose safe builders, and macro assisted tooling for safe creation metric and label `Name`'s, as well as `Summary`'s `Quantile`.

Finally each labelled metric creation is guarded by an appropriated sized relationship backed by shapeless' `Sized`. Ensuring that you will always apply the same number of labels, as the number of label names you make the metric with.

We hope to provide the tools to make working with Prometheus a joy, with pure functional programming as the backbone.

- simpleclient 0.6.0
- cats-core 1.6.0
- cats-effect 1.2.0
- shapeless 2.3.3

## <a name="0.1.0"></a>New and Noteworthy for Version 0.1.0

This is a backport of the 0.2.0 branch for those still stuck on cats-effect 0.10.1. In this you lose the convenience constructors for timing. This is a legacy port at release.

- simpleclient 0.6.0
- cats-core 1.6.0
- cats-effect 0.10.1
- shapeless 2.3.3