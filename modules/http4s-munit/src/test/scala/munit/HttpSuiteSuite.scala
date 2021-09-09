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

import scala.concurrent.ExecutionContext.global

import cats.effect.IO
import cats.effect.Resource

import io.circe.Json
import org.http4s.Method.GET
import org.http4s.Uri
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.dsl.io._
import org.http4s.syntax.all._

class HttpSuiteSuite extends HttpSuite {

  override def http4sMUnitClient: Resource[IO, Client[IO]] = BlazeClientBuilder[IO](global).resource

  override def baseUri(): Uri = uri"https://api.github.com"

  test(GET(uri"users/gutiory")) { response =>
    assertEquals(response.status.code, 200)

    val result = response.as[Json].map(_.hcursor.get[String]("login"))

    assertIO(result, Right("gutiory"))
  }

}
