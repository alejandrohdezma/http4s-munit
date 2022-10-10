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

import cats.effect._

import org.http4s.client.Client

class ClientSuiteSuite extends ClientSuite {

  class PingService[F[_]: Async](client: Client[F]) {

    def ping(): F[String] = client.expect[String]("ping")

  }

  val fixture = Client.fixture(client => Resource.pure(new PingService[IO](client)))

  fixture { case GET -> Root / "ping" =>
    Ok("pong")
  }.test("PingService.ping works") { service =>
    val result = service.ping()

    assertIO(result, "pong")
  }

}
