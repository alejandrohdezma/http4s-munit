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

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import cats.syntax.option._

final case class Http4sMunitConfig(repetitions: Option[Int], maxParallel: Option[Int])

object Http4sMunitConfig {

  lazy val default = Http4sMunitConfig(
    sys.props
      .get("http4s.munit.repetitions")
      .map(toIntOption)
      .flatten
      .orElse(sys.env.get("HTTP4S_MUNIT_REPETITIONS").map(toIntOption).flatten),
    sys.props
      .get("http4s.munit.maxParallel")
      .map(toIntOption)
      .flatten
      .orElse(sys.env.get("HTTP4S_MAX_CONCURRENT").map(toIntOption).flatten)
  )

  private def toIntOption(intStr: String): Option[Int] = Try(intStr.toInt) match {
    case Success(value) => value.some
    case Failure(_)     => None
  }
}
