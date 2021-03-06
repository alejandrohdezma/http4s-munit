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

import cats.Show
import cats.effect.IO
import cats.effect.Resource
import cats.effect.SyncIO

import org.http4s.AuthedRequest
import org.http4s.AuthedRoutes
import org.http4s.ContextRequest
import org.http4s.Request
import org.http4s.Response
import org.http4s.syntax.all._

/** Base class for suites testing `AuthedRoutes`.
  *
  * To use this class you'll need to provide the routes being tested by
  * overriding `routes`.
  *
  * Ensure that a `Show` instance for the request's context type is in
  * scope. This instance will be used to include the context's information
  * in the test's name.
  *
  * @example
  * {{{
  * import cats.effect.IO
  *
  * import org.http4s.AuthedRoutes
  * import org.http4s.client.dsl.io._
  * import org.http4s.dsl.io._
  * import org.http4s.syntax.all._
  *
  * class MyAuthedRoutesSuite extends munit.Http4sAuthedRoutesSuite[String] {
  *
  *  override val routes: AuthedRoutes[String, IO] = AuthedRoutes.of {
  *    case GET -> Root / "hello" as user => Ok(user + " says Hi")
  *  }
  *
  *  test(GET(uri"hello").as("Jose")) { response =>
  *    assertIO(response.as[String], "Jose says Hi")
  *  }
  *
  * }
  * }}}
  */
abstract class Http4sAuthedRoutesSuite[A: Show] extends Http4sSuite[A] {

  /** The HTTP routes being tested */
  val routes: AuthedRoutes[A, IO]

  implicit class Request2AuthedRequest(request: IO[Request[IO]]) {

    /** Converts an `IO[Request[IO]]` into an `IO[AuthedRequest[IO, A]]`
      * by providing the `A` context.
      */
    def context(context: A): IO[AuthedRequest[IO, A]] = request.map(AuthedRequest(context, _))

    /** Converts an `IO[Request[IO]]` into an `IO[AuthedRequest[IO, A]]`
      * by providing the `A` context.
      */
    def ->(a: A): IO[AuthedRequest[IO, A]] = context(a)

  }

  override def http4sMUnitFunFixture: SyncIO[FunFixture[ContextRequest[IO, A] => Resource[IO, Response[IO]]]] =
    SyncIO.pure(FunFixture(_ => routes.orNotFound.run(_).to[Resource[IO, *]], _ => ()))

  implicit class Http4sMUnitTestCreatorOps(private val testCreator: Http4sMUnitTestCreator) {

    /** Provide a new request created from the response of the previous request. The
      * alias entered as parameter will be used to construct the test's name.
      *
      * If this is the last `andThen` call, the response provided to the test will be
      * the one obtained from executing this request
      */
    def andThen(alias: String)(f: Response[IO] => IO[ContextRequest[IO, A]]): Http4sMUnitTestCreator =
      testCreator.copy(followingRequests = testCreator.followingRequests :+ ((alias, f)))

    /** Provide a new request created from the response of the previous request.
      *
      * If this is the last `andThen` call, the response provided to the test will be
      * the one obtained from executing this request
      */
    def andThen(f: Response[IO] => IO[ContextRequest[IO, A]]): Http4sMUnitTestCreator = andThen("")(f)

  }

  /** Declares a test for the provided request. That request will be executed using
    * the routes provided in `routes`.
    *
    * @example
    * {{{
    * test(GET(uri"users" / 42).as("user-1")) { response =>
    *    // test body
    * }
    * }}}
    *
    * @example
    * {{{
    * test(POST(json, uri"users") -> "user-2").alias("Create a new user") { response =>
    *    // test body
    * }
    * }}}
    *
    * @example
    * {{{
    * test(GET(uri"users" / 42).as("user-3")).flaky { response =>
    *    // test body
    * }
    * }}}
    */
  def test(request: IO[ContextRequest[IO, A]]): Http4sMUnitTestCreator = Http4sMUnitTestCreator(request.unsafeRunSync())

}
