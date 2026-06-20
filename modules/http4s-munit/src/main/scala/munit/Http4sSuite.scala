/*
 * Copyright 2020-2026 Alejandro Hernández <https://github.com/alejandrohdezma>
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

import io.circe.parser.parse
import org.http4s.HttpApp
import org.http4s.Request
import org.http4s.Response
import org.http4s.client.Client

/** Base class for all of the other suites using http4s' requests to test HTTP servers/routes.
  *
  * @author
  *   Alejandro Hernández
  * @author
  *   José Gutiérrez
  */
trait Http4sSuite extends CatsEffectSuite with Http4sMUnitSyntax {

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
    */
  def http4sMUnitTestNameCreator: Http4sMUnitTestNameCreator = Http4sMUnitTestNameCreator.default

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

  /** Resource that creates the client used to execute this suite's requests.
    *
    * It backs [[http4sMUnitClientTestFixture]] (by default a fresh client per test) and is available ambiently inside
    * any test body through [[http4sMUnitClient]]. Both the request run by the `test(...)` DSL and any manual
    * `client.run(...)` in the body share that same instance.
    */
  @nowarn("cat=deprecation")
  def http4sMUnitClientResource: Resource[IO, Client[IO]] =
    http4sMUnitClientFixture.to[IO].toResource.flatMap { fixture =>
      Resource.make(IO.fromFuture(IO.blocking(fixture.setup(TestOptions("")))))(c =>
        IO.fromFuture(IO.blocking(fixture.teardown(c)))
      )
    }

  /** The fixture that provides the client to the `test(...)` DSL and to [[http4sMUnitClient]].
    *
    * Defaults to a `ResourceTestLocalFixture` (a fresh client per test). Override it (as an `override lazy val`) to
    * change that, for example with a `ResourceSuiteLocalFixture` to create the client once and share it across the
    * whole suite:
    *
    * {{{
    * override lazy val http4sMUnitClientTestFixture =
    *   ResourceSuiteLocalFixture("http4sMUnitClient", http4sMUnitClientResource)
    * }}}
    */
  lazy val http4sMUnitClientTestFixture: AnyFixture[Client[IO]] = // scalafix:ok DisableSyntax.valInAbstract
    ResourceTestLocalFixture("http4sMUnitClient", http4sMUnitClientResource)

  /** The client used to run the current test's request.
    *
    * It is the same instance the `test(...)` DSL uses, so stateful middleware (a `CookieJar`, a connection pool, ...)
    * is shared between the auto-run request and any manual `client.run(...)` in the body. Only valid inside a test
    * body.
    */
  def http4sMUnitClient: Client[IO] = http4sMUnitClientTestFixture()

  /** Fixture that creates the client which will be used to execute this suite's requests. */
  @deprecated("Override `http4sMUnitClientResource` instead", "3.0.0")
  def http4sMUnitClientFixture: SyncIO[FunFixture[Client[IO]]] =
    ResourceFunFixture(
      Resource.eval(
        IO.raiseError(new NotImplementedError("Override `http4sMUnitClientResource` to provide the client"))
      )
    )

  override def munitFixtures: Seq[AnyFixture[_]] = super.munitFixtures :+ http4sMUnitClientTestFixture

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

  implicit class Http4sMUnitTestCreatorOps(creator: Http4sMUnitTestCreator) {

    /** Allows overriding the app used when running this test.
      *
      * When this method is called, the request is run against the provided app instead of the client from
      * [[http4sMUnitClientResource]]. Note that `http4sMUnitClient` still returns the latter, not this app.
      */
    def withHttpApp[A](httpApp: HttpApp[IO]): Http4sMUnitTestCreator = {
      val client = Client[IO](httpApp.run(_).toResource)

      creator.copy(executor = options => body => test(options)(body(client)))
    }

  }

  /** Declares a test for the provided request against the client built by the provided fixture. */
  @deprecated("Override `http4sMUnitClientResource` and use the suite's `test` methods instead", "3.0.0")
  implicit final class ClientFunFixtureTestOps(fixture: SyncIO[FunFixture[Client[IO]]])
      extends SyncIOFunFixtureOps(fixture) {

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
      Http4sMUnitTestCreator(
        request = Right(request),
        executor = SyncIOFunFixtureOps(fixture).test,
        nameCreator = http4sMUnitTestNameCreator,
        bodyPrettifier = http4sMUnitBodyPrettifier
      )

    /** Declares a test for a request that is built effectfully when the test runs.
      *
      * Use this overload when building the request has side effects that must run at test-execution time (for example,
      * when it depends on a fixture). Since the request is not available while naming the test, providing an alias is
      * mandatory.
      *
      * @example
      *   {{{
      * test(IO(GET(uri"users" / 42))).alias("Get the user") { response =>
      *     // test body
      * }
      *   }}}
      */
    def test(request: IO[Request[IO]]): Http4sMUnitTestCreator =
      Http4sMUnitTestCreator(
        request = Left(request),
        executor = SyncIOFunFixtureOps(fixture).test,
        nameCreator = http4sMUnitTestNameCreator,
        bodyPrettifier = http4sMUnitBodyPrettifier
      )

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
    Http4sMUnitTestCreator(
      request = Right(request),
      executor = options => body => test(options)(body(http4sMUnitClient)),
      nameCreator = http4sMUnitTestNameCreator,
      bodyPrettifier = http4sMUnitBodyPrettifier
    )

  /** Declares a test for a request that is built effectfully when the test runs.
    *
    * Use this overload when building the request has side effects that must run at test-execution time (for example,
    * when it depends on a fixture). Since the request is not available while naming the test, providing an alias is
    * mandatory.
    *
    * @example
    *   {{{
    * test(IO(GET(uri"users" / 42))).alias("Get the user") { response =>
    *     // test body
    * }
    *   }}}
    */
  def test(request: IO[Request[IO]]): Http4sMUnitTestCreator =
    Http4sMUnitTestCreator(
      request = Left(request),
      executor = options => body => test(options)(body(http4sMUnitClient)),
      nameCreator = http4sMUnitTestNameCreator,
      bodyPrettifier = http4sMUnitBodyPrettifier
    )

}
