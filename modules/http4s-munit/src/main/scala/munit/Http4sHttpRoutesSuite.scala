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
import cats.effect.Resource
import cats.effect.SyncIO

import org.http4s.ContextRequest
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.Response
import org.http4s.syntax.all._

/** Base class for suites testing `HttpRoutes`.
  *
  * To use this class you'll need to provide the routes being tested by overriding `routes`.
  *
  * @example
  *   {{{
  * import cats.effect.IO
  *
  * import org.http4s.HttpRoutes
  * import org.http4s.client.dsl.io._
  * import org.http4s.dsl.io._
  * import org.http4s.syntax.all._
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
abstract class Http4sHttpRoutesSuite extends Http4sSuite[Request[IO]] {

  /** The HTTP routes being tested */
  val routes: HttpRoutes[IO]

  /** @inheritdoc */
  override def http4sMUnitNameCreator(
      request: Request[IO],
      followingRequests: List[String],
      testOptions: TestOptions,
      config: Http4sMUnitConfig
  ): String =
    Http4sMUnitDefaults.http4sMUnitNameCreator(ContextRequest((), request), followingRequests, testOptions, config)

  override def http4sMUnitFunFixture: SyncIO[FunFixture[Request[IO] => Resource[IO, Response[IO]]]] =
    SyncIO.pure(FunFixture(_ => req => routes.orNotFound.run(req).to[Resource[IO, *]], _ => ()))

  /** Declares a test for the provided request. That request will be executed using the routes provided in `routes`.
    *
    * @example
    *   {{{
    * test(GET(uri"users" / 42)) { response =>
    *     // test body
    * }
    *   }}}
    *
    * @example
    *   {{{
    * test(POST(json, uri"users")).alias("Create a new user") { response =>
    *     // test body
    * }
    *   }}}
    *
    * @example
    *   {{{
    * test(GET(uri"users" / 42)).flaky { response =>
    *     // test body
    * }
    *   }}}
    */
  def test(request: Request[IO]): Http4sMUnitTestCreator = Http4sMUnitTestCreator(request)

}
