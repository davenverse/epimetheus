package io.chrisdavenport.epimetheus

import scala.compiletime.ops.int.*

trait ShapelessPolyfill {

  type Represented[R] = R match {
    case IndexedSeq[a] => a
  }

  type TupleSized[R, N <: Int] <: Tuple = N match {
    case 0 => EmptyTuple
    case S[n] => Represented[R] *: TupleSized[R, n]
  }

  extension [R, N <: Int] (s: TupleSized[R, N]) {
    def unsized: IndexedSeq[Represented[R]] = s.toList.toIndexedSeq.asInstanceOf[IndexedSeq[Represented[R]]]
  }

  type Nat = Int

  type Sized[+Repr, L <: Nat] = TupleSized[Repr, L]

}
