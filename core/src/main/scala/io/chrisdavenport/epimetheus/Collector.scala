package io.chrisdavenport.epimetheus

import io.prometheus.client.{Collector => JCollector}

final class Collector private (private val underlying: JCollector)
object Collector {
  def fromJava(j: JCollector): Collector = new Collector(j)

  object Unsafe {
    def asJava(c: Collector): JCollector = c.underlying
  }
}
