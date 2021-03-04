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

import org.http4s.AuthedRequest
import org.http4s.AuthedRoutes
import org.http4s.ContextRequest
import org.http4s.Request
import org.http4s.Response
import org.http4s.syntax.all._

/**
 * Base class for suites testing `AuthedRoutes`.
 *
 * @author Alejandro Hernández
 * @author José Gutiérrez
 */
abstract class Http4sAuthedRoutesSuite[A: Show] extends Http4sBaseSuite[A] {

  /** The HTTP routes being tested */
  val routes: AuthedRoutes[A, IO]

  implicit class Request2AuthedRequest(request: IO[Request[IO]]) {

    /**
     * Converts an `IO[Request[IO]]` into an `IO[AuthedRequest[IO, A]]`
     * by providing the `A` context.
     */
    def context(context: A): IO[AuthedRequest[IO, A]] = request.map(AuthedRequest(context, _))

    /**
     * Converts an `IO[Request[IO]]` into an `IO[AuthedRequest[IO, A]]`
     * by providing the `A` context.
     */
    def ->(a: A): IO[AuthedRequest[IO, A]] = context(a)

  }

  implicit class TestCreatorOps(private val testCreator: TestCreator) {

    def apply(body: Response[IO] => Any)(implicit loc: munit.Location): Unit = testCreator.execute(test, body) {
      Resource.liftF(routes.orNotFound.run(testCreator.request))
    }

  }

  /**
   * Declares a test for the provided request.
   *
   * @example
   * {{{
   * test(GET(uri"users" / 42).as("user-1")) { response =>
   *    // test body
   * }
   * }}}
   */
  def test(request: IO[ContextRequest[IO, A]]): TestCreator =
    TestCreator(request.unsafeRunSync(), TestOptions(""), None, None)

}
