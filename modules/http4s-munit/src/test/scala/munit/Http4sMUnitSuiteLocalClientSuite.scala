/*
 * Copyright 2020-2026 Alejandro Hernández <https://github.com/alejandrohdezma>
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
import cats.effect.Resource

import org.http4s.HttpRoutes
import org.http4s.client.Client

class Http4sMUnitSuiteLocalClientSuite extends Http4sSuite {

  val allocations = new AtomicInteger(0)

  override def http4sMUnitClientResource =
    Resource.eval(IO(allocations.incrementAndGet())).flatMap { _ =>
      HttpRoutes.of[IO] { case GET -> Root / "ping" => Ok("pong") }.orFail.asClient
    }

  override lazy val http4sMUnitClientTestFixture: AnyFixture[Client[IO]] =
    ResourceSuiteLocalFixture("http4sMUnitClient", http4sMUnitClientResource)

  test(GET(uri"/ping")) { response =>
    assertEquals(allocations.get(), 1)
    assertIO(response.as[String], "pong")
  }

  test("the client is created once and shared across the whole suite") {
    assertEquals(allocations.get(), 1)
    assertIO(http4sMUnitClient.expect[String](GET(uri"/ping")), "pong")
  }

}
