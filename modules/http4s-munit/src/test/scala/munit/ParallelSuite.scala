package munit

import java.util.concurrent.atomic.AtomicInteger

import cats.effect.IO

import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.syntax.all._

class ParallelSuite extends Http4sHttpRoutesSuite {

  override val routes: HttpRoutes[IO] = HttpRoutes.pure(Response().withEntity("hello!"))

  // `repeat`/`parallel`

  {
    val execution: AtomicInteger = new AtomicInteger(0)

    test(GET(uri"it-does-not-mater"))
      .repeat(1000)
      .parallel(10) { response =>
        execution.incrementAndGet()

        assertIO(response.as[String], "hello!")
      }

    test("all individual tests in a stress test must be run") {
      assertEquals(execution.get(), 1000)
    }

  }

  // `doNotRepeat`

  {
    val executions: AtomicInteger = new AtomicInteger(0)

    test(GET(uri"it-does-not-mater"))
      .alias("under no circumstance this test should be repeated")
      .repeat(1000)
      .parallel(10)
      .doNotRepeat { response =>
        executions.incrementAndGet()

        assertIO(response.as[String], "hello!")
      }

    test("tests using `doNotRepeat` flag must be run just once") {
      assertEquals(executions.get(), 1)
    }

  }

}
