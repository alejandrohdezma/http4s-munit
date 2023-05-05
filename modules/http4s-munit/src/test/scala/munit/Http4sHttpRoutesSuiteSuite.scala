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

import cats.effect.IO

import org.http4s.HttpRoutes

class Http4sHttpRoutesSuiteSuite extends Http4sSuite {

  override def http4sMUnitClientFixture = HttpRoutes
    .of[IO] {
      case GET -> Root / "hello"        => Ok("Hi")
      case GET -> Root / "hello" / name => Ok(s"Hi $name")
    }
    .orFail
    .asFixture

  test(GET(uri"/hello")).alias("Test 1") { response =>
    assertIO(response.as[String], "Hi")
  }

  test(GET(uri"/hello" / "Jose")).alias("Test 2") { response =>
    assertIO(response.as[String], "Hi Jose")
  }

  test(GET(uri"/hello"))
    .withRoutes(HttpRoutes.of[IO] { case GET -> Root / "hello" => Ok("Hey") })
    .alias("Test 1 (overriding routes)") { response =>
      assertIO(response.as[String], "Hey")
    }

  test(GET(uri"/hello" / "Jose"))
    .withRoutes(HttpRoutes.of[IO] { case GET -> Root / "hello" / name => Ok(s"Hey $name") })
    .alias("Test 2 (overriding routes)") { response =>
      assertIO(response.as[String], "Hey Jose")
    }

}
