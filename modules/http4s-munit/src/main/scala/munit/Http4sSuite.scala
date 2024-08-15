/*
 * Copyright 2020-2024 Alejandro Hernández <https://github.com/alejandrohdezma>
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
  @deprecated(message = "Use `http4sMUnitTestNameCreator` instead", since = "0.16.0")
  def http4sMUnitNameCreator(
      request: Request[IO],
      followingRequests: List[String],
      testOptions: TestOptions,
      config: Http4sMUnitConfig
  ): String = Http4sMUnitDefaults.http4sMUnitNameCreator(
    request, followingRequests, testOptions, config, http4sMUnitNameCreatorReplacements()
  )

  /** List of replacements that will be applied to the result of `http4sMUnitNameCreator` using `String#replaceAll` */
  @deprecated(
    message = "Override `http4sMUnitTestNameCreator` instead and provide replacements to `default` constructor",
    since = "0.16.0"
  )
  def http4sMUnitNameCreatorReplacements(): Seq[(String, String)] = Nil

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
  def http4sMUnitTestNameCreator: Http4sMUnitTestNameCreator = http4sMUnitNameCreator(_, _, _, _): @nowarn

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

  implicit class Http4sMUnitTestCreatorOps(creator: Http4sMUnitTestCreator) {

    /** Allows overriding the app used when running this test.
      *
      * When this method is called, the test ignores the fixture on `http4sMUnitClientFixture` and runs the request
      * against the provided app.
      */
    def withHttpApp[A](httpApp: HttpApp[IO]): Http4sMUnitTestCreator = {
      val client = Client[IO](httpApp.run(_).toResource)

      creator.copy(executor = options => body => test(options)(body(client)))
    }

  }

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
        request = request,
        executor = fixture.test,
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
      request = request,
      executor = options => f => (http4sMUnitFunFixture: @nowarn).test(options)(client => f(Client[IO](client))),
      nameCreator = http4sMUnitTestNameCreator,
      bodyPrettifier = http4sMUnitBodyPrettifier
    )

}
