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

import java.time.Instant
import java.util.UUID

import cats.effect.IO

import io.circe.Json
import io.circe.literal._
import io.circe.syntax._

class Http4sMUnitJsonSuite extends CatsEffectSuite with Http4sMUnitSyntax {

  val obtained: Json = Json.obj(
    "id"         := UUID.randomUUID(),
    "name"       := "Jose",
    "created_at" := Instant.now(),
    "tags"       := List("a", "b"),
    "nested"     := Json.obj("id" := UUID.randomUUID())
  )

  test("placeholders stand for any value they accept") {
    val expected = json"""{
      "id": "<uuid>",
      "name": "Jose",
      "created_at": "<recent timestamp>",
      "tags": ["a", "b"],
      "nested": { "id": "<uuid>" }
    }"""

    assertIOJson(IO.pure(obtained), expected)
  }

  test("a value that does not satisfy its placeholder is reported as received") {
    val expected = json"""{ "id": "<uuid>", "name": "<recent timestamp>" }"""

    val obtained = json"""{ "id": "not-a-uuid", "name": "Jose" }"""

    interceptIO[ComparisonFailException](assertIOJson(IO.pure(obtained), expected)).map { failure =>
      assert(failure.getMessage.contains("not-a-uuid"))
      assert(failure.getMessage.contains("Jose"))
    }
  }

  test("placeholders can be extended") {
    implicit val placeholders: Placeholders =
      Placeholders.default + ("<positive>", _.as[Int].exists(_ > 0))

    val expected = json"""{ "id": "<uuid>", "count": "<positive>" }"""

    val obtained = Json.obj("id" := UUID.randomUUID(), "count" := 3)

    assertIOJson(IO.pure(obtained), expected)
  }

  test("a value is read by its path") {
    assertIO(IO.pure(obtained).map(_[String]("name")), "Jose")
  }

}
