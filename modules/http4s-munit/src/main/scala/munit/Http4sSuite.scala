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

import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.IO

import org.http4s.Response
import org.http4s.syntax.all._

/**
 * The base class for all http4s suites.
 *
 * @author Alejandro Hernández
 * @author José Gutiérrez
 *
 * @param Req The request type (`org.http4s.Request` or `org.http4s.AuthedRequest`)
 * @param Routes The routes type (`org.http4s.HttpRoutes` or `org.http4s.AuthedRoutes`)
 */
abstract class Http4sSuite[Req, Routes <: Kleisli[OptionT[IO, *], Req, Response[IO]]] extends CatsEffectSuite {

  /** The HTTP routes being tested */
  val routes: Routes

  /**
   * Allows altering the name of the generated tests.
   */
  def munitHttp4sNameCreator(request: Req, testOptions: TestOptions): String

  case class TestCreator(request: Req, testOptions: TestOptions) {

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
      test(testOptions.withName(munitHttp4sNameCreator(request, testOptions)).withLocation(loc)) {
        routes.orNotFound.run(request).map(body).flatMap {
          case io: IO[Any] => io
          case a           => IO(a)
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
  def test(request: IO[Req]): TestCreator =
    TestCreator(request.unsafeRunSync(), new TestOptions("", Set.empty, Location.empty))

}
