package io.chrisdavenport.epimetheus

import org.specs2.mutable.Specification

class NameSpec extends Specification{
  "Name" should {
    "Return the input string if valid" in {
      Name("L").getName must_=== "L"
    }
  }

  "Suffix" should {
    "Return the input string if valid" in {
      Name.Suffix("0").getSuffix must_=== "0"
    }
  }

  object Compile {
    val good = Name("asdf_basrr")
    // val badNumberFirst = Name("0asdfa")
    val goodIfNumberSecond = Name("a0asdfa")
    // val badAscii = Name("^")

    val goodSuffix = Name.Suffix("0asdfa")
    // val failAscci = Name.Suffix("^")
  }
}