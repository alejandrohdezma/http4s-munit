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
import cats.syntax.all._

import org.http4s.HttpRoutes
import org.http4s.Status

class RealWorldTest extends Http4sHttpRoutesSuite {

  override val routes: HttpRoutes[IO] = HttpRoutes.fail

  trait Database {

    def getUser(id: Int): IO[Option[String]]

  }

  def makeRoutes(database: Database): HttpRoutes[IO] = HttpRoutes.of { case GET -> Root / "user" / idStr =>
    Either.catchNonFatal(idStr.toInt).toOption match {
      case Some(id) =>
        database.getUser(id).flatMap {
          case Some(username) => Ok(username)
          case None           => NotFound("User not found")
        }
      case None => BadRequest("Id is not a number")
    }
  }

  test(GET(uri"/user/1"))
    .withRoutes(makeRoutes(_ => IO(Some("Jack"))))
    .alias("Return Ok when user exists") { response =>
      assertEquals(response.status, Status.Ok)
      assertIO(response.as[String], "Jack")
    }

  test(GET(uri"/user/1"))
    .withRoutes(makeRoutes(_ => IO(None)))
    .alias("Return NotFound when user does not exist") { response =>
      assertEquals(response.status, Status.NotFound)
      assertIO(response.as[String], "User not found")
    }

  test(GET(uri"/user/NaN"))
    .withRoutes(makeRoutes(_ => IO(fail("should not be called"))))
    .alias("Return BadRequest when user id is not a number") { response =>
      assertEquals(response.status, Status.BadRequest)
      assertIO(response.as[String], "Id is not a number")
    }

}
