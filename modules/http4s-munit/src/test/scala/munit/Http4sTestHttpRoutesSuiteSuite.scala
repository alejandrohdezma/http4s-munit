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
import org.http4s.HttpRoutes
import org.http4s.Status
import cats.implicits._

class Http4sTestHttpRoutesSuiteSuite extends Http4sTestHttpRoutesSuite {

  test(routes = HttpRoutes.of { case GET -> Root / "hello" =>
    Ok("Hi")
  })(GET(uri"/hello")).alias("Test 1") { response =>
    assertIO(response.as[String], "Hi")
  }

  test(
    routes = {
      HttpRoutes.of { case GET -> Root / "hello" / name =>
        Ok(s"Hi $name")
      }
    }
  )(GET(uri"/hello" / "Jose")).alias("Test 2") { response =>
    assertIO(response.as[String], "Hi Jose")
  }

  // Real world test

  trait Database {

    def getUser(id: Int): IO[Option[String]]

  }

  def makeRoutes(database: Database): HttpRoutes[IO] = {
    HttpRoutes.of { case GET -> Root / "user" / idStr =>
      idStr.toIntOption match {
        case Some(id) =>
          database.getUser(id).flatMap {
            case Some(username) => Ok(username)
            case None           => NotFound("User not found")
          }
        case None => BadRequest("Id is not a number")
      }
    }
  }

  test(makeRoutes(new Database {

    override def getUser(id: Int): IO[Option[String]] = IO(Some("Jack"))

  }))(GET(uri"/user/1")).alias("Return Ok when user exists") { response =>
    assertEquals(response.status, Status.Ok)
    assertIO(response.as[String], "Jack")
  }

  test(makeRoutes(new Database {

    override def getUser(id: Int): IO[Option[String]] = IO(None)

  }))(GET(uri"/user/1")).alias("Return NotFound when user does not exist") { response =>
    assertEquals(response.status, Status.NotFound)
    assertIO(response.as[String], "User not found")
  }

  test(makeRoutes(new Database {

    override def getUser(id: Int): IO[Option[String]] = IO(fail("should not be called"))

  }))(GET(uri"/user/NaN")).alias("Return BadRequest when user id is not a number") { response =>
    assertEquals(response.status, Status.BadRequest)
    assertIO(response.as[String], "Id is not a number")
  }

}
