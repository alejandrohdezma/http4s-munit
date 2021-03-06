/*
 * Copyright 2020-2021 Alejandro Hernández <https://github.com/alejandrohdezma>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
