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

import com.dimafeng.testcontainers.SingleContainer
import com.dimafeng.testcontainers.munit.TestContainersSuite
import org.http4s.Uri

/** Base class for suites testing HTTP servers running in docker containers using testcontainers.
  *
  * To use this class you'll need to select also one of the two testcontainers specific suites: `TestContainersForAll`
  * or `TestContainersForEach`. Also you'll need to override the `val containerDef: ContainerDef` definition with your
  * container. Lastly you'll need to ensure your container's URI is obtainable either by using the default extractor
  * (which just uses `localhost:first-exposed-port`) or providing an specific one for your container by overriding the
  * `http4sMUnitContainerUriExtractor` list.
  *
  * @example
  *   {{{
  * import scala.concurrent.ExecutionContext.global
  *
  * import cats.effect.IO
  * import cats.effect.Resource
  *
  * import com.dimafeng.testcontainers.ContainerDef
  * import com.dimafeng.testcontainers.GenericContainer
  * import com.dimafeng.testcontainers.munit.TestContainerForAll
  *
  * import org.http4s.client.Client
  * import org.http4s.ember.client.EmberClientBuilder
  *
  * import org.testcontainers.containers.wait.strategy.Wait
  *
  * class HttpFromContainerSuiteSuite extends munit.HttpFromContainerSuite with TestContainerForAll {
  *
  *   override def http4sMUnitClient: Resource[IO, Client[IO]] = BlazeClientBuilder[IO](global).resource
  *
  *   override val containerDef = new ContainerDef {
  *
  *     override type Container = GenericContainer
  *
  *     protected def createContainer(): GenericContainer = GenericContainer(
  *       dockerImage = "briceburg/ping-pong",
  *       exposedPorts = Seq(80)
  *     )
  *
  *   }
  *
  *   test(GET(uri"ping")) { response =>
  *     assertEquals(response.status.code, 200)
  *
  *     assertIO(response.as[String], "pong")
  *   }
  *
  * }
  *   }}}
  *
  * @author
  *   Alejandro Hernández
  * @author
  *   José Gutiérrez
  */
@deprecated("Use HttpSuite + fixtures instead", "http4s-munit-testcontainers 0.10.0")
abstract class HttpFromContainerSuite extends HttpSuite with TestContainersSuite {

  override def baseUri(): Uri = withContainers { (containers: Containers) =>
    http4sMUnitContainerUriExtractor
      .lift(containers)
      .getOrElse(fail("Unable to get container's URI"))
  }

  /** This list contains ways to get the container's URI. The first succesfull URI that this list creates will be used
    * as the test's base URI.
    *
    * By default it will only match `SingleContainer` by setting the URI to localhost with the container's first mapped
    * port.
    *
    * If you want to add support for other containers you can add a new value to this list or override it completely:
    *
    * {{{
    * override def http4sMUnitContainerUriExtractor: PartialFunction[Containers, Uri] =
    *   super.http4sMUnitContainerUriExtractor orElse { case c: MyContainer[_] => c.uri }
    * }}}
    */
  def http4sMUnitContainerUriExtractor: PartialFunction[Containers, Uri] = {
    case c: SingleContainer[_] if c.exposedPorts.nonEmpty =>
      Uri.unsafeFromString(s"http://localhost:${c.mappedPort(c.exposedPorts.head)}")
  }

}
