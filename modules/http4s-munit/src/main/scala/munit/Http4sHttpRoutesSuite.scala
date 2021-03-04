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
import cats.effect.Resource

import org.http4s.ContextRequest
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.Response
import org.http4s.syntax.all._

/**
 * Base class for suites testing `HttpRoutes`.
 *
 * @author Alejandro Hernández
 * @author José Gutiérrez
 */
abstract class Http4sHttpRoutesSuite extends Http4sBaseSuite[Unit] {

  /** The HTTP routes being tested */
  val routes: HttpRoutes[IO]

  implicit class TestCreatorOps(private val testCreator: TestCreator) {

    def apply(body: Response[IO] => Any)(implicit loc: munit.Location): Unit = testCreator.execute(test, body) {
      Resource.liftF(routes.orNotFound.run(testCreator.request.req))
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
    TestCreator(ContextRequest((), request.unsafeRunSync()), TestOptions(""), None, None)

}
