/*
 * Copyright 2020-2024 Alejandro Hernández <https://github.com/alejandrohdezma>
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

/** Test's configuration.
  *
  * @param repetitions
  *   number of times a test must be repeated
  * @param maxParallel
  *   maximum number of repetitions to run in parallel
  * @param showAllStackTraces
  *   if all stacktraces should be printed when multiple failures occurred in a repeated test
  *
  * @author
  *   Alejandro Hernández
  * @author
  *   José Gutiérrez
  */
final case class Http4sMUnitConfig(
    repetitions: Option[Int],
    maxParallel: Option[Int],
    showAllStackTraces: Option[Boolean]
)

object Http4sMUnitConfig {

  lazy val default = Http4sMUnitConfig(
    sys.props
      .get("http4s.munit.repetitions")
      .orElse(sys.env.get("HTTP4S_MUNIT_REPETITIONS"))
      .map(_.toInt),
    sys.props
      .get("http4s.munit.max.parallel")
      .orElse(sys.env.get("HTTP4S_MUNIT_MAX_PARALLEL"))
      .map(_.toInt),
    sys.props
      .get("http4s.munit.showAllStackTraces")
      .orElse(sys.env.get("HTTP4S_SHOW_ALL_STACK_TRACES"))
      .map(_.toBoolean)
  )

}
