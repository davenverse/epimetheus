package io.chrisdavenport.epimetheus

import scala.collection.immutable.ListMap

trait SeqMapPolyfill {

  type SeqMap[K, +V] = ListMap[K, V]
  val SeqMap = ListMap

}
