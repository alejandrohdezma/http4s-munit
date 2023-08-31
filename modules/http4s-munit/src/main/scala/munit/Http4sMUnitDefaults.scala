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

object Http4sMUnitDefaults {

  @deprecated("Use Http4sMUnitTestNameCreator.default instead", since = "0.16.0")
  def http4sMUnitNameCreator(
      request: Request[IO],
      followingRequests: List[String],
      testOptions: TestOptions,
      config: Http4sMUnitConfig,
      replacements: Seq[(String, String)] = Nil // scalafix:ok
  ): String = Http4sMUnitTestNameCreator.default
    .replacing(replacements: _*)
    .nameFor(request, followingRequests, testOptions, config)

}
