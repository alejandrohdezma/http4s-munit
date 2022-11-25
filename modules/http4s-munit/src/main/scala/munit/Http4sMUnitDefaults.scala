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

import cats.Show
import cats.effect.IO
import cats.syntax.all._

import org.http4s.ContextRequest
import org.http4s.Uri

object Http4sMUnitDefaults {

  def http4sMUnitNameCreator[A: Show](
      request: ContextRequest[IO, A],
      followingRequests: List[String],
      testOptions: TestOptions,
      config: Http4sMUnitConfig,
      replacements: Seq[(String, String)] = Nil // scalafix:ok
  ): String = {
    val clue = followingRequests.+:(testOptions.name).filter(_.nonEmpty) match {
      case Nil                 => ""
      case List(head)          => s" ($head)"
      case List(first, second) => s" ($first and then $second)"
      case list                => s"${list.init.mkString(" (", ", ", ", and then")} ${list.last})"
    }

    val context = request.context match {
      case _: Unit => None
      case context => context.show.some.filterNot(_.isEmpty())
    }

    val reps = config.repetitions match {
      case Some(rep) if rep > 1 =>
        s" - executed $rep times" + config.maxParallel.fold("")(paral => s" with $paral in parallel")
      case _ => ""
    }

    val nameWithoutReplacements = s"${request.req.method.name} -> ${Uri.decode(request.req.uri.renderString)}" +
      s"$clue${context.fold("")(" as " + _)}$reps"

    replacements.foldLeft(nameWithoutReplacements) { case (name, (value, replacement)) =>
      name.replaceAll(value, replacement)
    }
  }

}
