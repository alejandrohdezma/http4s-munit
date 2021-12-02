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
import cats.syntax.all._

import com.dimafeng.testcontainers.GenericContainer
import com.dimafeng.testcontainers.munit.TestContainerForAll
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.Method._
import org.http4s.Response
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.client.dsl.io._
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.syntax.all._
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.wait.strategy.Wait

class HttpFromContainerSuiteSuite extends HttpFromContainerSuite with TestContainerForAll {

  override def http4sMUnitResponseClueCreator(response: Response[IO]): Clues =
    new Clues(super.http4sMUnitResponseClueCreator(response).values :+ Clue.generate(withContainers(_.logs)))

  override def http4sMUnitClient: Resource[IO, Client[IO]] = EmberClientBuilder.default[IO].build

  override val containerDef = GenericContainer.Def(
    dockerImage = "clue/json-server",
    exposedPorts = Seq(80),
    classpathResourceMapping = Seq(("db.json", "/data/db.json", BindMode.READ_ONLY)),
    waitStrategy = Wait.forHttp("/posts")
  )

  case class Post(id: Int)

  test(GET(uri"posts"))
    .alias("retrieve the list of posts")
    .andThen("get the first post from the list")(_.as[List[Post]].map {
      case Nil               => fail("The list of posts should not be empty")
      case (head: Post) :: _ => GET(uri"posts" / head.id.show)
    })
    .andThen("delete it")(_.as[Post].map { post =>
      DELETE(uri"posts" / post.id.show)
    }) { response =>
      assertEquals(response.status.code, 200, response.clues)

      assertIO(response.as[Json], Json.obj(), response.clues)
    }

  test(GET(uri"posts")).alias("retrieve the remaining posts") { response =>
    assertEquals(response.status.code, 200, response.clues)

    val expected = Json.arr(
      Json.obj("id" := 2, "body" := "Second", "published" := false),
      Json.obj("id" := 3, "body" := "Third", "published"  := true)
    )

    assertIO(response.as[Json], expected, response.clues)
  }

}
