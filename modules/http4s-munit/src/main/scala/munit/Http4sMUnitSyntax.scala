/*
 * Copyright 2020-2026 Alejandro Hernández <https://github.com/alejandrohdezma>
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

import scala.util.control.NoStackTrace

import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.IO
import cats.effect.Resource
import cats.effect.SyncIO
import cats.syntax.all._

import io.circe.Json
import org.http4s.AuthedRequest
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
import org.typelevel.vault.Key

trait Http4sMUnitSyntax extends Http4sDsl[IO] with Http4sClientDsl[IO] with AllSyntax { self: CatsEffectSuite =>

  implicit class Http4sMUnitClientTypeOps(t: Client.type) {

    /** A `Client` instance that always fails */
    def fail: Client[IO] = Client[IO](request => Assertions.fail("This should not be called", clues(request)))

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
      Client.fromHttpApp(HttpApp[IO](r => pf.lift(r).getOrElse(Assertions.fail("This should not be called", clues(r)))))

    /** Creates an MUnit fixture that initializes some class that depends on an http4s `Client` for each test.
      *
      * @example
      *   {{{
      * val fixture = Client.partialFixture(client => Resource.pure(PingService.create(client)))
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

  /** Asserts that the obtained JSON equals `expected`, where a string in `expected` that is the marker of one of the
    * `placeholders` in scope stands for any value that placeholder accepts.
    *
    * On failure the diff shows the obtained document with every value that satisfied its placeholder replaced by the
    * marker, so the values that did not match are shown as they were received.
    *
    * {{{
    * val expected = json"""{ "id": "<uuid>", "name": "Jose", "created_at": "<recent timestamp>" }"""
    *
    * assertIOJson(response.as[Json], expected)
    * }}}
    */
  def assertIOJson(obtained: IO[Json], expected: Json)(implicit placeholders: Placeholders, loc: Location): IO[Unit] =
    assertIO(obtained.map(_.withPlaceholders(expected)), expected)

  implicit final class Http4sMUnitJsonOps(json: Json) {

    /** `json` with every value that satisfies the placeholder at the same position in `expected` replaced by that
      * placeholder's marker.
      */
    def withPlaceholders(expected: Json)(implicit placeholders: Placeholders): Json =
      if (placeholders.find(expected).exists(_.matches(json)))
        expected
      else if (json.isObject && expected.isObject)
        Json.fromFields(json.asObject.get.toList.map { case (key, value) =>
          key -> expected.asObject.get(key).fold(value)(value.withPlaceholders)
        })
      else if (json.isArray && expected.isArray)
        Json.fromValues {
          json.asArray.get.zip(expected.asArray.get).map { case (value, expected) => value.withPlaceholders(expected) }
        }
      else
        json

  }

  implicit final class Http4sMunitCIStringOps(ci: CIString) {

    /** Creates a `Header.Raw` value from a case-insensitive string. */
    def :=(value: String): Header.Raw = Header.Raw(ci, value)

  }

  /** Alias for `http://localhost` */
  def localhost = uri"http://localhost"

  implicit class Http4sMUnitAuthedRequestTypeOps(t: AuthedRequest.type) {

    /** Provides a Kleisli for obtaining an `AuthedRequest` that can be combined with an `AuthedRoutes` instance easily.
      *
      * @example
      *   {{{
      *   val myAuthedRoutes: AuthedRoutes[IO, A] = ???
      *
      *   AuthedRequest.fromContext[A].andThen(myAuthedRoutes).orFail.asClient
      *   }}}
      */
    def fromContext[A: Key]: Kleisli[OptionT[IO, *], Request[IO], AuthedRequest[IO, A]] =
      AuthedRequest((_: Request[IO]).getContext[A])
        .mapK(OptionT.liftK)

  }

  case class ContextNotFound(request: Request[IO])
      extends RuntimeException(s"Auth context not found on request $request, remember to add one with `.context()`")
      with NoStackTrace

  implicit class Http4sMUnitRequestOps(request: Request[IO]) {

    /** Adds a request context as an attribute using an implicit key. */
    def context[A](context: A)(implicit key: Key[A]): Request[IO] = request.withAttribute(key, context)

    /** Retrieves the context stored as an attribute using an implicit key..
      *
      * You can use `Request#context` to set the context attribute.
      */
    def getContext[A](implicit key: Key[A]): IO[A] = request.attributes.lookup(key).liftTo[IO](ContextNotFound(request))

  }

  implicit final class Http4sMUnitKleisliResponseOps[A](kleisli: Kleisli[OptionT[IO, *], A, Response[IO]]) {

    def orFail: Kleisli[IO, A, Response[IO]] =
      Kleisli(a => kleisli.run(a).getOrElse(Assertions.fail("This should not be called", clues(a))))

  }

  implicit final class Http4sMUnitHttpAppOps(httpApp: HttpApp[IO]) {

    /** Transforms this app into an http4s `Client` resource. */
    def asClient: Resource[IO, Client[IO]] =
      Client.fromHttpApp(httpApp).pure[Resource[IO, *]]

    /** Transforms the provided app into an http4s' `Client` fixture. */
    @deprecated("Use `asClient` together with `http4sMUnitClientResource`", "3.0.0")
    def asFixture: SyncIO[FunFixture[Client[IO]]] = ResourceFunFixture(asClient)

  }

  implicit class Http4sMUnitClientOps(client: Client[IO]) {

    /** Prepends the provided `Uri` to every request made by this client. */
    def withBaseUri(uri: Uri): Client[IO] = withUpdatedUri(uri.resolve)

    /** Applies a method that updates the requests's `Uri` on every request. */
    def withUpdatedUri(f: Uri => Uri): Client[IO] = Client(request => client.run(request.withUri(f(request.uri))))

    /** Transforms the provided client into a `FunFixture`. */
    @deprecated("Override `http4sMUnitClientResource` instead", "3.0.0")
    def asFixture: SyncIO[FunFixture[Client[IO]]] = ResourceFunFixture(client.pure[Resource[IO, *]])

  }

  implicit class Http4sMUnitUriOps(uri: Uri) {

    /** Allows changing the URIs port */
    def withPort(port: Int): Uri = {
      val authority = uri.authority.fold(Uri.Authority(port = Some(port)))(_.copy(port = Some(port)))
      uri.copy(authority = Some(authority))
    }

  }

}
