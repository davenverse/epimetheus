package io.chrisdavenport.epimetheus

import scala.compiletime.ops.int.*

type Sized[+A, N <: Int] <: Tuple = N match {
  case 0 => EmptyTuple
  case S[n] => A *: Sized[A, n]
}

extension [A, N <: Int] (s: Sized[A, N]) {
  def unsized: IndexedSeq[A] = s.toList.toIndexedSeq.asInstanceOf[IndexedSeq[A]]
}
