/*
 * Copyright 2020-2022 Alejandro Hernández <https://github.com/alejandrohdezma>
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
import cats.effect.SyncIO

import org.http4s.HttpRoutes
import org.http4s.client.Client

/** Base class for suites testing `HttpRoutes`.
  *
  * To use this class you'll need to provide the routes being tested by overriding `routes`.
  *
  * @example
  *   {{{
  * import cats.effect.IO
  *
  * import org.http4s.HttpRoutes
  *
  * class MyHttpRoutesSuite extends munit.Http4sHttpRoutesSuite[String] {
  *
  *   override val routes: HttpRoutes[IO] = HttpRoutes.of {
  *     case GET -> Root / "hello" => Ok("Hello!")
  *   }
  *
  *   test(GET(uri"hello")) { response =>
  *     assertIO(response.as[String], "Hello!")
  *   }
  *
  * }
  *   }}}
  *
  * @author
  *   Alejandro Hernández
  * @author
  *   José Gutiérrez
  */
@deprecated("Use `Http4sSuite` overriding `http4sMUnitClientFixture` instead", since = "0.16.0")
trait Http4sHttpRoutesSuite extends Http4sSuite {

  /** The HTTP routes being tested.
    *
    * If running every test with `withRoutes` you can set this value to:
    *
    * ```scala
    * override val routes = HttpRoutes.fail
    * ```
    */
  val routes: HttpRoutes[IO]

  implicit class HttpRoutesCompanionOps(companion: HttpRoutes.type) {

    /** An HttpRoutes instance that always fails */
    val fail: HttpRoutes[IO] = HttpRoutes(request => Assertions.fail("This should not be called", clues(request)))

  }

  /** @inheritdoc */
  override def http4sMUnitClientFixture: SyncIO[FunFixture[Client[IO]]] = routes.orFail.asFixture

}
