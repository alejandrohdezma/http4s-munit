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

import io.circe.Json
import org.http4s.circe._

class HttpSuiteNoBaseUriSuite extends HttpSuite {

  test(GET(uri"https://api.github.com" / "users" / "gutiory")) { response =>
    assertEquals(response.status.code, 200, response.clues)

    val result = response.as[Json].map(_.hcursor.get[String]("login"))

    assertIO(result, Right("gutiory"), response.clues)
  }

  test(GET(uri"https://api.github.com" / "users" / "alejandrohdezma")) { response =>
    assertEquals(response.status.code, 200, response.clues)

    val result = response.as[Json].map(_.hcursor.get[String]("login"))

    assertIO(result, Right("alejandrohdezma"), response.clues)
  }

}
