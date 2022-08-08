package io.chrisdavenport.epimetheus

trait SeqMapPolyfill {

  type SeqMap[K, +V] = scala.collection.immutable.SeqMap[K, V]
  val SeqMap = scala.collection.immutable.SeqMap
}
