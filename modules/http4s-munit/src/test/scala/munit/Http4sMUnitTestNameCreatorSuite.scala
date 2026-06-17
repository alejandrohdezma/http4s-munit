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

import org.http4s.Method
import org.http4s.Request
import org.http4s.implicits._

class Http4sMUnitTestNameCreatorSuite extends FunSuite {

  val creator = Http4sMUnitTestNameCreator.default

  val request = Request[IO](Method.GET, uri"/users")

  test("eager request renders method, uri and alias") {
    val name = creator.nameFor(Some(request), Nil, TestOptions("All users"), Http4sMUnitConfig.default)
    assertEquals(name, "GET -> /users (All users)")
  }

  test("deferred request renders the alias as the name") {
    val name = creator.nameFor(None, Nil, TestOptions("Create a user"), Http4sMUnitConfig.default)
    assertEquals(name, "Create a user")
  }

  test("deferred request appends repetitions") {
    val name = creator.nameFor(None, Nil, TestOptions("Create a user"), Http4sMUnitConfig(Some(2), Some(3), None))
    assertEquals(name, "Create a user - executed 2 times with 3 in parallel")
  }

  test("deferred request without an alias fails") {
    intercept[FailException](creator.nameFor(None, Nil, TestOptions(""), Http4sMUnitConfig.default))
  }

}
