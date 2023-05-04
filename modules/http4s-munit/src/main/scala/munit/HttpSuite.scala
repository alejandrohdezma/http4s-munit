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

import cats.effect.IO
import cats.effect.Resource
import cats.effect.SyncIO

import org.http4s.Request
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

/** Base class for suites testing remote HTTP servers.
  *
  * You can provide the `Uri` of the remote server by overriding `baseUri`. If you provide one this URI will be used as
  * base for every test.
  *
  * By default this class uses `Ember` as the client to connect to the server. If you don't want to use this specific
  * client you will need to override the `http4sMUnitClient` value.
  *
  * @example
  *   {{{
  * import scala.concurrent.ExecutionContext.global
  *
  * import cats.effect.IO
  * import cats.effect.Resource
  *
  * import io.circe.Json
  * import org.http4s.Uri
  * import org.http4s.circe._
  * import org.http4s.client.Client
  * import org.http4s.ember.client.EmberClientBuilder
  *
  * class HttpSuiteSuite extends munit.HttpSuite {
  *
  *   override def http4sMUnitClient: Resource[IO, Client[IO]] = BlazeClientBuilder[IO](global).resource
  *
  *   override def baseUri(): Uri = uri"https://api.github.com"
  *
  *   test(GET(uri"users/gutiory")) { response =>
  *     assertEquals(response.status.code, 200)
  *
  *     val result = response.as[Json].map(_.hcursor.get[String]("login"))
  *
  *     assertIO(result, Right("gutiory"))
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
trait HttpSuite extends Http4sSuite with CatsEffectFunFixtures {

  /** The base URI for all tests. This URI will prepend the one used in each test's request. */
  def baseUri(): Uri = Uri()

  /** This client is used under the hood to execute the requests. */
  def http4sMUnitClient: Resource[IO, Client[IO]] = try
    EmberClientBuilder.default[IO].build
  catch {
    case _: NoClassDefFoundError =>
      throw new IllegalAccessException(
        "To use `HttpSuite` you either need to add `http4s-ember-client` dependency or provide your own " +
          "client implementation by overriding `http4sMUnitClient`."
      ) // scalafix:ok
  }

  /** @inheritdoc */
  @SuppressWarnings(Array("scalafix:DisableSyntax.=="))
  override def http4sMUnitClientFixture: SyncIO[FunFixture[Client[IO]]] = ResourceFunFixture {
    http4sMUnitClient.map { client =>
      if (baseUri() == Uri()) client
      else Client((request: Request[IO]) => client.run(request.withUri(baseUri().resolve(request.uri))))
    }
  }

}
