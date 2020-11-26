/*
 * Copyright 2020 Alejandro Hernández <https://github.com/alejandrohdezma>
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
import cats.syntax.show._

import org.http4s.AuthedRequest
import org.http4s.AuthedRoutes
import org.http4s.Request
import org.http4s.Uri

/**
 * Base class for suites testing `AuthedRoutes`.
 *
 * @author Alejandro Hernández
 * @author José Gutiérrez
 */
abstract class Http4sAuthedRoutesSuite[A: Show] extends Http4sSuite[AuthedRequest[IO, A], AuthedRoutes[A, IO]] {

  /**
   * Allows altering the name of the generated tests.
   *
   * By default it will generate test names like:
   *
   * {{{
   * test(GET(uri"users" / 42) -> "user1")               // GET -> users/42 as user1
   * test(GET(uri"users") -> "user2").alias("All users") // GET -> users (All users) as user2
   * }}}
   */
  override def munitHttp4sNameCreator(request: AuthedRequest[IO, A], testOptions: TestOptions): String = {
    val alias = if (testOptions.name.nonEmpty) s" (${testOptions.name})" else ""

    s"${request.req.method.name} -> ${Uri.decode(request.req.uri.renderString)}$alias as ${request.context.show}"
  }

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

}
