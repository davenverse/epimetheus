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
    val errors1 = compileErrors("""Name("0asdfa")""")
    assert(errors1.nonEmpty)

    val goodIfNumberSecond = Name("a0asdfa")
    val errors2 = compileErrors("""Name("^")""")
    assert(errors2.nonEmpty)

    val goodSuffix = Name.Suffix("0asdfa")
    val errors3 = compileErrors("""Name.Suffix("^")""")
    assert(errors3.nonEmpty)

    assert(compileErrors("""Name("metric_total")""").nonEmpty)
    assert(compileErrors("""Name("metric_created")""").nonEmpty)
    assert(compileErrors("""Name("metric_bucket")""").nonEmpty)
    assert(compileErrors("""Name("metric_info")""").nonEmpty)
    assert(compileErrors("""Name("metric.total")""").nonEmpty)
    assert(compileErrors("""Name("metric.created")""").nonEmpty)
    assert(compileErrors("""Name("metric.bucket")""").nonEmpty)
    assert(compileErrors("""Name("metric.info")""").nonEmpty)
  }
}
