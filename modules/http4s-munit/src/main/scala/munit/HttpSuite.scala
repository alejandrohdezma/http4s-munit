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
 * Base class for suites testing HTTP servers.
 *
 * @author Alejandro Hernández
 * @author José Gutiérrez
 */
abstract class HttpSuite extends Http4sSuite[Unit] with CatsEffectFunFixtures {

  /**
   * The base URI for all tests. This URI will prepend the one used in each
   * test's request.
   */
  val baseUri: Uri

  def httpClient: SyncIO[FunFixture[Client[IO]]] = ResourceFixture(AsyncHttpClient.resource[IO]())

  implicit class TestCreatorOps(private val testCreator: TestCreator) {

    def apply(body: Response[IO] => Any)(implicit loc: munit.Location): Unit =
      testCreator.execute[Client[IO]](a => b => httpClient.test(a)(b)(loc), body) { client: Client[IO] =>
        val uri = Uri.resolve(baseUri, testCreator.request.req.uri)

        client.run(testCreator.request.req.withUri(uri))
      }

  }

  /**
   * Declares a test for the provided request.
   *
   * @example
   * {{{
   * test(GET(uri"users" / 42)) { response =>
   *    // test body
   * }
   * }}}
   */
  def test(request: IO[Request[IO]]) = TestCreator(ContextRequest((), request.unsafeRunSync()))

}
