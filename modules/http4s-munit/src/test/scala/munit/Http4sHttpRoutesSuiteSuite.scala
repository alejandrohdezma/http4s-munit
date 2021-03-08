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

import cats.effect.IO

import org.http4s.ContextRequest
import org.http4s.HttpRoutes
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.syntax.all._

class Http4sHttpRoutesSuiteSuite extends Http4sHttpRoutesSuite {

  override val routes: HttpRoutes[IO] = HttpRoutes.of {
    case GET -> Root / "hello"        => Ok("Hi")
    case GET -> Root / "hello" / name => Ok(s"Hi $name")
  }

  override def http4sMUnitNameCreator(request: ContextRequest[IO, Unit], testOptions: TestOptions): String =
    "Test - " + super.http4sMUnitNameCreator(request, testOptions)

  test(GET(uri"hello")).alias("Test 1") { response =>
    assertIO(response.as[String], "Hi")
  }

  test(GET(uri"hello" / "Jose")).alias("Test 2") { response =>
    assertIO(response.as[String], "Hi Jose")
  }

}
