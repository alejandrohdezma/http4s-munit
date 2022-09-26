package munit

import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all._

import com.dimafeng.testcontainers.GenericContainer
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.ci._

class TestContainersSuite extends munit.HttpSuite {

  override def http4sMUnitClient =
    Resource.fromAutoCloseable(IO(container.start()).as(container)) >> EmberClientBuilder.default[IO].build

  override def http4sMUnitResponseClueCreator(response: Response[IO]) = {
    val logs = response.headers
      .get(ci"x-request-id")
      .map(_.head.value)
      .map(id => container.logs.split("\n").filter(_.contains(id)).mkString("\n"))
      .getOrElse(container.logs)

    clues(response, logs)
  }

  lazy val container = GenericContainer(dockerImage = "mendhak/http-https-echo", exposedPorts = List(80))

  override def baseUri() = Uri.unsafeFromString(s"http://localhost:${container.mappedPort(80)}")

  test(GET(uri"ping")) { response =>
    assertEquals(response.status.code, 200, response.clues)
    assertIOBoolean(response.as[Json].map(_.isObject), response.clues)
  }

}
