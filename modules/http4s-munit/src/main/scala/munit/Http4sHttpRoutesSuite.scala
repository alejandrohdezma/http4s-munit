/*
 * Copyright 2020-2023 Alejandro Hernández <https://github.com/alejandrohdezma>
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
import cats.effect.Resource
import cats.effect.SyncIO
import cats.syntax.all._

import org.http4s.HttpRoutes
import org.http4s.Request
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

  implicit class Http4sMUnitTestCreatorOps(creator: Http4sMUnitTestCreator) {

    /** Allows overriding the routes used when running this test. */
    def withRoutes(newRoutes: HttpRoutes[IO]): Http4sMUnitTestCreator = creator.copy(
      http4sMUnitFunFixture = ResourceFunFixture {
        ((request: Request[IO]) => newRoutes.orNotFound.run(request).toResource).pure[Resource[IO, *]]
      }
    )

  }

  implicit class HttpRoutesCompanionOps(companion: HttpRoutes.type) {

    /** An HttpRoutes instance that always fails */
    val fail: HttpRoutes[IO] = HttpRoutes(request => Assertions.fail("This should not be called", clues(request)))

  }

  /** @inheritdoc */
  override def http4sMUnitClientFixture: SyncIO[FunFixture[Client[IO]]] =
    ResourceFunFixture(Client[IO](request => routes.orNotFound.run(request).toResource).pure[Resource[IO, *]])

}
