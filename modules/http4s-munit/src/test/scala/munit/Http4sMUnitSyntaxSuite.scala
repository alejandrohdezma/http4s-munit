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

import cats.effect.IO
import cats.effect._

import org.http4s.Header
import org.http4s.client.Client
import org.typelevel.ci._

class Http4sMUnitSyntaxSuite extends CatsEffectSuite with Http4sMUnitSyntax {

  class PingService[F[_]: Async](client: Client[F]) {

    def ping(): F[String] = client.expect[String]("ping")

  }

  val fixture = Client.partialFixture(client => Resource.pure(new PingService[IO](client)))

  fixture { case GET -> Root / "ping" =>
    Ok("pong")
  }.test("PingService.ping works") { service =>
    val result = service.ping()

    assertIO(result, "pong")
  }

  test("header interpolator creates a valid raw header") {
    val header = ci"my-header" := "my-value"

    assertEquals(header, Header.Raw(ci"my-header", "my-value"))
  }

}
