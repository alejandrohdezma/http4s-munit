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

import cats.effect.IO
import cats.effect.Resource
import cats.effect.SyncIO

import org.http4s._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDslBinCompat

/** Base class for suites testing classes that need a `Client` instance.
  *
  * It adds two extension methods to the `Client` companion object: `from` and `fixture`.
  *
  * See method docs for more information on how to use them.
  *
  * It also brings the http-dsl into scope, so you don't need to import it yourself.
  *
  * @example
  *   {{{
  * import cats.effect._
  * import org.http4s.client.Client
  *
  * class PingService[F[_]: Async](client: Client[F]) {
  *
  *   def ping(): F[String] = client.expect[String]("ping")
  *
  * }
  *
  * class PingServiceSuite extends munit.ClientSuite {
  *
  *   val fixture = Client.fixture(client => Resource.pure(new PingService[IO](client)))
  *
  *   fixture {
  *     case GET -> Root / "ping" => Ok("pong")
  *   }.test("PingService.ping works") { service =>
  *     val result = service.ping()
  *
  *     assertIO(result, "pong")
  *   }
  *
  * }
  *   }}}
  *
  * @author
  *   Alejandro Hernández
  */
trait ClientSuite extends CatsEffectSuite with Http4sDslBinCompat[IO] {

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
    def fixture[A](
        f: Client[IO] => Resource[IO, A]
    ): PartialFunction[Request[IO], IO[Response[IO]]] => SyncIO[FunFixture[A]] =
      pf => ResourceFunFixture(f(from(pf)))

  }

}
