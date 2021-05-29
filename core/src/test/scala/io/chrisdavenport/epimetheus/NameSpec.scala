package io.chrisdavenport.epimetheus

class NameSpec extends munit.CatsEffectSuite {

  test("Name: Return the input string if valid") {
    assertEquals(Name("L").getName, "L")
  }

  test("Name.Suffix: Return the input string if valid") {
    assertEquals(Name.Suffix("0").getSuffix, "0")
  }

  test("Name: compile-time check fails for invalid values") {
    val good = Name("asdf_basrr")
    assert(compileErrors("""Name("0asdfa")""").nonEmpty)
    val goodIfNumberSecond = Name("a0asdfa")
    assert(compileErrors("""Name("^")""").nonEmpty)

    val goodSuffix = Name.Suffix("0asdfa")
    assert(compileErrors("""Name.Suffix("^")""").nonEmpty)
  }
}
