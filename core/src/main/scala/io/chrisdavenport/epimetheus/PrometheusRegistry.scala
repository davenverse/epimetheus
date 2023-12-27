package io.chrisdavenport.epimetheus

import cats.effect._
import cats.implicits._
import io.prometheus.metrics.model.registry.{PrometheusRegistry => JPrometheusRegistry}

/**
 * A [[PrometheusRegistry]] is a registry of Collectors.
 *
 * It represents the concurrently shared state which holds the information
 * of the metrics in question.
 *
 * ==On Creation==
 * Due to how prometheus scraping occurs, only one PrometheusRegistry is generally useful per
 * application. There are generally 2 approaches.
 *
 * 1. Create your own registry. Register Metrics with it. Expose that.
 *    Advantages: Full Control Of the Code
 * 2. Use the global [[PrometheusRegistry.defaultRegistry defaultRegistry]]
 *    Advantages: Easier Interop with Java libraries that may not give
 *    an option for interaction with arbitrary PrometheusRegistries.
 */
final class PrometheusRegistry[F[_]: Sync] private(private val pr: JPrometheusRegistry){
  /**
   * Register A [[Collector]] with this Collector Registry
   */
  def register(c: Collector): F[Unit] =
    Sync[F].delay(pr.register(Collector.Unsafe.asJava(c)))

  /**
   * Unregister A [[Collector]] with this PrometheusRegistry
   */
  def unregister(c: Collector): F[Unit] =
    Sync[F].delay(pr.unregister(Collector.Unsafe.asJava(c)))

  /**
   * Get Java PrometheusRegistry
   */
  def underlying: JPrometheusRegistry = pr
}
object PrometheusRegistry {

  /**
   * Build an Empty PrometheusRegistry
   */
  def build[F[_]: Sync]: F[PrometheusRegistry[F]] =
    Sync[F].delay(new PrometheusRegistry(new JPrometheusRegistry))

  /**
   * Build a PrometheusRegistry which has all of the [[Collector Collectors]] in
   * [[Collector.Defaults]] registered.
   *
   * This is simply a convenience function.
   */
  def buildWithDefaults[F[_]: Sync]: F[PrometheusRegistry[F]] =
    for {
      pr <- build[F]
      _ <- Collector.Defaults.registerDefaults(pr)
    } yield pr

  /**
   * Default Global Registry, what many Java interactions may
   * automatically register with, so may be necessary for those tools.
   */
  def defaultRegistry[F[_]: Sync]: PrometheusRegistry[F] =
    Unsafe.fromJava(JPrometheusRegistry.defaultRegistry)

  object Unsafe {
    def fromJava[F[_]: Sync](j: JPrometheusRegistry): PrometheusRegistry[F] = new PrometheusRegistry[F](j)
    def asJava[F[_]](c: PrometheusRegistry[F]): JPrometheusRegistry = c.pr
  }
}