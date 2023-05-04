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

import org.http4s.Header
import org.http4s.HttpApp
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.syntax.AllSyntax
import org.typelevel.ci.CIString

trait Http4sMUnitSyntax extends Http4sDsl[IO] with Http4sClientDsl[IO] with AllSyntax { self: CatsEffectSuite =>

  implicit class ClientTypeOps(t: Client.type) {

    /** Creates an http4s `Client` from a partial function representing routes (like those created with
      * `HttpRoutes.of`).
      *
      * @example
      *   {{{
      * Client.from {
      *     case GET -> Root / "ping" / id => Ok("pong")
      * }
      *   }}}
      */
    def from(pf: PartialFunction[Request[IO], IO[Response[IO]]]): Client[IO] =
      Client.fromHttpApp(HttpApp[IO](r => pf.lift(r).getOrElse(fail("This should not be called", clues(r)))))

    /** Creates an MUnit fixture that initializes some class that depends on an http4s `Client` for each test.
      *
      * @example
      *   {{{
      * val fixture = Client.fixture(PingService.create[F](_))
      *
      * fixture {
      *     case GET -> Root / "ping" => Ok("pong")
      * }.test("testing my service") { service =>
      *     ...
      * }
      *   }}}
      */
    def partialFixture[A](
        f: Client[IO] => Resource[IO, A]
    ): PartialFunction[Request[IO], IO[Response[IO]]] => SyncIO[FunFixture[A]] =
      pf => ResourceFunFixture(f(from(pf)))

  }

  implicit final class CiStringHeaderOps(ci: CIString) {

    /** Creates a `Header.Raw` value from a case-insensitive string. */
    def :=(value: String): Header.Raw = Header.Raw(ci, value)

  }

  /** Alias for `http://localhost` */
  def localhost = uri"http://localhost"

  implicit class RequestContextOps(request: Request[IO]) {

    /** Adds a request context as an attribute using [[RequestContext.key]]. */
    def context[A: Show](context: A): Request[IO] = request.withAttribute(RequestContext.key, RequestContext(context))

    /** Retrieves the context stored as an attribute using [[RequestContext.key]].
      *
      * You can use `Request#context` to set the context attribute.
      */
    def getContext[A]: A = request.attributes
      .lookup(RequestContext.key)
      .getOrElse(fail("Auth context not found on request, remember to add one with `.context`", clues(request)))
      .as[A]

  }

  implicit class ClientWithBaseUriOps(client: Client[IO]) {

    /** Prepends the provided `Uri` to every request made by this client. */
    def withBaseUri(uri: Uri): Client[IO] = client.withUpdatedUri(uri.resolve)

    /** Applies a method that updates the requests's `Uri` on every request. */
    def withUpdatedUri(f: Uri => Uri): Client[IO] = Client(request => client.run(request.withUri(f(request.uri))))

  }

  implicit class UriWithPort(uri: Uri) {

    /** Allows changing the URIs port */
    def withPort(port: Int): Uri = {
      val authority = uri.authority.fold(Uri.Authority(port = Some(port)))(_.copy(port = Some(port)))
      uri.copy(authority = Some(authority))
    }

  }

}
