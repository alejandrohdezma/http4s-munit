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

import org.http4s.AuthedRoutes
import cats.effect.IO

class Http4sTestAuthedRoutesSuiteSuite extends Http4sTestAuthedRoutesSuite[String] {

  test(routes = AuthedRoutes.of[String, IO] { case GET -> Root / "hello" as user =>
    Ok(s"$user: Hi")
  })(GET(uri"/hello") -> "jose").alias("Test 1") { response =>
    assertIO(response.as[String], "jose: Hi")
  }

  test(routes = AuthedRoutes.of[String, IO] { case GET -> Root / "hello" / name as user =>
    Ok(s"$user: Hi $name")
  })(GET(uri"/hello" / "Jose").context("alex")).alias("Test 2") { response =>
    assertIO(response.as[String], "alex: Hi Jose")
  }

}
