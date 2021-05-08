# Integration between http4s & MUnit

[![][maven-badge]][maven] [![][steward-badge]][steward]

Integration library between [MUnit](https://scalameta.org/munit/) and [http4s](https://github.com/http4s/http4s/).

## Installation

Add the following line to your `build.sbt` file:

```sbt
libraryDependencies += "com.alejandrohdezma" %% "http4s-munit" % 0.6.1 % Test)
```

## Usage

### Testing `HttpRoutes`

We can use the `Http4sHttpRoutesSuite` to write tests for an `HttpRoutes` using `Request[IO]` values easily:

```scala
import cats.effect.IO

import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.syntax.all._

class MyHttpRoutesSuite extends munit.Http4sHttpRoutesSuite {

  override val routes: HttpRoutes[IO] = HttpRoutes.of {
    case GET -> Root / "hello"        => Ok("Hi")
    case GET -> Root / "hello" / name => Ok(s"Hi $name")
  }

  test(GET(uri"hello" / "Jose")).alias("Say hello to Jose") { response =>
    assertIO(response.as[String], "Hi Jose")
  }

}
```

The `test` method receives a `Request[IO]` object and when the test runs, it runs that request against the provided routes and let you assert the response.

`http4s-munit` will automatically name your tests using the information of the provided `Request`. For example, for the test shown in the previous code snippet, the following will be seing when running the test:

```
munit.MyHttpRoutesSuite:0s
  + GET -> hello/Jose (Say hello to Jose) 0.014s
```

### Testing `AuthedRoutes`

If we want to test authenticated routes (`AuthedRoutes` in http4s) we can use the `Http4sAuthedRoutesSuite`. It is completely similar to the previous suite, except that we need to ensure a `Show` instance is available for the auth "context" type and that we need to provide `AuthedRequest` instead of `Request` in the test definition. We can do this using its own constructor or by using our extension function `context` or `->`:

```scala
import cats.effect.IO

import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.syntax.all._

class MyAuthedRoutesSuite extends munit.Http4sAuthedRoutesSuite[String] {

  override val routes: AuthedRoutes[String, IO] = AuthedRoutes.of {
    case GET -> Root / "hello" as user        => Ok(s"$user: Hi")
    case GET -> Root / "hello" / name as user => Ok(s"$user: Hi $name")
  }

  test(GET(uri"hello" / "Jose").context("alex")).alias("Say hello to Jose") { response =>
    assertIO(response.as[String], "alex: Hi Jose")
  }

}
```

### Testing a remote HTTP server

In the case you don't want to use static http4s routes, but a running HTTP server you have available the `HttpSuite`. This suite behaves exactly the same as the previous ones except that you don't provide a `routes` object, but a `baseUri` with the URI of your HTTP server. Any `Request` added in tests will prepend
this URI before making a call using a real http4s `Client`.

```scala
import io.circe.Json

import org.http4s._
import org.http4s.circe._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.syntax.all._

class GitHubSuite extends munit.HttpSuite {

  override val baseUri: Uri = uri"https://api.github.com"

  test(GET(uri"users/gutiory")) { response =>
    assertEquals(response.status.code, 200)

    val result = response.as[Json].map(_.hcursor.get[String]("login"))

    assertIO(result, Right("gutiory"))
  }

}
```

If you want to customize the http4s' `Client` used to make the calls you can override `http4sMUnitClient`, for example to provide a different implementation or initialize it in any way out of the default one (different timeouts, SSL certificates...).

### Testing an HTTP server running inside a container

The last of our suites can be used when you want to test a "live" container inside a [test-containers](https://github.com/testcontainers/testcontainers-scala) container. This suite lives in a different artifact, so if you want to use it, you'll need to add the following to your `build.sbt`:

```sbt
libraryDependencies += "com.alejandrohdezma" %% "http4s-munit-testcontainers" % 0.6.1 % Test)
```

It is similar to the previous suite (in fact it extends from it) but instead of a base URI we provide a container definition:


```scala
import org.http4s.dsl.io._
import org.http4s.client.dsl.io._
import org.http4s.syntax.all._

import com.dimafeng.testcontainers.munit.TestContainerForAll

class DummyHttpContainerSuite extends munit.HttpFromContainerSuite with TestContainerForAll {

  // A dummy container definition using "briceburg/ping-pong" image
  override val containerDef = DummyHttpContainer.Def()

  test(GET(uri"ping")) { response =>
    assertEquals(response.status.code, 200)

    assertIO(response.as[String], "pong")
  }

}
```

As you can see in order to use this suite you'll need to select also one of the two [test-containers](https://github.com/testcontainers/testcontainers-scala) specific suites: `TestContainersForAll` or `TestContainersForEach`. Lastly you'll need to ensure your container's URI is obtainable either by using the default extractor (which just uses `localhost:first-exposed-port`) or providing an specific one for your container by overriding the `http4sMUnitContainerUriExtractors` list:

```scala
override def http4sMUnitContainerUriExtractors: List[ContainerUriExtractor] =
  super.http4sMUnitContainerUriExtractors ++
    List(new ContainerUriExtractor({ case _: DummyHttpContainer => uri"http://localhost:80" }))
```

## Other features

### Tagging your tests

Once the request has been passed to the `test` method, we can tag our tests before implementing them:


```scala
// Marks the test as failing (it will pass if the assertions fails)
test(GET(uri"hello")).fail { response => assertEquals(response.status.code, 200) }

// Marks a test as "flaky". Check MUnit docs to know more about this feature:
// https://scalameta.org/munit/docs/tests.html#tag-flaky-tests
test(GET(uri"hello")).flaky { response => assertEquals(response.status.code, 200) }

// Skips this test when running the suite
test(GET(uri"hello")).ignore { response => assertEquals(response.status.code, 200) }

// Runs only this test when running the suite
test(GET(uri"hello")).only { response => assertEquals(response.status.code, 200) }

// We can also use our own tags, just like with MUnit `test`
val IntegrationTest = new munit.Tag("integration-test")
test(GET(uri"hello")).tag(IntegrationTest) { response => assertEquals(response.status.code, 200) }
```

[maven]: https://search.maven.org/search?q=g:%20com.alejandrohdezma%20AND%20a:http4s-munit_2.13
[maven-badge]: https://maven-badges.herokuapp.com/maven-central/com.alejandrohdezma/http4s-munit_2.13/badge.svg?kill_cache=1
[steward]: https://scala-steward.org
[steward-badge]: https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=
