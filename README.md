# Integration between http4s & MUnit

[![][github-action-badge]][github-action] [![][maven-badge]][maven] [![][steward-badge]][steward]

Integration library for [MUnit](https://scalameta.org/munit/) and [http4s](https://github.com/http4s/http4s/).

## Installation

Add the following line to your `build.sbt` file:

```sbt
libraryDependencies += "com.alejandrohdezma" %% "http4s-munit" % 0.1.0 % Test)
```

## Getting Started

This library provides three different kinds of suites:

### `Http4sHttpRoutesSuite`

Provides the ability to write tests from a `Request[IO]` easily:

```scala
import cats.effect.IO

import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.syntax.all._

import munit._

class MyHttpRoutesSuite extends Http4sHttpRoutesSuite {

  override val routes: HttpRoutes[IO] = HttpRoutes.of {
    case GET -> Root / "hello"        => Ok("Hi")
    case GET -> Root / "hello" / name => Ok(s"Hi $name")
  }

  test(GET(uri"hello" / "Jose")).alias("Say hello to Jose") { response =>
    assertIO(response.as[String], "Hi Jose")
  }

}
```

### `Http4sAuthedRoutesSuite`

Provides the ability to write tests from a `AuthedRequest[IO, A]` easily:

```scala
import cats.effect.IO

import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.syntax.all._

import munit._

class MyAuthedRoutesSuite extends Http4sAuthedRoutesSuite[String] {

  override val routes: AuthedRoutes[String, IO] = AuthedRoutes.of {
    case GET -> Root / "hello" as user        => Ok(s"$user: Hi")
    case GET -> Root / "hello" / name as user => Ok(s"$user: Hi $name")
  }

  test(GET(uri"hello" / "Jose").context("alex")).alias("Say hello to Jose") { response =>
    assertIO(response.as[String], "alex: Hi Jose")
  }

}
```

### `HttpSuite`

Provides the ability to test a remote HTTP server using http4s' requests to declare the tests:

```scala
import io.circe.Json

import org.http4s._
import org.http4s.circe._
import org.http4s.client.dsl.io._
import org.http4s.syntax.all._

class GithubSuite extends HttpSuite {

  override val baseUri: Uri = uri"https://api.github.com"

  test(GET(uri"users/gutiory")) { response =>
    assertEquals(response.status.code, 200)

    val result = response.as[Json].map(_.hcursor.get[String]("login"))

    assertIO(result, Right("gutiory"))
  }

}
```

[github-action]: https://github.com/alejandrohdezma/http4s-munit/actions
[github-action-badge]: https://img.shields.io/endpoint.svg?url=https%3A%2F%2Factions-badge.atrox.dev%2Falejandrohdezma%2Fhttp4s-munit%2Fbadge%3Fref%3Dmaster&style=flat

[maven]: https://search.maven.org/search?q=g:%20com.alejandrohdezma%20AND%20a:http4s-munit
[maven-badge]: https://maven-badges.herokuapp.com/maven-central/com.alejandrohdezma/http4s-munit/badge.svg?kill_cache=1

[steward]: https://scala-steward.org
[steward-badge]: https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=
