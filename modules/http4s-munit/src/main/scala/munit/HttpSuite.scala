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
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.http4s.client.Client

/** Base class for suites testing remote HTTP servers.
  *
  * To use this class you'll need to provide the `Uri` of the remote container by overriding `baseUri`.
  *
  * @example
  *   {{{
  * import scala.concurrent.ExecutionContext.global
  *
  * import cats.effect.IO
  * import cats.effect.Resource
  *
  * import io.circe.Json
  * import org.http4s.Method.GET
  * import org.http4s.Uri
  * import org.http4s.circe._
  * import org.http4s.client.Client
  * import org.http4s.ember.client.EmberClientBuilder
  * import org.http4s.client.dsl.io._
  * import org.http4s.syntax.all._
  *
  * class HttpSuiteSuite extends munit.HttpSuite {
  *
  *   override def http4sMUnitClient: Resource[IO, Client[IO]] = BlazeClientBuilder[IO](global).resource
  *
  *   override def baseUri(): Uri = uri"https://api.github.com"
  *
  *   test(GET(uri"users/gutiory")) { response =>
  *     assertEquals(response.status.code, 200)
  *
  *     val result = response.as[Json].map(_.hcursor.get[String]("login"))
  *
  *     assertIO(result, Right("gutiory"))
  *   }
  *
  * }
  *   }}}
  *
  * @author
  *   Alejandro Hernández
  * @author
  *   José Gutiérrez
  */
abstract class HttpSuite extends Http4sSuite[Request[IO]] with CatsEffectFunFixtures {

  /** The base URI for all tests. This URI will prepend the one used in each test's request. */
  def baseUri(): Uri

  /** @inheritdoc */
  override def http4sMUnitNameCreator(
      request: Request[IO],
      followingRequests: List[String],
      testOptions: TestOptions,
      config: Http4sMUnitConfig
  ): String =
    Http4sMUnitDefaults.http4sMUnitNameCreator(ContextRequest((), request), followingRequests, testOptions, config)

  /** This client is used under the hood to execute the requests.
    *
    * Users need to provide a value for it using one of the available clients.
    */
  def http4sMUnitClient: Resource[IO, Client[IO]]

  override def http4sMUnitFunFixture: SyncIO[FunFixture[Request[IO] => Resource[IO, Response[IO]]]] =
    ResourceFixture(http4sMUnitClient.map(client => req => client.run(req.withUri(baseUri().resolve(req.uri)))))

  /** Declares a test for the provided request. That request will be executed using the provided client in `httpClient`
    * to the server indicated in `baseUri`.
    *
    * @example
    *   {{{
    * test(GET(uri"users" / 42)) { response =>
    *     // test body
    * }
    *   }}}
    *
    * @example
    *   {{{
    * test(POST(json, uri"users")).alias("Create a new user") { response =>
    *     // test body
    * }
    *   }}}
    *
    * @example
    *   {{{
    * test(GET(uri"users" / 42)).flaky { response =>
    *     // test body
    * }
    *   }}}
    */
  def test(request: IO[Request[IO]]) = Http4sMUnitTestCreator(request.unsafeRunSync())

}
