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
import cats.effect.Resource
import cats.syntax.all._

import com.dimafeng.testcontainers.GenericContainer
import org.http4s._
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.ci._

class TestContainersSuite extends munit.Http4sSuite {

  lazy val container = GenericContainer(dockerImage = "nginxdemos/hello", exposedPorts = List(80))

  /** This will start/stop a hello server before/after every test */
  override def http4sMUnitClientFixture = ResourceFunFixture {
    Resource.fromAutoCloseable(IO(container.start()).as(container)) >>
      EmberClientBuilder.default[IO].build.map(_.withUpdatedUri(localhost.withPort(container.mappedPort(80)).resolve))
  }

  override def http4sMUnitResponseClueCreator(response: Response[IO]) = {
    val logs = response.headers
      .get(ci"x-request-id")
      .map(_.head.value)
      .map(id => container.logs.split("\n").filter(_.contains(id)).mkString("\n"))
      .getOrElse(container.logs)

    clues(response, logs)
  }

  test(GET(uri"ping")) { response =>
    assertEquals(response.status.code, 200, response.clues)
  }

}
