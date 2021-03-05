package munit

import cats.Show
import cats.effect.IO
import cats.effect.Resource
import cats.effect.SyncIO
import cats.syntax.all._

import fs2.Stream
import io.circe.parser.parse
import org.http4s.ContextRequest
import org.http4s.Response
import org.http4s.Uri

/**
 * Base class for all of the other suites using http4s' requests to test HTTP servers/routes.
 *
 * @author Alejandro Hernández
 * @author José Gutiérrez
 */
abstract class Http4sSuite[A: Show] extends CatsEffectSuite {

  /**
   * Allows altering the name of the generated tests.
   *
   * It will generate test names like:
   *
   * {{{
   * test(GET(uri"users" / 42))               // GET -> users/42
   * test(GET(uri"users")).alias("All users") // GET -> users (All users)
   * }}}
   *
   * @param request the test's request
   * @param testOptions the options for the current test
   * @return the test's name
   */
  def munitHttp4sNameCreator(
      request: ContextRequest[IO, A],
      testOptions: TestOptions,
      repetitions: Option[Int],
      maxParallel: Option[Int]
  ): String = {
    val clue = if (testOptions.name.nonEmpty) s" (${testOptions.name})" else ""

    val context = request.context match {
      case _: Unit => None
      case context => context.show.some.filterNot(_.isEmpty())
    }

    val reps = repetitions match {
      case Some(rep) if rep > 1 =>
        s" - executed $rep times" + maxParallel.fold("")(paral => s" with $paral in parallel")
      case _ => ""
    }
    s"${request.req.method.name} -> ${Uri.decode(request.req.uri.renderString)}$clue${context.fold("")(" as " + _)}$reps"
  }

  /**
   * Allows pretiffing the response's body before outputting it to logs.
   *
   * By default it will try to parse it as JSON and apply a code highlight
   * if `munitAnsiColors` is `true`.
   *
   * @param body the response's body to prettify
   * @return the prettified version of the response's body
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

  /**
   * Base fixture used to obtain a response from a request. Can be re-implemented if you want
   * to override the default behaviour of a suite.
   */
  def http4sMUnitFunFixture: SyncIO[FunFixture[ContextRequest[IO, A] => Resource[IO, Response[IO]]]]

  case class Http4sMUnitTestCreator(
      request: ContextRequest[IO, A],
      testOptions: TestOptions = TestOptions(""),
      repetitions: Option[Int] = None,
      maxParallel: Option[Int] = None
  ) {

    /** Mark a test case that is expected to fail */
    def fail: Http4sMUnitTestCreator = tag(Fail)

    /**
     * Mark a test case that has a tendency to non-deterministically fail for known or unknown reasons.
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
      else copy(repetitions = times.some)

    /** Allows to run the tests in parallel */
    def parallel(maxParallel: Int = 5 /* scalafix:ok */ ) =
      if (maxParallel < 1) Assertions.fail("maxParallel must be > 0")
      else copy(maxParallel = maxParallel.some)

    /** Force the test to be executed just once */
    def doNotRepeat = copy(repetitions = None)

    def apply(body: Response[IO] => Any)(implicit loc: Location): Unit =
      http4sMUnitFunFixture.test(testOptions.withName(http4sMUnitNameCreator(request, testOptions)).withLocation(loc)) {
        client =>
          Stream
            .emits(1 to repetitions.getOrElse(config.values.repetitions.getOrElse(1)))
            .covary[IO]
            .parEvalMapUnordered(maxParallel.getOrElse(config.values.maxConcurrent.getOrElse(1))) { _ =>
              client(request).use { response =>
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
            }
            .compile
            .drain
            .unsafeRunSync()
      }

  }

}
