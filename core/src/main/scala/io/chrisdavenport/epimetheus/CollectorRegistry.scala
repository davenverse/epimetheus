package io.chrisdavenport.epimetheus

import cats.effect._
import cats.implicits._
import io.prometheus.client.{CollectorRegistry => JCollectorRegistry}

import java.io.StringWriter
import io.prometheus.client.exporter.common.TextFormat

final class CollectorRegistry[F[_]: Sync] private(private val cr: JCollectorRegistry){
  def register(c: Collector): F[Unit] = 
    Sync[F].delay(cr.register(Collector.Unsafe.asJava(c)))

  def unregister(c: Collector): F[Unit] =
    Sync[F].delay(cr.unregister(Collector.Unsafe.asJava(c)))

  def write004: F[String] = Sync[F].delay {
    val writer = new StringWriter
    TextFormat.write004(writer, cr.metricFamilySamples)
    writer.toString
  }

}
object CollectorRegistry {

  def build[F[_]: Sync]: F[CollectorRegistry[F]] = 
    Sync[F].delay(new CollectorRegistry(new JCollectorRegistry))

  // Future Work: 
  // https://github.com/prometheus/client_java/commit/fcde9554d759c397775298c64f99c0c169c8111b
  // Makes aviailable a default registration we can use to keep in line
  // with future changes.
  def buildWithDefaults[F[_]: Sync]: F[CollectorRegistry[F]] = 
    for {
      cr  <- build[F]
      bpe <- Collector.Defaults.BufferPoolsExports
      _   <- cr.register(bpe) 
      cle <- Collector.Defaults.ClassLoadingExports
      _   <- cr.register(cle)
      gce <- Collector.Defaults.GarbageCollectorExports
      _   <- cr.register(gce)
      mae <- Collector.Defaults.MemoryAllocationExports
      _   <- cr.register(mae)
      mpe <- Collector.Defaults.MemoryPoolsExports
      _   <- cr.register(mpe)
      se  <- Collector.Defaults.StandardExports
      _   <- cr.register(se)
      te  <- Collector.Defaults.ThreadExports
      _   <- cr.register(te)
      vie <- Collector.Defaults.VersionInfoExports
      _   <- cr.register(vie)
    } yield cr

  def defaultRegistry[F[_]: Sync]: CollectorRegistry[F] =
    Unsafe.fromJava(JCollectorRegistry.defaultRegistry)

  object Unsafe {
    def fromJava[F[_]: Sync](j: JCollectorRegistry): CollectorRegistry[F] = new CollectorRegistry[F](j)
    def asJava[F[_]](c: CollectorRegistry[F]): JCollectorRegistry = c.cr
  }
}