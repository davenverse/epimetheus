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
      Sync[F].delay(JvmMetrics.builder().register(PrometheusRegistry.Unsafe.asJava(pr)))

    def registerJvmBufferPoolMetrics[F[_]: Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(JvmBufferPoolMetrics.builder().register(PrometheusRegistry.Unsafe.asJava(pr)))
    def registerJvmClassLoadingMetrics[F[_]: Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(JvmClassLoadingMetrics.builder().register(PrometheusRegistry.Unsafe.asJava(pr)))
    def registerJvmCompilationMetrics[F[_]: Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(JvmCompilationMetrics.builder().register(PrometheusRegistry.Unsafe.asJava(pr)))
    def registerJvmGarbageCollectorMetrics[F[_]: Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(JvmGarbageCollectorMetrics.builder().register(PrometheusRegistry.Unsafe.asJava(pr)))
    def registerJvmMemoryMetrics[F[_] : Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(JvmMemoryMetrics.builder().register(PrometheusRegistry.Unsafe.asJava(pr)))
    def registerJvmMemoryPoolAllocationMetrics[F[_]: Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(JvmMemoryPoolAllocationMetrics.builder().register(PrometheusRegistry.Unsafe.asJava(pr)))
    def registerProcessMetrics[F[_] : Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(ProcessMetrics.builder().register(PrometheusRegistry.Unsafe.asJava(pr)))
    def registerJvmRuntimeInfoMetric[F[_] : Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(JvmRuntimeInfoMetric.builder().register(PrometheusRegistry.Unsafe.asJava(pr)))
    def registerJvmThreadsMetrics[F[_]: Sync](pr: PrometheusRegistry[F]): F[Unit] =
      Sync[F].delay(JvmThreadsMetrics.builder().register(PrometheusRegistry.Unsafe.asJava(pr)))
  }

  object Unsafe {
    def fromJava(j: JCollector): Collector = new Collector(j)
    def asJava(c: Collector): JCollector = c.underlying
  }
}
