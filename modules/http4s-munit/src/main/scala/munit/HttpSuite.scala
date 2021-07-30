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
  * To use this class you'll need to provide the `Uri` of the remote container by
  * overriding `baseUri`.
  *
  * @example
  * {{{
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
  * import org.http4s.client.blaze.BlazeClientBuilder
  * import org.http4s.client.dsl.io._
  * import org.http4s.syntax.all._
  *
  * class HttpSuiteSuite extends munit.HttpSuite {
  *
  *  override def http4sMUnitClient: Resource[IO, Client[IO]] = BlazeClientBuilder[IO](global).resource
  *
  *  override def baseUri(): Uri = uri"https://api.github.com"
  *
  *  test(GET(uri"users/gutiory")) { response =>
  *    assertEquals(response.status.code, 200)
  *
  *    val result = response.as[Json].map(_.hcursor.get[String]("login"))
  *
  *    assertIO(result, Right("gutiory"))
  *  }
  *
  * }
  * }}}
  *
  * @author Alejandro Hernández
  * @author José Gutiérrez
  */
abstract class HttpSuite extends Http4sSuite[Unit] with CatsEffectFunFixtures {

  /** The base URI for all tests. This URI will prepend the one used in each
    * test's request.
    */
  def baseUri(): Uri

  /** This client is used under the hood to execute the requests.
    *
    * Users need to provide a value for it using one of the available clients.
    */
  def http4sMUnitClient: Resource[IO, Client[IO]]

  override def http4sMUnitFunFixture: SyncIO[FunFixture[ContextRequest[IO, Unit] => Resource[IO, Response[IO]]]] =
    ResourceFixture(http4sMUnitClient.map(client => req => client.run(req.req.withUri(baseUri().resolve(req.req.uri)))))

  implicit class Http4sMUnitTestCreatorOps(private val testCreator: Http4sMUnitTestCreator) {

    /** Provide a new request created from the response of the previous request. The
      * alias entered as parameter will be used to construct the test's name.
      *
      * If this is the last `andThen` call, the response provided to the test will be
      * the one obtained from executing this request
      */
    def andThen(alias: String)(f: Response[IO] => IO[Request[IO]]): Http4sMUnitTestCreator =
      testCreator.copy(followingRequests =
        testCreator.followingRequests :+ ((alias, f.andThen(_.map(ContextRequest((), _)))))
      )

    /** Provide a new request created from the response of the previous request.
      *
      * If this is the last `andThen` call, the response provided to the test will be
      * the one obtained from executing this request
      */
    def andThen(f: Response[IO] => IO[Request[IO]]): Http4sMUnitTestCreator = andThen("")(f)

  }

  /** Declares a test for the provided request. That request will be executed using the
    * provided client in `httpClient` to the server indicated in `baseUri`.
    *
    * @example
    * {{{
    * test(GET(uri"users" / 42)) { response =>
    *    // test body
    * }
    * }}}
    *
    * @example
    * {{{
    * test(POST(json, uri"users")).alias("Create a new user") { response =>
    *    // test body
    * }
    * }}}
    *
    * @example
    * {{{
    * test(GET(uri"users" / 42)).flaky { response =>
    *    // test body
    * }
    * }}}
    */
  def test(request: IO[Request[IO]]) = Http4sMUnitTestCreator(ContextRequest((), request.unsafeRunSync()))

}
