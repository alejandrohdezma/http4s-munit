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

import com.dimafeng.testcontainers.munit.TestContainerForAll
import org.http4s.ContextRequest
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.asynchttpclient.AsyncHttpClient

/**
 * Base class for suites testing HTTP servers running in testcontainers.
 *
 * The container must expose an HTTP server in the 8080 port.
 *
 * @author Alejandro Hernández
 * @author José Gutiérrez
 */
abstract class HttpFromContainerSuite
    extends Http4sBaseSuite[Unit]
    with CatsEffectFunFixtures
    with TestContainerForAll
    with LowPrecedenceContainer2Uri {

  def httpClient: SyncIO[FunFixture[Client[IO]]] = ResourceFixture(AsyncHttpClient.resource[IO]())

  implicit class TestCreatorOps(private val testCreator: TestCreator) {

    def apply(body: Response[IO] => Any)(implicit loc: munit.Location, container2Uri: Containers => Uri): Unit =
      testCreator.execute(httpClient.test, body) { client: Client[IO] =>
        withContainers { (container: Containers) =>
          val uri = Uri.resolve(container2Uri(container), testCreator.request.req.uri)

          client.run(testCreator.request.req.withUri(uri))
        }
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
  def test(request: IO[Request[IO]]) =
    TestCreator(ContextRequest((), request.unsafeRunSync()), TestOptions(""), None, None)

}
