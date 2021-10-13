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

import cats.effect.IO
import cats.effect.SyncIO
import cats.effect.kernel.Resource

import org.http4s.Header
import org.http4s.Response
import org.typelevel.ci._

class HeaderInterpolatorSuite extends Http4sSuite[String] {

  override def http4sMUnitNameCreator(
      request: String,
      followingRequests: List[String],
      testOptions: TestOptions,
      config: Http4sMUnitConfig
  ): String = fail("This should no be called")

  override def http4sMUnitFunFixture: SyncIO[FunFixture[String => Resource[IO, Response[IO]]]] =
    fail("This should no be called")

  test("header interpolator creates a valid raw header") {
    val header = ci"my-header" := "my-value"

    assertEquals(header, Header.Raw(ci"my-header", "my-value"))
  }

}
