package io.chrisdavenport.epimetheus

import cats.effect._
import io.prometheus.client.{CollectorRegistry => JCollectorRegistry}

final class CollectorRegistry[F[_]: Sync] private(private val cr: JCollectorRegistry){
  def register(c: Collector): F[Unit] = 
    Sync[F].delay(cr.register(Collector.Unsafe.asJava(c)))

  def unrgister(c: Collector): F[Unit] =
    Sync[F].delay(cr.unregister(Collector.Unsafe.asJava(c)))
}
object CollectorRegistry {



  def build[F[_]: Sync]: F[CollectorRegistry[F]] = 
    Sync[F].delay(new CollectorRegistry(new JCollectorRegistry))

  object Unsafe {
    def fromJava[F[_]: Sync](j: JCollectorRegistry): CollectorRegistry[F] = new CollectorRegistry[F](j)
    def asJava[F[_]](c: CollectorRegistry[F]): JCollectorRegistry = c.cr
  }
}