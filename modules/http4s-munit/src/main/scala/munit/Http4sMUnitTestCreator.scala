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
import cats.syntax.all._

import fs2.Stream
import org.http4s.Request
import org.http4s.Response
import org.http4s.client.Client

/** Represents a class capable of creating a test from a request.
  *
  * @param request
  *   the request to test against
  * @param executor
  *   the method used to execute the test
  * @param nameCreator
  *   the instance used to create the test's name
  * @param bodyPrettifier
  *   the method used to prettify the response body on errors
  * @param followingRequests
  *   the list of requests to apply after the first one
  * @param testOptions
  *   this test's options
  * @param config
  *   the configuration with which to run the test
  */
final case class Http4sMUnitTestCreator(
    request: Request[IO],
    executor: TestOptions => (Client[IO] => IO[Unit]) => Unit,
    nameCreator: Http4sMUnitTestNameCreator = Http4sMUnitTestNameCreator.default,
    bodyPrettifier: String => String = identity,
    followingRequests: List[(String, Response[IO] => IO[Request[IO]])] = Nil,
    testOptions: TestOptions = TestOptions(""),
    config: Http4sMUnitConfig = Http4sMUnitConfig.default
) {

  /** Mark a test case that is expected to fail */
  def fail: Http4sMUnitTestCreator = tag(Fail)

  /** Mark a test case that has a tendency to non-deterministically fail for known or unknown reasons.
    *
    * By default, flaky tests fail like basic tests unless the `MUNIT_FLAKY_OK` environment variable is set to `true`.
    * You can override `munitFlakyOK` to customize when it's OK for flaky tests to fail.
    */
  def flaky: Http4sMUnitTestCreator = tag(Flaky)

  /** Skips an individual test case in a test suite */
  def ignore: Http4sMUnitTestCreator = tag(Ignore)

  /** When running munit, run only a single test */
  def only: Http4sMUnitTestCreator = tag(Only)

  /** Add a tag to this test */
  def tag(t: Tag): Http4sMUnitTestCreator = copy(testOptions = testOptions.tag(t))

  /** Adds an alias to this test (the test name will be suffixed with this alias when printed) */
  def alias(s: String): Http4sMUnitTestCreator = copy(testOptions = testOptions.withName(s))

  /** Allows to run the same test several times sequencially */
  def repeat(times: Int) =
    if (times < 1) Assertions.fail("times must be > 0")
    else copy(config = Http4sMUnitConfig(times.some, config.maxParallel, config.showAllStackTraces))

  /** Force the test to be executed just once */
  def doNotRepeat = copy(config = Http4sMUnitConfig(None, None, config.showAllStackTraces))

  /** Allows to run the tests in parallel */
  def parallel(maxParallel: Int = 5) =
    if (maxParallel < 1) Assertions.fail("maxParallel must be > 0")
    else copy(config = Http4sMUnitConfig(config.repetitions, maxParallel.some, config.showAllStackTraces))

  /** Provide a new request created from the response of the previous request. The alias entered as parameter will be
    * used to construct the test's name.
    *
    * If this is the last `andThen` call, the response provided to the test will be the one obtained from executing this
    * request
    */
  def andThen(alias: String)(f: Response[IO] => IO[Request[IO]]): Http4sMUnitTestCreator =
    copy(followingRequests = followingRequests :+ ((alias, f)))

  /** Provide a new request created from the response of the previous request.
    *
    * If this is the last `andThen` call, the response provided to the test will be the one obtained from executing this
    * request
    */
  def andThen(f: Response[IO] => IO[Request[IO]]): Http4sMUnitTestCreator = andThen("")(f)

  def apply(body: Response[IO] => Any)(implicit loc: Location): Unit = executor {
    val options = testOptions.withLocation(loc)
    options.withName(nameCreator.nameFor(request, followingRequests.map(_._1), options, config))
  } { (client: Client[IO]) =>
    val numRepetitions     = config.repetitions.getOrElse(1)
    val showAllStackTraces = config.showAllStackTraces.getOrElse(false)
    Stream
      .range[IO, Int](1, numRepetitions + 1)
      .parEvalMapUnordered(config.maxParallel.getOrElse(1)) { _ =>
        followingRequests
          .foldLeft(client.run(request)) { (previousRequest, nextRequest) =>
            for {
              previousResponse <- previousRequest
              request          <- nextRequest._2(previousResponse).toResource
              response         <- client.run(request)
            } yield response
          }
          .use { response =>
            IO(body(response)).attempt.flatMap {
              case Right(io: IO[Any]) => io
              case Right(a)           => IO.pure(a)
              case Left(t: FailExceptionLike[_]) if t.getMessage().contains("Clues {\n") =>
                response.bodyText.compile.string.map(bodyPrettifier(_)) >>= { body =>
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
                response.bodyText.compile.string.map(bodyPrettifier(_)) >>= { body =>
                  IO.raiseError(t.withMessage(s"${t.getMessage()}\n\nResponse body was:\n\n$body\n"))
                }
              case Left(t) => IO.raiseError(t)
            }
          }
          .attempt
      }
      .mapFilter(_.swap.toOption)
      .compile
      .toList
      .flatMap {
        case Nil                                      => IO.unit
        case List(throwables) if numRepetitions === 1 => IO.raiseError(throwables)
        case throwables if showAllStackTraces =>
          IO.raiseError(
            new FailException(
              s"${throwables.size} / $numRepetitions  tests failed while execution this parallel test\n${throwables
                  .map(_.getMessage())
                  .mkString("/n/n")}",
              loc
            )
          )
        case throwables =>
          IO.raiseError(
            new FailException(
              s"${throwables.size} / $numRepetitions tests failed while execution this parallel test",
              loc
            )
          )
      }
  }

}
