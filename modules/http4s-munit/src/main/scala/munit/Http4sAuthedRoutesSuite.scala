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

import cats.Show
import cats.effect.IO
import cats.effect.Resource
import cats.effect.SyncIO

import org.http4s.AuthedRequest
import org.http4s.AuthedRoutes
import org.http4s.ContextRequest
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
abstract class Http4sAuthedRoutesSuite[A: Show] extends Http4sSuite[AuthedRequest[IO, A]] {

  /** The HTTP routes being tested */
  val routes: AuthedRoutes[A, IO]

  /** @inheritdoc */
  override def http4sMUnitNameCreator(
      request: AuthedRequest[IO, A],
      followingRequests: List[String],
      testOptions: TestOptions,
      config: Http4sMUnitConfig
  ): String = Http4sMUnitDefaults.http4sMUnitNameCreator(
    request,
    followingRequests,
    testOptions,
    config,
    http4sMUnitNameCreatorReplacements()
  )

  implicit class Request2AuthedRequest(request: Request[IO]) {

    /** Converts an `IO[Request[IO]]` into an `IO[AuthedRequest[IO, A]]` by providing the `A` context. */
    def context(context: A): AuthedRequest[IO, A] = AuthedRequest(context, request)

    /** Converts an `IO[Request[IO]]` into an `IO[AuthedRequest[IO, A]]` by providing the `A` context. */
    def ->(a: A): AuthedRequest[IO, A] = context(a)

  }

  implicit class Http4sMUnitTestCreatorOps(creator: Http4sMUnitTestCreator) {

    /** Allows overriding the routes used when running this test. */
    def withRoutes(newRoutes: AuthedRoutes[A, IO]): Http4sMUnitTestCreator = creator.copy(
      http4sMUnitFunFixture =
        SyncIO.pure(FunFixture(_ => req => newRoutes.orNotFound.run(req).to[Resource[IO, *]], _ => ()))
    )

  }

  implicit class AuthedRoutesCompanionOps(companion: AuthedRoutes.type) {

    /** An AuthedRoutes instance that always fails */
    val fail: AuthedRoutes[A, IO] =
      AuthedRoutes(request => Assertions.fail("This should not be called", clues(request)))

  }

  def http4sMUnitFunFixture: SyncIO[FunFixture[ContextRequest[IO, A] => Resource[IO, Response[IO]]]] =
    SyncIO.pure(FunFixture(_ => routes.orNotFound.run(_).to[Resource[IO, *]], _ => ()))

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
  def test(request: AuthedRequest[IO, A]): Http4sMUnitTestCreator =
    Http4sMUnitTestCreator(request, http4sMUnitFunFixture)

}
