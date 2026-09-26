package io.chrisdavenport.epimetheus

import cats.effect._

class PrometheusRegistrySpec extends munit.CatsEffectSuite {

  test("write004: render a registered counter") {
    val test = for {
      pr <- PrometheusRegistry.build[IO]
      counter <- Counter.noLabels[IO](pr, Name("boo"), "Boo Counter")
      _ <- counter.inc
      out <- pr.write004
    } yield out

    test.map { out =>
      assert(out.contains("# TYPE boo_total counter"), out)
      assert(out.contains("boo_total 1.0"), out)
    }
  }

  test("writeOpenMetrics100: render a registered counter") {
    val test = for {
      pr <- PrometheusRegistry.build[IO]
      counter <- Counter.noLabels[IO](pr, Name("boo"), "Boo Counter")
      _ <- counter.inc
      out <- pr.writeOpenMetrics100
    } yield out

    test.map { out =>
      assert(out.contains("# TYPE boo counter"), out)
      assert(out.contains("boo_total 1.0"), out)
    }
  }

  test("write004: empty registry still renders") {
    PrometheusRegistry.build[IO].flatMap(_.write004).map(_ => ())
  }
}
