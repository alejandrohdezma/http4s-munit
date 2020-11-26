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

import cats.effect.IO

import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.Uri

/**
 * Base class for suites testing `HttpRoutes`.
 *
 * @author Alejandro Hernández
 * @author José Gutiérrez
 */
abstract class Http4sHttpRoutesSuite extends Http4sSuite[Request[IO], HttpRoutes[IO]] {

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
  override def munitHttp4sNameCreator(request: Request[IO], testOptions: TestOptions): String = {
    val alias = if (testOptions.name.nonEmpty) s" (${testOptions.name})" else ""

    s"${request.method.name} -> ${Uri.decode(request.uri.renderString)}$alias"
  }

}
