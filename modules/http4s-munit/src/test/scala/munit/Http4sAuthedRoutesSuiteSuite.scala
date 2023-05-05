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

import cats.effect.IO

import org.http4s.AuthedRequest
import org.http4s.AuthedRoutes
import org.http4s.Request
import org.typelevel.vault.Key

class Http4sAuthedRoutesSuiteSuite extends Http4sSuite {

  implicit val key = Key.newKey[IO, String].unsafeRunSync()

  override def http4sMUnitClientFixture = AuthedRoutes
    .of[String, IO] {
      case GET -> Root / "hello" as user        => Ok(s"$user: Hi")
      case GET -> Root / "hello" / name as user => Ok(s"$user: Hi $name")
    }
    .orFail
    .local((r: Request[IO]) => AuthedRequest(r.getContext[String], r))
    .asFixture

  test(GET(uri"/hello").context("jose")).alias("Test 1") { response =>
    assertIO(response.as[String], "jose: Hi")
  }

  test(GET(uri"/hello" / "Jose").context("alex")).alias("Test 2") { response =>
    assertIO(response.as[String], "alex: Hi Jose")
  }

  test(GET(uri"/hello").context("jose")).withHttpApp {
    AuthedRoutes
      .of[String, IO] { case GET -> Root / "hello" as user => Ok(s"$user: Hey") }
      .orFail
      .local((r: Request[IO]) => AuthedRequest(r.getContext[String], r))
  }.alias("Test 1 (overriding routes)") { response =>
    assertIO(response.as[String], "jose: Hey")
  }

  test(GET(uri"/hello" / "Jose").context("alex")).withHttpApp {
    AuthedRoutes
      .of[String, IO] { case GET -> Root / "hello" / name as user => Ok(s"$user: Hey $name") }
      .orFail
      .local((r: Request[IO]) => AuthedRequest(r.getContext[String], r))
  }.alias("Test 2 (overriding routes)") { response =>
    assertIO(response.as[String], "alex: Hey Jose")
  }

}
