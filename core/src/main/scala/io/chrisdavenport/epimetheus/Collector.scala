package io.chrisdavenport.epimetheus

import cats.implicits._
import cats.effect._
import io.prometheus.client.{Collector => JCollector}
import io.prometheus.client.hotspot._

/**
 * A [[Collector]] Represents a Metric or Group of Metrics that
 * can be registered with a [[CollectorRegistry]].
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
    def registerDefaults[F[_]: Sync](cr: CollectorRegistry[F]): F[Unit] = 
      for {
        bpe <- BufferPoolsExports[F]
        _   <- cr.register(bpe) 
        cle <- ClassLoadingExports
        _   <- cr.register(cle)
        gce <- GarbageCollectorExports
        _   <- cr.register(gce)
        mae <- MemoryAllocationExports
        _   <- cr.register(mae)
        mpe <- MemoryPoolsExports
        _   <- cr.register(mpe)
        se  <- StandardExports
        _   <- cr.register(se)
        te  <- ThreadExports
        _   <- cr.register(te)
        vie <- VersionInfoExports
        _   <- cr.register(vie)
      } yield ()

    def defaultCollectorRegisterDefaults[F[_]: Sync]: F[Unit] = Sync[F].delay{
      DefaultExports.initialize
    }

    def BufferPoolsExports[F[_]: Sync]: F[Collector] =
      Sync[F].delay(new BufferPoolsExports())
        .map(Unsafe.fromJava(_))
    def ClassLoadingExports[F[_]: Sync]: F[Collector] =
      Sync[F].delay(new ClassLoadingExports())
        .map(Unsafe.fromJava(_))
    def GarbageCollectorExports[F[_]: Sync]: F[Collector] =
      Sync[F].delay(new GarbageCollectorExports())
        .map(Unsafe.fromJava(_))
    def MemoryAllocationExports[F[_]: Sync]: F[Collector] = 
      Sync[F].delay(new MemoryAllocationExports())
        .map(Unsafe.fromJava(_))
    def MemoryPoolsExports[F[_]: Sync]: F[Collector] =
      Sync[F].delay(new MemoryPoolsExports())
        .map(Unsafe.fromJava(_))
    def StandardExports[F[_]: Sync]: F[Collector] =
      Sync[F].delay(new StandardExports())
        .map(Unsafe.fromJava(_))
    def ThreadExports[F[_]: Sync]: F[Collector] = 
      Sync[F].delay(new ThreadExports())
        .map(Unsafe.fromJava(_))
    def VersionInfoExports[F[_]: Sync]: F[Collector] = 
      Sync[F].delay(new VersionInfoExports())
        .map(Unsafe.fromJava(_))
  }

  object Unsafe {
    def fromJava(j: JCollector): Collector = new Collector(j)
    def asJava(c: Collector): JCollector = c.underlying
  }
}
