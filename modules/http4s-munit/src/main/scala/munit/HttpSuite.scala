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
import cats.syntax.all._

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
abstract class HttpSuite extends CatsEffectSuite with CatsEffectFunFixtures {

  /**
   * The base URI for all tests. This URI will prepend the one used in each
   * test's request.
   */
  val baseUri: Uri

  /**
   * Allows altering the name of the generated tests.
   *
   * By default it will generate test names like:
   *
   * {{{
   * test(GET(uri"users" / 42))               // GET -> users/42
   * test(GET(uri"users")).alias("All users") // GET -> users (All users)
   * }}}
   */
  def munitHttp4sNameCreator(request: Request[IO], testOptions: TestOptions): String = {
    val clue = if (testOptions.name.nonEmpty) s" (${testOptions.name})" else ""

    s"${request.method.name} -> ${Uri.decode(request.uri.renderString)}$clue"
  }

  def httpClient: SyncIO[FunFixture[Client[IO]]] = ResourceFixture(AsyncHttpClient.resource[IO]())

  case class TestCreator(request: Request[IO], testOptions: TestOptions) {

    /** Mark a test case that is expected to fail */
    def fail: TestCreator = tag(Fail)

    /**
     * Mark a test case that has a tendency to non-deterministically fail for known or unknown reasons.
     *
     * By default, flaky tests fail like basic tests unless the `MUNIT_FLAKY_OK` environment variable is set to `true`.
     * You can override [[munitFlakyOK]] to customize when it's OK for flaky tests to fail.
     */
    def flaky: TestCreator = tag(Flaky)

    /** Skips an individual test case in a test suite */
    def ignore: TestCreator = tag(Ignore)

    /** When running munit, run only a single test */
    def only: TestCreator = tag(Only)

    /** Add a tag to this test */
    def tag(t: Tag): TestCreator = copy(testOptions = testOptions.tag(t))

    /** Adds an alias to this test (the test name will be suffixed with this alias when printed) */
    def alias(s: String): TestCreator = copy(testOptions = testOptions.withName(s))

    def apply(body: Response[IO] => Any)(implicit loc: munit.Location): Unit =
      httpClient.test(testOptions.withName(munitHttp4sNameCreator(request, testOptions)).withLocation(loc)) {
        _.run(request).use { response =>
          body(response) match {
            case io: IO[Any] => io
            case a           => IO(a)
          }
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
  def test(request: IO[Request[IO]]): TestCreator =
    TestCreator(
      request.map(r => r.withUri(Uri.resolve(baseUri, r.uri))).unsafeRunSync(),
      new TestOptions("", Set.empty, Location.empty)
    )

}
