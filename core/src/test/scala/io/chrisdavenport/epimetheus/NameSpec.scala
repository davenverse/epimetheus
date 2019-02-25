package io.chrisdavenport.epimetheus

import org.specs2.mutable.Specification

class NameSpec extends Specification{
  "Name" should {
    "Return the input string if valid" in {
      Name("L").getName must_=== "L"
    }
  }
}