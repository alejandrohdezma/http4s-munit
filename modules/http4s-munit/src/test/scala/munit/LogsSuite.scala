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

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

import cats.effect.IO
import cats.syntax.all._

import sbt.testing.EventHandler
import sbt.testing.TaskDef

import org.http4s.HttpApp
import org.http4s.Response

/** This suite ensures that the logs outputed by this library are correct.
  *
  * For this we create some dummy suites in the suite's companion object (so they are not launched), execute them in a
  * controlled runner, get its output, and compare it against an expected one.
  */
class LogsSuite extends FunSuite {

  override def munitIgnore: Boolean = !scala.util.Properties.versionNumberString.startsWith("2.13")

  private val thisFile = s"${sys.props("user.dir")}/src/test/scala/munit/LogsSuite.scala"

  test("test names are correctly generated from an Http4sMUnitTestCreator") {
    val obtained = execute[LogsSuite.SimpleSuite]

    val expected =
      """|==> Success GET -> posts
         |==> Success GET -> posts/1
         |==> Success GET -> posts/2 (get second post)
         |==> Success GET -> posts/3 - executed 12 times with 5 in parallel
         |==> Success GET -> posts/1 (get first post)
         |==> Success GET -> posts/1 (get first post and then second post)
         |==> Success GET -> posts/1 (get 1st post and 2nd secuentially)
         |==> Success GET -> posts/1 (get first and second posts secuentially)
         |""".stripMargin

    assertNoDiff(obtained, expected)
  }

  test("test body is correctly outputed as a 'clue'") {
    val obtained = execute[LogsSuite.JsonSuite]

    val expected =
      s"""|==> Failure GET -> posts at $thisFile
          |    test(GET(uri"posts"))(r => assertEquals(r.status.code, 204))
          |values are not the same
          |=> Obtained
          |200
          |=> Diff (- obtained, + expected)
          |-200
          |+204
          |
          |Response body was:
          |
          |{
          |  "id" : 1,
          |  "name" : "Jose"
          |}
          |
          |==> Failure GET -> posts-1 at $thisFile
          |    test(GET(uri"posts"))(r => assertEquals(r.status.code, 204, clues(r.headers)))
          |Clues {
          |  response.bodyText.compile.string: String = '''
          |    {
          |      "id" : 1,
          |      "name" : "Jose"
          |    }
          |  ''',
          |  r.headers: Headers = Headers(Content-Type: text/plain; charset=UTF-8, Content-Length: 25)
          |}
          |=> Obtained
          |200
          |=> Diff (- obtained, + expected)
          |-200
          |+204
          |""".stripMargin

    assertNoDiff(obtained, expected)
  }

  private def execute[T](implicit classTag: ClassTag[T]): String = {
    val framework = new Framework

    val runner = framework.runner(Array("+l"), Array(), this.getClass().getClassLoader())

    val taskDef = new TaskDef(classTag.runtimeClass.getName(), framework.munitFingerprint, false, Array())

    val tasks = runner.tasks(Array(taskDef))

    val buffer = ListBuffer.empty[String]

    val eventHandler: EventHandler = event => {
      buffer += "==> "
      buffer += event.status().name()
      buffer += " "
      buffer += event.fullyQualifiedName()
      buffer += (if (event.throwable().isDefined()) s" at ${event.throwable().get().getMessage()}" else "")
      buffer += "\n"
    }

    tasks.foreach(_.execute(eventHandler, Array()))

    buffer.mkString
      .replace("\"\"\"", "'''")
      .replaceAll("\\.scala:\\d+", ".scala")
      .replaceAll("""\d+:\n""", "")
      .replaceAll("""\d+:(.*)\n""", "$1\n")
      .replace(classTag.runtimeClass.getName() + ".", "")
  }

}

object LogsSuite {

  class SimpleSuite extends Http4sSuite {

    override def http4sMUnitClientFixture = HttpApp.pure[IO](Response()).asFixture

    test(GET(uri"posts"))(_ => ())

    test(GET(uri"posts" / "1"))(_ => ())

    test(GET(uri"posts" / "2")).alias("get second post")(_ => ())

    test(GET(uri"posts" / "3")).repeat(12).parallel()(_ => ())

    test(GET(uri"posts" / "1")).alias("get first post")(_ => ())

    test(GET(uri"posts" / "1"))
      .alias("get first post")
      .andThen("second post")(_ => GET(uri"posts" / "2").pure[IO])(_ => ())

    test(GET(uri"posts" / "1"))
      .alias("get 1st post and 2nd secuentially")
      .andThen(_ => GET(uri"posts" / "2").pure[IO])(_ => ())

    test(GET(uri"posts" / "1"))
      .andThen("get first and second posts secuentially")(_ => GET(uri"posts" / "2").pure[IO])(_ => ())

  }

  class JsonSuite extends Http4sSuite {

    override def http4sMUnitClientFixture = HttpApp.liftF[IO](Ok("""{"id": 1, "name": "Jose"}""")).asFixture

    test(GET(uri"posts"))(r => assertEquals(r.status.code, 204))

    test(GET(uri"posts"))(r => assertEquals(r.status.code, 204, clues(r.headers)))

  }

}
