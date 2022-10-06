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
import cats.effect.Resource
import cats.effect.SyncIO

import org.http4s.ContextRequest
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.Response

/** Base class for suites testing per-test `HttpRoutes`.
  *
  * @example
  *   {{{
  * import cats.effect.IO
  *
  * import org.http4s.HttpRoutes
  *
  * class MyHttpRoutesSuite extends munit.Http4sTestHttpRoutesSuite {
  *
  *   test(routes = HttpRoutes.of { case GET -> Root / "hello" =>
  *     Ok("Hello!")
  *   }(GET(uri"hello")) { response =>
  *     assertIO(response.as[String], "Hello!")
  *   }
  *
  * }
  *   }}}
  * @author
  *   Jack Treble
  */
@deprecated("Use Http4sHttpRoutesSuite overriding routes per-test with `withRoutes` instead", since = "0.13.0")
trait Http4sTestHttpRoutesSuite extends Http4sSuite[Request[IO]] {

  /** @inheritdoc */
  override def http4sMUnitNameCreator(
      request: Request[IO],
      followingRequests: List[String],
      testOptions: TestOptions,
      config: Http4sMUnitConfig
  ): String =
    Http4sMUnitDefaults.http4sMUnitNameCreator(
      ContextRequest((), request),
      followingRequests,
      testOptions,
      config,
      http4sMUnitNameCreatorReplacements()
    )

  def http4sMUnitFunFixture(routes: HttpRoutes[IO]): SyncIO[FunFixture[Request[IO] => Resource[IO, Response[IO]]]] =
    SyncIO.pure(FunFixture(_ => req => routes.orNotFound.run(req).to[Resource[IO, *]], _ => ()))

  /** Declares a test for the provided routes and request.
    *
    * @example
    *   {{{
    * test(routes = HttpRoutes.of { case GET -> Root / "users" / number =>
    *   Ok(number)
    * }(GET(uri"users" / 42)) { response =>
    *     // test body
    * }
    *   }}}
    * @example
    *   {{{
    * test(routes = HttpRoutes.of { case req @ POST -> Root / "users" =>
    *   Ok(req.as[String])
    * }(POST(json, uri"users")).alias("Create a new user") { response =>
    *     // test body
    * }
    *   }}}
    * @example
    *   {{{
    * test(routes = HttpRoutes.of { case GET -> Root / "users" / number =>
    *   Ok(number)
    * }(GET(uri"users" / 42)).flaky { response =>
    *     // test body
    * }
    *   }}}
    */
  @deprecated("Use Http4sHttpRoutesSuite overriding routes per-test with `withRoutes` instead", since = "0.13.0")
  def test(routes: HttpRoutes[IO])(request: Request[IO]): Http4sMUnitTestCreator = {
    Http4sMUnitTestCreator(
      request,
      http4sMUnitFunFixture(routes)
    )
  }

}
