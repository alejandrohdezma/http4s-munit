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

import cats.Show
import cats.effect.IO
import cats.effect.Resource
import cats.effect.SyncIO
import cats.syntax.all._

import org.http4s.AuthedRequest
import org.http4s.AuthedRoutes
import org.http4s.Request
import org.http4s.Response

/** Base class for suites testing `AuthedRoutes`.
  *
  * To use this class you'll need to provide the routes being tested by overriding `routes`.
  *
  * Ensure that a `Show` instance for the request's context type is in scope. This instance will be used to include the
  * context's information in the test's name.
  *
  * @example
  *   {{{
  * import cats.effect.IO
  *
  * import org.http4s.AuthedRoutes
  *
  * class MyAuthedRoutesSuite extends munit.Http4sAuthedRoutesSuite[String] {
  *
  *   override val routes: AuthedRoutes[String, IO] = AuthedRoutes.of {
  *     case GET -> Root / "hello" as user => Ok(user + " says Hi")
  *   }
  *
  *   test(GET(uri"hello").as("Jose")) { response =>
  *     assertIO(response.as[String], "Jose says Hi")
  *   }
  *
  * }
  *   }}}
  */
abstract class Http4sAuthedRoutesSuite[A: Show] extends Http4sSuite {

  /** The HTTP routes being tested */
  val routes: AuthedRoutes[A, IO]

  implicit class Request2AuthedRequest(request: Request[IO]) {

    /** Alias for adding a request's context. */
    def context(context: A): Request[IO] = request.withAttribute(RequestContext.key, RequestContext(context))

    /** Alias for adding a request's context. */
    @deprecated("Use `.context` instead", since = "0.16.0")
    def ->(a: A): Request[IO] = context(a)

  }

  implicit class Http4sMUnitTestCreatorOps(creator: Http4sMUnitTestCreator) {

    /** Allows overriding the routes used when running this test. */
    def withRoutes(newRoutes: AuthedRoutes[A, IO]): Http4sMUnitTestCreator = creator.copy(
      http4sMUnitFunFixture = ResourceFunFixture(
        ((request: Request[IO]) => newRoutes.orNotFound.run(AuthedRequest(request.getContext, request)).toResource)
          .pure[Resource[IO, *]]
      )
    )

  }

  implicit class RequestContextOps(request: Request[IO]) {

    def getContext: A = request.attributes
      .lookup(RequestContext.key)
      .getOrElse(fail("Auth context not found on request, remember to add one with `.context`", clues(request)))
      .as[A]

  }

  implicit class AuthedRoutesCompanionOps(companion: AuthedRoutes.type) {

    /** An AuthedRoutes instance that always fails */
    val fail: AuthedRoutes[A, IO] =
      AuthedRoutes(request => Assertions.fail("This should not be called", clues(request)))

  }

  /** @inheritdoc */
  override def http4sMUnitFunFixture: SyncIO[FunFixture[Request[IO] => Resource[IO, Response[IO]]]] =
    ResourceFunFixture(
      ((request: Request[IO]) => routes.orNotFound.run(AuthedRequest(request.getContext, request)).toResource)
        .pure[Resource[IO, *]]
    )

  /** Declares a test for the provided request. That request will be executed using the routes provided in `routes`.
    *
    * @example
    *   {{{
    * test(GET(uri"users" / 42).context("user-1")) { response =>
    *     // test body
    * }
    *   }}}
    *
    * @example
    *   {{{
    * test(POST(json, uri"users") -> "user-2").alias("Create a new user") { response =>
    *     // test body
    * }
    *   }}}
    *
    * @example
    *   {{{
    * test(GET(uri"users" / 42).context("user-3")).flaky { response =>
    *     // test body
    * }
    *   }}}
    */
  def test(request: Request[IO]): Http4sMUnitTestCreator = {
    if (!request.attributes.contains(RequestContext.key))
      fail("Auth context not found on request, remember to add one with `.context`", clues(request))

    Http4sMUnitTestCreator(request, http4sMUnitFunFixture)
  }

}
