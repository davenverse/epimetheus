package io.chrisdavenport.epimetheus

import cats.effect._
import io.prometheus.client.{CollectorRegistry => JCollectorRegistry}

final class CollectorRegistry private(private val cr: JCollectorRegistry)
object CollectorRegistry {
  def build[F[_]: Sync]: F[CollectorRegistry] = 
    Sync[F].delay(new CollectorRegistry(new JCollectorRegistry))

  object Unsafe {
    def asJava(c: CollectorRegistry): JCollectorRegistry = c.cr
  }
}