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

import cats.effect.IO
import cats.syntax.all._

import org.http4s.HttpRoutes
import org.http4s.client.middleware.CookieJar

class Http4sMUnitAmbientClientSuite extends Http4sSuite {

  override def http4sMUnitClientResource = HttpRoutes
    .of[IO] {
      case GET -> Root / "login"        => Ok("ok").map(_.addCookie("sid", "abc"))
      case req @ GET -> Root / "whoami" => Ok(req.cookies.find(_.name === "sid").map(_.content).getOrElse("anonymous"))
      case GET -> Root / "ping"         => Ok("pong")
    }
    .orFail
    .asClient
    .evalMap(CookieJar.impl[IO](_))

  test("the ambient client is usable from a plain test body") {
    assertIO(http4sMUnitClient.expect[String](GET(uri"http://localhost/ping")), "pong")
  }

  test(GET(uri"http://localhost/login")).alias("the body shares the request's client (same CookieJar)") { _ =>
    assertIO(http4sMUnitClient.expect[String](GET(uri"http://localhost/whoami")), "abc")
  }

  test("each test gets a fresh client (the cookie jar is empty)") {
    assertIO(http4sMUnitClient.expect[String](GET(uri"http://localhost/whoami")), "anonymous")
  }

}
