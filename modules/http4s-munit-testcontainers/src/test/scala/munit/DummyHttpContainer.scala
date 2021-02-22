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

import com.dimafeng.testcontainers.GenericContainer
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.wait.strategy.Wait

final case class DummyHttpContainer(underlying: GenericContainer) extends GenericContainer(underlying)

object DummyHttpContainer {

  final case class Def()
      extends GenericContainer.Def[DummyHttpContainer](
        new DummyHttpContainer(
          GenericContainer(
            dockerImage = "clue/json-server",
            exposedPorts = Seq(80),
            classpathResourceMapping = Seq(("db.json", "/data/db.json", BindMode.READ_ONLY)),
            waitStrategy = Wait.forHttp("/posts")
          )
        )
      )
}
