# When http4s met MUnit

Integration library between [MUnit](https://scalameta.org/munit/) and [http4s](https://github.com/http4s/http4s/).

---

- [Installation](#installation)
- [Contributors to this project](#contributors-to-this-project)
- [Usage](#usage)
  - [Testing `HttpRoutes`](#testing-httproutes)
  - [Testing `AuthedRoutes`](#testing-authedroutes)
  - [Using a mocked http4s `Client`](#using-a-mocked-http4s-client)
  - [Testing a remote HTTP server](#testing-a-remote-http-server)
  - [Testing an HTTP server running inside a container](#testing-an-http-server-running-inside-a-container)
- [Other features](#other-features)
  - [Running an effect before running your test](#running-an-effect-before-running-your-test)
  - [Tagging your tests](#tagging-your-tests)
  - [Stress-testing](#stress-testing)
  - [Nested requests](#nested-requests)
  - [Test names](#test-names)
  - [Body in failed assertions](#body-in-failed-assertions)
  - [Response clues](#response-clues)

## Installation

Add the following line to your `build.sbt` file:

```sbt
libraryDependencies += "com.alejandrohdezma" %% "http4s-munit" % "1.1.0" % Test
```

## Contributors to this project

| <a href="https://github.com/alejandrohdezma"><img alt="alejandrohdezma" src="https://avatars.githubusercontent.com/u/9027541?v=4&s=120" width="120px" /></a> | <a href="https://github.com/gutiory"><img alt="gutiory" src="https://avatars.githubusercontent.com/u/3316502?v=4&s=120" width="120px" /></a> | <a href="https://github.com/JackTreble"><img alt="JackTreble" src="https://avatars.githubusercontent.com/u/4872989?v=4&s=120" width="120px" /></a> |
| :--: | :--: | :--: |
| <a href="https://github.com/alejandrohdezma"><sub><b>alejandrohdezma</b></sub></a> | <a href="https://github.com/gutiory"><sub><b>gutiory</b></sub></a> | <a href="https://github.com/JackTreble"><sub><b>JackTreble</b></sub></a> |

## Usage

This library provides a new type of suite (`Http4sSuite`) that you can use for
several things:

### Testing `HttpRoutes`

We can use the `Http4Suite` to write tests for an `HttpRoutes` using `Request[IO]` values easily:

```scala
import cats.effect.IO

import org.http4s._

class MyHttpRoutesSuite extends munit.Http4sSuite {

  override def http4sMUnitClientFixture = HttpRoutes.of[IO] {
    case GET -> Root / "hello"        => Ok("Hi")
    case GET -> Root / "hello" / name => Ok(s"Hi $name")
  }.orFail.asFixture

  test(GET(uri"hello" / "Jose")).alias("Say hello to Jose") { response =>
    assertIO(response.as[String], "Hi Jose")
  }

  // You can also override routes per-test
  test(GET(uri"hello" / "Jose"))
    .withHttpApp(HttpRoutes.of[IO] { case GET -> Root / "hello" / _=> Ok("Hi") }.orFail)
    .alias("Overriden routes") { response =>
      assertIO(response.as[String], "Hi")
    }

}
```

The `test` method receives a `Request[IO]` object and when the test runs, it runs that request against the provided routes and let you assert the response.

`http4s-munit` will automatically name your tests using the information of the provided `Request`. For example, for the test shown in the previous code snippet, the following will be shown when running the test:

```
munit.MyHttpRoutesSuite:0s
  + GET -> hello/Jose (Say hello to Jose) 0.014s
```

### Testing `AuthedRoutes`

If we want to test authenticated routes (`AuthedRoutes` in http4s) it will be
completely similar to the previous section, except that we need to ensure we
provide the context in the request. The library provides a couple methods to
simplify this: `context` and `getContext`.

For both of them you need to have an implicit `Key[A]` instance (being `A`
your context's type) in scope.

```scala
import cats.effect.IO

import org.http4s._
import org.typelevel.vault.Key

class MyAuthedRoutesSuite extends munit.Http4sSuite {

  implicit val key: Key[String] = Key.newKey[IO, String].unsafeRunSync()

  override def http4sMUnitClientFixture = AuthedRequest.fromContext[String].andThen {
    AuthedRoutes.of[String, IO] {
      case GET -> Root / "hello" as user        => Ok(s"$user: Hi")
      case GET -> Root / "hello" / name as user => Ok(s"$user: Hi $name")
    }
  }.orFail.asFixture

  test(GET(uri"hello" / "Jose").context("alex")).alias("Say hello to Jose") { response =>
    assertIO(response.as[String], "alex: Hi Jose")
  }

  // You can also override routes per-test
  test(GET(uri"hello" / "Jose").context("alex"))
    .withHttpApp {
      AuthedRequest.fromContext[String]
        .andThen(AuthedRoutes.of[String, IO] { case GET -> Root / "hello" / _ as _ => Ok("Hey") })
        .orFail
    }
    .alias("Overriden routes") { response =>
      assertIO(response.as[String], "Hey")
    }

}
```

### Using a mocked http4s `Client`

If you just want to add tests for a class or algebra that uses a `Client` instance you can make your suite extend `Http4sMUnitSyntax` (it also requires extending `CatsEffectSuite`).

It includes a handful of utilities among which are two extension methods to the `Client` companion object: `from` and `partialFixture`.

`Client.from` lets you create a mocked client from a partial function representing routes:

```scala
import org.http4s.client.Client

class ClientSuiteSuite extends munit.CatsEffectSuite with munit.Http4sMUnitSyntax {

  val client = Client.from {
    case GET -> Root / "ping" => Ok("pong")
  }

}
```

On the other hand, the class also provides another extension method: `Client.partialFixture`. This method is inteded to be used to easily create a fixture for testing a class that uses an http4s' `Client`.

Given an algebra like:

```scala
import cats.effect._
import org.http4s.client.Client

trait PingService[F[_]] {

  def ping(): F[String]

}

object PingService {

  def create[F[_]: Async](client: Client[F]) =
    new PingService[F] {

      def ping(): F[String] = client.expect[String]("ping")

    }
  

}
```

You can test it using `Http4sMUnitSyntax` like:

```scala
import cats.effect._
import org.http4s.client.Client

class PingServiceSuite extends munit.CatsEffectSuite with munit.Http4sMUnitSyntax {

  val fixture = Client.partialFixture(client => Resource.pure(PingService.create(client)))

  fixture {
    case GET -> Root / "ping" => Ok("pong")
  }.test("PingService.ping works") { service =>
    val result = service.ping()

    assertIO(result, "pong")
  }

}
```

### Testing a remote HTTP server

In the case you don't want to use static http4s routes, but a running HTTP server,
you just need to provide a real http4s' `Client` implementation under `http4sMUnitClient`.
Every test request you write will be made using this client.

```scala
import cats.effect.IO
import cats.effect.SyncIO

import io.circe.Json
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

class GitHubSuite extends munit.Http4sSuite {

  override def http4sMUnitClientFixture: SyncIO[FunFixture[Client[IO]]] =
    ResourceFunFixture(EmberClientBuilder.default[IO].build.map(_.withBaseUri(uri"https://api.github.com")))

  test(GET(uri"users/gutiory")) { response =>
    assertEquals(response.status.code, 200)

    val result = response.as[Json].map(_.hcursor.get[String]("login"))

    assertIO(result, Right("gutiory"))
  }

}
```

> If you are making requests to the same server, you can override `http4sMUnitClientFixture` like:
>
> ```scala
> override def http4sMUnitClientFixture: SyncIO[FunFixture[Client[IO]]] =
>   ResourceFunFixture(EmberClientBuilder.default[IO].build.map(_.withBaseUri(localhost.withPort(8080))))
> ```

### Testing an HTTP server running inside a container

Testing a Docker container with TestContainers and `http4s-munit` is easy. You
just need to use `TestCotnainersFixtures` and use `Http4sSuite` to connect to
it:

```scala
import cats.effect.IO
import cats.effect.SyncIO

import com.dimafeng.testcontainers.GenericContainer
import com.dimafeng.testcontainers.munit.fixtures.TestContainersFixtures
import io.circe.Json
import org.http4s.client.Client
import org.http4s.circe._
import org.http4s.ember.client.EmberClientBuilder

class TestContainersSuite extends munit.Http4sSuite with TestContainersFixtures {

  // There is also available `ForEachContainerFixture`
  val container = ForAllContainerFixture {
    GenericContainer(dockerImage = "mendhak/http-https-echo", exposedPorts = List(80))
  }

  override def munitFixtures = List(container)

  override def http4sMUnitClientFixture: SyncIO[FunFixture[Client[IO]]] = ResourceFunFixture {
    EmberClientBuilder.default[IO].build.map(_.withBaseUri(localhost.withPort(container().mappedPort(80))))
  }

  test(GET(uri"ping")) { response =>
    assertEquals(response.status.code, 200)
    assertIOBoolean(response.as[Json].map(_.isObject))
  }

}
```

Or if you don't want to use container fixtures and you don't mind starting a container for each test:

```scala
import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all._

import com.dimafeng.testcontainers.GenericContainer
import org.http4s.ember.client.EmberClientBuilder

class TestContainersSuite extends munit.Http4sSuite {

  lazy val container = GenericContainer(dockerImage = "nginxdemos/hello", exposedPorts = List(80))

  override def http4sMUnitClientFixture = ResourceFunFixture {
    Resource.fromAutoCloseable(IO(container.start()).as(container)) >>
      EmberClientBuilder.default[IO].build.map(_.withBaseUri(localhost.withPort(container.mappedPort(80))))
  }

  test(GET(uri"ping")) { response =>
    assertEquals(response.status.code, 200, response.clues)
  }

}
```

## Other features

### Running an effect before running your test

Sometimes (specially when you are testing against a real server) you need something to be
run before running your test. On these cases, you can just create a
`ResourceFunFixture[Client[IO]]` (in which you can add other effects) and run it with `test`.

Essentially this is the same as just running `test` since it is just an alias for
`http4sMUnitClientFixture.test`.

```scala
import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all._

import io.circe.Json
import io.circe.syntax._
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.circe._

class MyBookstoreSuite extends munit.Http4sSuite {

  def httpClient = EmberClientBuilder.default[IO].build

  override def http4sMUnitClientFixture = ResourceFunFixture(httpClient)

  ResourceFunFixture {
    httpClient.flatTap { client =>
      Resource.make {
        val newBook = Json.obj("name":= "The Lord Of The Rings")

        client
          .expect[Json](POST(newBook, uri"http://localhost:8080/books"))
          .flatMap(_.hcursor.get[Int]("id").liftTo[IO])
      } { id =>
        client.run(DELETE(uri"http://localhost:8080/books" / id)).use_
      }.as(client)
    }
  }.test(GET(uri"http://localhost:8080/books?q=Rings")) { response =>
    assertEquals(response.status.code, 200, response.clues)

    val result = response.as[Json].map(_.hcursor.get[String]("name"))

    assertIO(result, Right("The Lord Of The Rings"), response.clues)
  }
}
```

### Tagging your tests

Once the request has been passed to the `test` method, we can tag our tests before implementing them:


```scala
// Marks the test as failing (it will pass if the assertion fails)
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

### Stress-testing

`http4s-munit` includes a small feature that allows you to "stress-test" a service. Once the request has been passed to the `test` method, we can call several methods to enable test repetition and parallelization:

```scala
test(GET(uri"hello"))
  .repeat(50)
  .parallel(10) { response => 
    assertEquals(response.status.code, 200) 
  }
```

On the other hand, if you do not want to have to call these methods for each test, you also have the possibility to enable repetition and parallelization using system properties or environment variables:

- Using environment variables:

  ```bash
  export HTTP4S_MUNIT_REPETITIONS=50
  export HTTP4S_MUNIT_MAX_PARALLEL=10

  sbt test
  ```

- Using system properties:

  ```bash
  sbt -Dhttp4s.munit.repetitions=50 -Dhttp4s.munit.max.parallel=10 test
  ```

Also, when multiple errors occured while running repeated tests, you can control wheter `http4s-munit` should output all failures or not using:

```bash
# Using environment variable
export HTTP4S_SHOW_ALL_STACK_TRACES=true

# Using system property
sbt -Dhttp4s.munit.showAllStackTraces=true test
```

Finally, if you want to disable repetitions for a specific test when using environment variables or system properties, you can use `doNotRepeat`:

```scala
test(GET(uri"hello")).doNotRepeat { response => 
  assertEquals(response.status.code, 200) 
}
```

### Nested requests

Sometimes one test needs some pre-condition in order to be executed (e.g., in order to test the deletion of a user, you need to create it first). In such cases, once the request has been passed to the `test` method, we can call `andThen` to provide nested requests from the response of the previous one:

```scala
test(GET(uri"posts" +? ("number" -> 10)))
    .alias("look for the 10th post")
    .andThen("delete it")(_.as[String].map { id =>
      DELETE(uri"posts" / id)
    }) { response =>
      assertEquals(response.status.code, 204)
    }
```

### Test names

The generated test names can be customized by overriding `http4sMUnitTestNameCreator`. It allows altering the name of the generated tests.

Default implementation generates test names like:

```scala
// GET -> users/42
test(GET(uri"users" / "42"))

// GET -> users (all users)
test(GET(uri"users")).alias("all users")

// GET -> users - executed 10 times with 2 in parallel
test(GET(uri"users")).repeat(10).parallel(2)

// GET -> posts?number=10 (look for the 10th post and delete it)
test(GET(uri"posts" +? ("number" -> 10)))
    .alias("look for the 10th post")
    .andThen("delete it")(_.as[String].map { id => DELETE(uri"posts" / id) })
```

### Body in failed assertions

`http4s-munit` always includes the responses body in a failed assertion's message.

For example, when running the following suite...

```scala
import cats.effect.IO

import org.http4s._

class MySuite extends munit.Http4sSuite {

  override def http4sMUnitClientFixture = 
    HttpRoutes.of[IO](_ => Ok("""{"id": 1, "name": "Jose"}""")).orFail.asFixture

  test(GET(uri"users"))(response => assertEquals(response.status.code, 204))

}
```

...it will fail with this message:

```
X MySuite.GET -> users  0.042s munit.ComparisonFailException: MySuite.scala:12
12:  test(GET(uri"users"))(response => assertEquals(response.status.code, 204))
values are not the same
=> Obtained
200
=> Diff (- obtained, + expected)
-200
+204

Response body was:

{
  "id": 1,
  "name": "Jose"
}
```

The body will be prettified using `http4sMUnitBodyPrettifier`, which, by default, will try to parse it as JSON and apply a code highlight if `munitAnsiColors` is `true`. If you want a different output or disabling body-prettifying just override this method.

### Response clues

Apart from the response body clues introduced in the previous section, `http4s-munit` also provides a simple way to transform a response into clues: the `response.clues` extension method.

The output of this extension method can be tweaked by using the `http4sMUnitResponseClueCreator`.

For example, this can be used on container suites to filter logs relevant to the current request (if your logs are JSON objects containing the request id):

```scala
import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all._

import com.dimafeng.testcontainers.GenericContainer
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.ci._

class TestContainersSuite extends munit.Http4sSuite {

  override def http4sMUnitClientFixture = ResourceFunFixture {
    Resource.fromAutoCloseable(IO(container.start()).as(container)) >>
      EmberClientBuilder.default[IO].build.map(_.withBaseUri(localhost.withPort(container.mappedPort(80))))
  }

  override def http4sMUnitResponseClueCreator(response: Response[IO]) = {
    val logs = response.headers
      .get(ci"x-request-id")
      .map(_.head.value)
      .map(id => container.logs.split("\n").filter(_.contains(id)).mkString("\n"))
      .getOrElse(container.logs)

    clues(response, logs)
  }

  lazy val container = GenericContainer(dockerImage = "mendhak/http-https-echo", exposedPorts = List(80))

  test(GET(uri"ping")) { response =>
    assertEquals(response.status.code, 200, response.clues)
    assertIOBoolean(response.as[Json].map(_.isObject), response.clues)
  }

}
```