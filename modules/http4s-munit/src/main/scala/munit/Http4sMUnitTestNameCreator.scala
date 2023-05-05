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

import org.http4s.Request
import org.http4s.Uri

/** Instances of this class provide a method to transform metadata for an `http4s-munit` test into a test name. */
@FunctionalInterface
trait Http4sMUnitTestNameCreator {

  /** Returns a valid test name for the provided metadata.
    *
    * @param request
    *   the test's request
    * @param followingRequests
    *   the following request' aliases
    * @param testOptions
    *   the options for the current test
    * @param config
    *   the configuration for this test
    * @return
    *   the test's name
    */
  def nameFor(
      request: Request[IO],
      followingRequests: List[String],
      testOptions: TestOptions,
      config: Http4sMUnitConfig
  ): String

  /** Applies the provided list of replacements to the result of `nameFor`. */
  def replacing(replacements: (String, String)*): Http4sMUnitTestNameCreator = andThen { (previous, _, _, _, _) =>
    replacements.foldLeft(previous) { case (name, (value, replacement)) =>
      name.replaceAll(value, replacement)
    }
  }

}

object Http4sMUnitTestNameCreator {

  /** Generates test names like:
    *
    * {{{
    * // GET -> users/42
    * test(GET(uri"users" / 42))
    *
    * // GET -> users (All users)
    * test(GET(uri"users")).alias("All users")
    *
    * // GET -> users as user-1
    * test(GET(uri"users").as("user-1"))
    *
    * // GET -> users - executed 10 times with 2 in parallel
    * test(GET(uri"users")).repeat(10).parallel(2)
    *
    * // GET -> users (retrieve the list of users and get the first user from the list)
    * test(GET(uri"users"))
    *     .alias("retrieve the list of users")
    *     .andThen("get the first user from the list")(_.as[List[User]].flatMap {
    *       case Nil               => fail("The list of users should not be empty")
    *       case (head: User) :: _ => GET(uri"users" / head.id.show)
    *     })
    * }}}
    */
  val default: Http4sMUnitTestNameCreator =
    (request, followingRequests, testOptions, config) => {
      val clue = followingRequests.+:(testOptions.name).filter(_.nonEmpty) match {
        case Nil                 => ""
        case List(head)          => s" ($head)"
        case List(first, second) => s" ($first and then $second)"
        case list                => s"${list.init.mkString(" (", ", ", ", and then")} ${list.last})"
      }

      val reps = config.repetitions match {
        case Some(rep) if rep > 1 =>
          s" - executed $rep times" + config.maxParallel.fold("")(paral => s" with $paral in parallel")
        case _ => ""
      }

      s"${request.method.name} -> ${Uri.decode(request.uri.renderString)}$clue$reps"
    }

}
