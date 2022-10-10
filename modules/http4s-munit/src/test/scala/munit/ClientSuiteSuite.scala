package munit

import cats.effect._

import org.http4s.client.Client

class ClientSuiteSuite extends ClientSuite {

  class PingService[F[_]: Async](client: Client[F]) {

    def ping(): F[String] = client.expect[String]("ping")

  }

  val fixture = Client.fixture(client => Resource.pure(new PingService[IO](client)))

  fixture { case GET -> Root / "ping" =>
    Ok("pong")
  }.test("PingService.ping works") { service =>
    val result = service.ping()

    assertIO(result, "pong")
  }

}
