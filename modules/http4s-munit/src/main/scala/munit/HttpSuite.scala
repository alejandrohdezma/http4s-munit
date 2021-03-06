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
import cats.effect.SyncIO

import org.http4s.ContextRequest
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.asynchttpclient.AsyncHttpClient

/**
 * Base class for suites testing remote HTTP servers.
 *
 * To use this class you'll need to provide the `Uri` of the remote container by
 * overriding `baseUri`.
 *
 * @example
 * {{{
 * import io.circe.Json
 *
 * import org.http4s.Method.GET
 * import org.http4s.Uri
 * import org.http4s.circe._
 * import org.http4s.client.dsl.io._
 * import org.http4s.syntax.all._
 *
 * class HttpSuiteSuite extends munit.HttpSuite {
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

  /**
   * The base URI for all tests. This URI will prepend the one used in each
   * test's request.
   */
  def baseUri(): Uri

  /**
   * This client is used under the hood to execute the requests.
   *
   * Override it if you want to use a different implementation or if you
   * want to initalize in any way out of the default one (different timeouts,
   * SSL certificates...).
   */
  def httpClient: SyncIO[FunFixture[Client[IO]]] = ResourceFixture(AsyncHttpClient.resource[IO]())

  implicit class TestCreatorOps(private val testCreator: TestCreator) {

    def apply(body: Response[IO] => Any)(implicit loc: munit.Location): Unit =
      testCreator.execute[Client[IO]](a => b => httpClient.test(a)(b)(loc), body) { client: Client[IO] =>
        val uri = Uri.resolve(baseUri(), testCreator.request.req.uri)

        client.run(testCreator.request.req.withUri(uri))
      }

  }

  /**
   * Declares a test for the provided request. That request will be executed using the
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
  def test(request: IO[Request[IO]]) = TestCreator(ContextRequest((), request.unsafeRunSync()))

}
