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

import com.dimafeng.testcontainers.munit.TestContainerForAll
import fs2.Stream
import io.circe.parser.parse
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
    extends CatsEffectSuite
    with CatsEffectFunFixtures
    with TestContainerForAll
    with LowPrecedenceContainer2Uri {

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

  def munitHttp4sBodyPrettifier(body: String): String =
    parse(body)
      .map(_.spaces2)
      .fold(
        _ => body,
        json =>
          if (munitAnsiColors)
            json
              .replaceAll("""(\"\w+\") : """, Console.CYAN + "$1" + Console.RESET + " : ")
              .replaceAll(""" : (\".*\")""", " : " + Console.YELLOW + "$1" + Console.RESET)
              .replaceAll(""" : (-?\d+\.\d+)""", " : " + Console.GREEN + "$1" + Console.RESET)
              .replaceAll(""" : (-?\d+)""", " : " + Console.GREEN + "$1" + Console.RESET)
              .replaceAll(""" : true""", " : " + Console.MAGENTA + "true" + Console.RESET)
              .replaceAll(""" : false""", " : " + Console.MAGENTA + "false" + Console.RESET)
              .replaceAll(""" : null""", " : " + Console.MAGENTA + "null" + Console.RESET)
          else json
      )

  def httpClient: SyncIO[FunFixture[Client[IO]]] = ResourceFixture(AsyncHttpClient.resource[IO]())

  case class TestCreator(
      request: Request[IO],
      testOptions: TestOptions,
      repetitions: Option[Int],
      runInParallel: Option[Boolean],
      maxParallel: Option[Int]
  ) {

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

    def apply(body: Response[IO] => Any)(implicit loc: munit.Location, container2Uri: Containers => Uri): Unit =
      httpClient.test(testOptions.withName(munitHttp4sNameCreator(request, testOptions)).withLocation(loc)) { client =>
        withContainers { (container: Containers) =>
          val uri = Uri.resolve(container2Uri(container), request.uri)

          val run = client
            .run(request.withUri(uri))
            .use { response =>
              IO(body(response)).attempt.flatMap {
                case Right(io: IO[Any]) => io
                case Right(a)           => IO.pure(a)
                case Left(t: FailExceptionLike[_]) if t.getMessage().contains("Clues {\n") =>
                  response.bodyText.compile.string.map(munitHttp4sBodyPrettifier(_)) >>= { body =>
                    t.getMessage().split("Clues \\{") match {
                      case Array(p1, p2) =>
                        val bodyClue =
                          "Clues {\n  response.bodyText.compile.string: String = \"\"\"\n" +
                            body.split("\n").map("    " + _).mkString("\n") + "\n  \"\"\","
                        IO.raiseError(t.withMessage(p1 + bodyClue + p2))
                      case _ => IO.raiseError(t)
                    }
                  }
                case Left(t: FailExceptionLike[_]) =>
                  response.bodyText.compile.string.map(munitHttp4sBodyPrettifier(_)) >>= { body =>
                    IO.raiseError(t.withMessage(s"${t.getMessage()}\n\nResponse body was:\n\n$body\n"))
                  }
                case Left(t) => IO.raiseError(t)
              }
            }
          runInParallel.fold(sequentialExecution(run)) {
            case false => sequentialExecution(run)
            case true  => parallelExecution(run)
          }
        }
      }

    /** Allows to run the same test several times sequencially */
    def repeat(times: Int) = copy(repetitions = times.some)

    /** Allows to run the tests in paralell */
    def parallel = copy(runInParallel = true.some)

    def parallel(maxParallel: Int) = copy(runInParallel = true.some, maxParallel = maxParallel.some)

    /** Force the test to be executed just once */
    def doNotRepeat = copy(repetitions = None)

    private def sequentialExecution(run: IO[Any]) = Stream
      .emits(1 to repetitions.getOrElse(1))
      .covary[IO]
      .evalMap(_ => run)
      .compile
      .drain
      .unsafeRunSync()

    private def parallelExecution(run: IO[Any]) = Stream
      .emits(1 to repetitions.getOrElse(1))
      .covary[IO]
      .parEvalMapUnordered(maxParallel.getOrElse(1))(_ => run)
      .compile
      .drain
      .unsafeRunSync()
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
    TestCreator(request.unsafeRunSync(), new TestOptions("", Set.empty, Location.empty), None, None, None)

}
