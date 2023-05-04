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

import scala.annotation.nowarn

import cats.effect.IO
import cats.effect.Resource
import cats.effect.SyncIO
import cats.syntax.all._

import fs2.Stream
import io.circe.parser.parse
import org.http4s.Header
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.syntax.AllSyntax
import org.typelevel.ci.CIString

/** Base class for all of the other suites using http4s' requests to test HTTP servers/routes.
  *
  * @author
  *   Alejandro Hernández
  * @author
  *   José Gutiérrez
  */
trait Http4sSuite extends CatsEffectSuite with Http4sDsl[IO] with Http4sClientDsl[IO] with AllSyntax {

  /** Allows altering the name of the generated tests.
    *
    * By default it will generate test names like:
    *
    * {{{
    * // GET -> users/42
    * test(GET(uri"users" / 42))
    *
    * // GET -> users (All users)
    * test(GET(uri"users")).alias("All users")
    *
    * // GET -> users as user-1
    * test(GET(uri"users").as("user-1"))
    *
    * // GET -> users - executed 10 times with 2 in parallel
    * test(GET(uri"users")).repeat(10).parallel(2)
    *
    * // GET -> users (retrieve the list of users and get the first user from the list)
    * test(GET(uri"users"))
    *     .alias("retrieve the list of users")
    *     .andThen("get the first user from the list")(_.as[List[User]].flatMap {
    *       case Nil               => fail("The list of users should not be empty")
    *       case (head: User) :: _ => GET(uri"users" / head.id.show)
    *     })
    * }}}
    *
    * @param request
    *   the test's request
    * @param followingRequests
    *   the following request' aliases
    * @param testOptions
    *   the options for the current test
    * @param config
    *   the configuration for this test
    * @return
    *   the test's name
    */
  def http4sMUnitNameCreator(
      request: Request[IO],
      followingRequests: List[String],
      testOptions: TestOptions,
      config: Http4sMUnitConfig
  ): String = Http4sMUnitDefaults.http4sMUnitNameCreator(
    request,
    followingRequests,
    testOptions,
    config,
    http4sMUnitNameCreatorReplacements()
  )

  /** List of replacements that will be applied to the result of `http4sMUnitNameCreator` using `String#replaceAll` */
  def http4sMUnitNameCreatorReplacements(): Seq[(String, String)] = Nil

  /** Returns the response as suite clues.
    *
    * This method is then used by `response.clues` extension method.
    *
    * @param response
    *   the response to convert to `Clues`
    * @return
    *   the clues extracted from the response
    */
  def http4sMUnitResponseClueCreator(response: Response[IO]): Clues =
    clues(response.headers.show, response.status.show)

  /** Fixture to run a request against this suite */
  @deprecated("Use `http4sMUnitClientFixture` instead", since = "0.16.0")
  def http4sMUnitFunFixture: SyncIO[FunFixture[Request[IO] => Resource[IO, Response[IO]]]] =
    http4sMUnitClientFixture.map { fixture =>
      FunFixture.async(
        setup = options => fixture.setup(options).map(_.run _),
        teardown = f => fixture.teardown(Client.apply(f))
      )
    }

  /** Fixture that creates the client which will be used to execute this suite's requests. */
  def http4sMUnitClientFixture: SyncIO[FunFixture[Client[IO]]]

  implicit class ResponseCluesOps(private val response: Response[IO]) {

    /** Transform a response into suite clues
      *
      * The output of this extension method can be controlled with `http4sMUnitResponseClueCreator`.
      */
    def clues: Clues = http4sMUnitResponseClueCreator(response)

  }

  /** Allows prettifing the response's body before outputting it to logs.
    *
    * By default it will try to parse it as JSON and apply a code highlight if `munitAnsiColors` is `true`.
    *
    * @param body
    *   the response's body to prettify
    * @return
    *   the prettified version of the response's body
    */
  def http4sMUnitBodyPrettifier(body: String): String =
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

  implicit final class CiStringHeaderOps(ci: CIString) {

    /** Creates a `Header.Raw` value from a case-insensitive string. */
    def :=(value: String): Header.Raw = Header.Raw(ci, value)

  }

