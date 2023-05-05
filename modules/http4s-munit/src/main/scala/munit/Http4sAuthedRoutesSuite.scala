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
import cats.effect.SyncIO

import org.http4s.AuthedRoutes
import org.http4s.Request
import org.http4s.client.Client
import org.typelevel.vault.Key

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
@deprecated("Use `Http4sSuite` overriding `http4sMUnitClientFixture` instead", since = "0.16.0")
abstract class Http4sAuthedRoutesSuite[A: Show] extends Http4sSuite {

  @SuppressWarnings(Array("scalafix:DisableSyntax.valInAbstract"))
  implicit val key: Key[A] = Key.newKey[IO, A].unsafeRunSync()

  /** The HTTP routes being tested */
  val routes: AuthedRoutes[A, IO]

  implicit class Request2AuthedRequest(request: Request[IO]) {

    /** Alias for adding a request's context. */
    @deprecated("Use `.context` instead", since = "0.16.0")
    def ->(a: A): Request[IO] = request.context(a)

  }

  implicit class AuthedRoutesCompanionOps(companion: AuthedRoutes.type) {

    /** An AuthedRoutes instance that always fails */
    val fail: AuthedRoutes[A, IO] =
      AuthedRoutes(request => Assertions.fail("This should not be called", clues(request)))

  }

  /** @inheritdoc */
  override def http4sMUnitClientFixture: SyncIO[FunFixture[Client[IO]]] = routes.asFixture

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
  override def test(request: Request[IO]): Http4sMUnitTestCreator = {
    if (!request.attributes.contains(key))
      fail("Auth context not found on request, remember to add one with `.context`", clues(request))

    super.test(request)
  }

}
