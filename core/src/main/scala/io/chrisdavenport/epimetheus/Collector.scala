package io.chrisdavenport.epimetheus

import cats.effect._
import io.prometheus.metrics.model.registry.{Collector => JCollector}
import io.prometheus.metrics.instrumentation.jvm._

/**
 * A [[Collector]] Represents a Metric or Group of Metrics that
 * can be registered with a [[PrometheusRegistry]].
 *
 * This is generally used for wrapping and bringing in Collectors
 * as defined for Java Components
 *
 */
final class Collector private (private val underlying: JCollector)
object Collector {
  /**
   * These are the set of HotSpot Metrics which can be
   * considered as expected defaults that would like
   * to be included generally.
   */
  object Defaults {
    /**
     * Register all defaults with the supplied registry.
     */
    def registerDefaults[F[_]: Sync](pr: PrometheusRegistry[F]): F[Unit] =
      registerJvmMetrics[F](pr)

    // registers all jvm metrics
    def registerJvmMetrics[F[_] : Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(JvmMetrics.builder().register(pr.underlying))

    def registerJvmBufferPoolMetrics[F[_]: Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(JvmBufferPoolMetrics.builder().register(pr.underlying))
    def registerJvmClassLoadingMetrics[F[_]: Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(JvmClassLoadingMetrics.builder().register(pr.underlying))
    def registerJvmCompilationMetrics[F[_]: Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(JvmCompilationMetrics.builder().register(pr.underlying))
    def registerJvmGarbageCollectorMetrics[F[_]: Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(JvmGarbageCollectorMetrics.builder().register(pr.underlying))
    def registerJvmMemoryMetrics[F[_] : Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(JvmMemoryMetrics.builder().register(pr.underlying))
    def registerJvmMemoryPoolAllocationMetrics[F[_]: Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(JvmMemoryPoolAllocationMetrics.builder().register(pr.underlying))
    def registerProcessMetrics[F[_] : Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(ProcessMetrics.builder().register(pr.underlying))
    def registerJvmRuntimeInfoMetric[F[_] : Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(JvmRuntimeInfoMetric.builder().register(pr.underlying))
    def registerJvmThreadsMetrics[F[_]: Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(JvmThreadsMetrics.builder().register(pr.underlying))
  }

  object Unsafe {
    def fromJava(j: JCollector): Collector = new Collector(j)
    def asJava(c: Collector): JCollector = c.underlying
  }
}
