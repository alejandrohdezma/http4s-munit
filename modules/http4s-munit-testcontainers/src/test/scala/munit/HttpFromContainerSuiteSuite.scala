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

import io.circe.Json
import io.circe.syntax._
import org.http4s.Method.GET
import org.http4s.circe._
import org.http4s.client.dsl.io._
import org.http4s.syntax.all._

class HttpFromContainerSuiteSuite extends HttpFromContainerSuite {

  override val containerDef = DummyHttpContainer.Def()

  test(GET(uri"posts")) { response =>
    assertEquals(response.status.code, 200)

    val expected = Json.arr(
      Json.obj("id" := 1, "body" := "foo", "published" := true),
      Json.obj("id" := 2, "body" := "bar", "published" := false)
    )

    assertIO(response.as[Json], expected)
  }

}