  def localhost = uri"http://localhost"

  implicit class ClientWithBaseUriOps(client: Client[IO]) {

    /** Prepends the provided `Uri` to every request made by this client. */
    def withBaseUri(uri: Uri): Client[IO] = Client(request => client.run(request.withUri(uri.resolve(request.uri))))

  }

  implicit class UriWithPort(uri: Uri) {

    /** Allows changing the URIs port */
    def withPort(port: Int): Uri = {
      val authority = uri.authority.fold(Uri.Authority(port = Some(port)))(_.copy(port = Some(port)))
      uri.copy(authority = Some(authority))
    }

  }

  case class Http4sMUnitTestCreator(
      request: Request[IO],
      http4sMUnitFunFixture: SyncIO[FunFixture[Request[IO] => Resource[IO, Response[IO]]]],
      followingRequests: List[(String, Response[IO] => IO[Request[IO]])] = Nil,
      testOptions: TestOptions = TestOptions(""),
      config: Http4sMUnitConfig = Http4sMUnitConfig.default
  ) {

    /** Mark a test case that is expected to fail */
    def fail: Http4sMUnitTestCreator = tag(Fail)

    /** Mark a test case that has a tendency to non-deterministically fail for known or unknown reasons.
      *
      * By default, flaky tests fail like basic tests unless the `MUNIT_FLAKY_OK` environment variable is set to `true`.
      * You can override [[munitFlakyOK]] to customize when it's OK for flaky tests to fail.
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
    def parallel(maxParallel: Int = 5 /* scalafix:ok */ ) =
      if (maxParallel < 1) Assertions.fail("maxParallel must be > 0")
      else copy(config = Http4sMUnitConfig(config.repetitions, maxParallel.some, config.showAllStackTraces))

    /** Provide a new request created from the response of the previous request. The alias entered as parameter will be
      * used to construct the test's name.
      *
      * If this is the last `andThen` call, the response provided to the test will be the one obtained from executing
      * this request
      */
    def andThen(alias: String)(f: Response[IO] => IO[Request[IO]]): Http4sMUnitTestCreator =
      copy(followingRequests = followingRequests :+ ((alias, f)))

    /** Provide a new request created from the response of the previous request.
      *
      * If this is the last `andThen` call, the response provided to the test will be the one obtained from executing
      * this request
      */
    def andThen(f: Response[IO] => IO[Request[IO]]): Http4sMUnitTestCreator = andThen("")(f)

    def apply(body: Response[IO] => Any)(implicit loc: Location): Unit =
      http4sMUnitFunFixture.test(
        testOptions
          .withName(http4sMUnitNameCreator(request, followingRequests.map(_._1), testOptions.withLocation(loc), config))
          .withLocation(loc)
      ) { client =>
        val numRepetitions     = config.repetitions.getOrElse(1)
        val showAllStackTraces = config.showAllStackTraces.getOrElse(false)
        Stream
          .emits(1 to numRepetitions)
          .covary[IO]
          .parEvalMapUnordered(config.maxParallel.getOrElse(1)) { _ =>
            followingRequests
              .foldLeft(client(request)) { (previousRequest, nextRequest) =>
                for {
                  previousResponse <- previousRequest
                  request          <- nextRequest._2(previousResponse).to[Resource[IO, *]]
                  response         <- client(request)
                } yield response
              }
              .use { response =>
                IO(body(response)).attempt.flatMap {
                  case Right(io: IO[Any]) => io
                  case Right(a)           => IO.pure(a)
                  case Left(t: FailExceptionLike[_]) if t.getMessage().contains("Clues {\n") =>
                    response.bodyText.compile.string.map(http4sMUnitBodyPrettifier(_)) >>= { body =>
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
                    response.bodyText.compile.string.map(http4sMUnitBodyPrettifier(_)) >>= { body =>
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

  /** Declares a test for the provided request.
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
  def test(request: Request[IO]): Http4sMUnitTestCreator =
    Http4sMUnitTestCreator(request, http4sMUnitFunFixture: @nowarn)

}
