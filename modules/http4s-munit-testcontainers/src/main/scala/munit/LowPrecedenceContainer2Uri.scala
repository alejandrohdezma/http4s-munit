/*
 * Copyright 2020 Alejandro Hernández <https://github.com/alejandrohdezma>
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

import com.dimafeng.testcontainers.SingleContainer
import org.http4s.Uri
import org.testcontainers.containers.GenericContainer

@SuppressWarnings(Array("scalafix:Disable.head"))
trait LowPrecedenceContainer2Uri {

  implicit def GenericContainer2Uri[A <: SingleContainer[GenericContainer[_]]]: A => Uri = container =>
    Uri.unsafeFromString(s"http://localhost:${container.mappedPort(container.exposedPorts.head)}")

}
