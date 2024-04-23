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

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.all._

import io.circe.Json
import org.http4s.circe._
import org.http4s.ember.client.EmberClientBuilder

@SuppressWarnings(Array("scalafix:DisableSyntax.null", "scalafix:DisableSyntax.var"))
class HttpSuiteSuite extends Http4sSuite {

  def httpClient = EmberClientBuilder.default[IO].build.map(_.withUpdatedUri(uri"https://api.github.com".resolve))

  override def http4sMUnitClientFixture = ResourceFixture(httpClient)

  var clientFixtureResult: String = null

  ResourceFixture {
    httpClient.flatTap { client =>
      client
        .expect[Json](uri"repos/alejandrohdezma/http4s-munit")
        .flatMap(_.hcursor.get[String]("name").liftTo[IO])
        .toResource
        .flatMap { name =>
          Resource.make(IO { clientFixtureResult = name })(_ => IO { clientFixtureResult = null })
        }
    }
  }.test(GET(uri"repos/alejandrohdezma/http4s-munit")) { response =>
    assertEquals(response.status.code, 200, response.clues)

    assertEquals(clientFixtureResult, "http4s-munit")

    val result = response.as[Json].map(_.hcursor.get[String]("name"))

    assertIO(result, Right(clientFixtureResult), response.clues)
  }

  test("Client.fixture teardowns after test") {
    assert(Option(clientFixtureResult).isEmpty)
  }

  test(GET(uri"users/gutiory")) { response =>
    assertEquals(response.status.code, 200, response.clues)

    val result = response.as[Json].map(_.hcursor.get[String]("login"))

    assertIO(result, Right("gutiory"), response.clues)
  }

  http4sMUnitClientFixture.test(GET(uri"users/alejandrohdezma")) { response =>
    assertEquals(response.status.code, 200, response.clues)

    val result = response.as[Json].map(_.hcursor.get[String]("login"))

    assertIO(result, Right("alejandrohdezma"), response.clues)
  }

}
