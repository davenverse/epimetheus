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
