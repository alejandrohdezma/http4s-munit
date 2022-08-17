# When http4s met MUnit

Integration library between [MUnit](https://scalameta.org/munit/) and [http4s](https://github.com/http4s/http4s/).

---

- [Installation](#installation)
- [Usage](#usage)
  - [Testing `HttpRoutes`](#testing-httproutes)
  - [Testing `AuthedRoutes`](#testing-authedroutes)
  - [Testing a remote HTTP server](#testing-a-remote-http-server)
  - [Testing an HTTP server running inside a container](#testing-an-http-server-running-inside-a-container)
- [Other features](#other-features)
  - [Tagging your tests](#tagging-your-tests)
  - [Stress-testing](#stress-testing)
  - [Nested requests](#nested-requests)
  - [Test names](#test-names)
  - [Body in failed assertions](#body-in-failed-assertions)
  - [Response clues](#response-clues)

## Installation

Add the following line to your `build.sbt` file:

```sbt
libraryDependencies += "com.alejandrohdezma" %% "http4s-munit" % "0.11.0" % Test
```

## Usage

### Testing `HttpRoutes`

We can use the `Http4sHttpRoutesSuite` to write tests for an `HttpRoutes` using `Request[IO]` values easily:

```scala
import cats.effect.IO

import org.http4s._

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

`http4s-munit` will automatically name your tests using the information of the provided `Request`. For example, for the test shown in the previous code snippet, the following will be shown when running the test:

```
munit.MyHttpRoutesSuite:0s
  + GET -> hello/Jose (Say hello to Jose) 0.014s
```

### Testing `HttpRoutes` per-test

We can use the `Http4sTestHttpRoutesSuite` to write tests for an `HttpRoutes` using `Request[IO]` values easily:

```scala
import cats.effect.IO

import org.http4s._

class MyHttpRoutesSuite extends munit.Http4sTestHttpRoutesSuite {

  test(
    routes = {
      HttpRoutes.of { case GET -> Root / "hello" / name =>
        Ok(s"Hi $name")
      }
    }
  )(GET(uri"hello" / "Jose")).alias("Say hello to Jose") { response =>
    assertIO(response.as[String], "Hi Jose")
  }

}
```

The `test` method receives `HttpRoutes[IO]` and `Request[IO]` object and when the test runs, it runs that request against the provided routes and let you assert the response.

`http4s-munit` will automatically name your tests using the information of the provided `Request`. For example, for the test shown in the previous code snippet, the following will be shown when running the test:

```
munit.MyHttpRoutesSuite:0s
  + GET -> hello/Jose (Say hello to Jose) 0.014s
```

### Testing `AuthedRoutes`

If we want to test authenticated routes (`AuthedRoutes` in http4s) we can use the `Http4sAuthedRoutesSuite`. It is completely similar to the previous suite, except that we need to ensure a `Show` instance is available for the auth "context" type and that we need to provide `AuthedRequest` instead of `Request` in the `test` definition. We can do this using its own constructor or by using our extension function `context` or `->`:

```scala
import cats.effect.IO

import org.http4s._

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

### Testing `AuthedRoutes`  per-test

If we want to test authenticated routes (`AuthedRoutes` in http4s) we can use the `Http4sTestAuthedRoutesSuite`. It is completely similar to the previous suite, except that we need to ensure a `Show` instance is available for the auth "context" type and that we need to provide `AuthedRequest` instead of `Request` in the `test` definition. We can do this using its own constructor or by using our extension function `context` or `->`:

```scala
import cats.effect.IO

import org.http4s._

class MyAuthedRoutesSuite extends munit.Http4sTestAuthedRoutesSuite[String] {

  test(routes = AuthedRoutes.of { case GET -> Root / "hello" / name as user =>
    Ok(s"$user: Hi $name")
  })(GET(uri"hello" / "Jose").context("alex")).alias("Say hello to Jose") { response =>
    assertIO(response.as[String], "alex: Hi Jose")
  }

}
```

### Testing a remote HTTP server

In the case you don't want to use static http4s routes, but a running HTTP server, you have available the `HttpSuite`. This suite behaves exactly the same as the previous ones except that you don't provide a `routes` object, but a `baseUri` with the URI of your HTTP server. Any `Request` added in tests will prepend
this URI before making a call using a real http4s `Client`. By default the library uses Ember as the client implementation (although you'll need to provide its dependency explicitly). If you want to use a different implementation just override `http4sMUnitClient`.

```scala
import cats.effect.IO
import cats.effect.Resource

import io.circe.Json

import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.blaze.client.BlazeClientBuilder

class GitHubSuite extends munit.HttpSuite {

  override def http4sMUnitClient: Resource[IO, Client[IO]] = BlazeClientBuilder[IO].resource

  override val baseUri: Uri = uri"https://api.github.com"

  test(GET(uri"users/gutiory")) { response =>
    assertEquals(response.status.code, 200)

    val result = response.as[Json].map(_.hcursor.get[String]("login"))

    assertIO(result, Right("gutiory"))
  }

}
```

### Testing an HTTP server running inside a container

Testing a Docker container with TestContainers and `http4s-munit` is easy. You
just need to use `TestCotnainersFixtures` and use an `HttpSuite` to connect to
it:

```scala
import cats.effect.IO

import com.dimafeng.testcontainers.GenericContainer
import com.dimafeng.testcontainers.munit.fixtures.TestContainersFixtures
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.ember.client.EmberClientBuilder

class TestContainersSuite extends munit.HttpSuite with TestContainersFixtures {

  override def http4sMUnitClient = EmberClientBuilder.default[IO].build

  // There is also available `ForEachContainerFixture`
  val container = ForAllContainerFixture {
    GenericContainer(dockerImage = "mendhak/http-https-echo", exposedPorts = List(80))
  }

  override def munitFixtures = List(container)

  override def baseUri() = Uri.unsafeFromString(s"http://localhost:${container().mappedPort(80)}")

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
import io.circe.Json
import org.http4s.circe._
import org.http4s.ember.client.EmberClientBuilder

class TestContainersSuite extends munit.HttpSuite {

  override def http4sMUnitClient =
    Resource.fromAutoCloseable(IO(container.start()).as(container)) >> EmberClientBuilder.default[IO].build

  lazy val container = GenericContainer(dockerImage = "mendhak/http-https-echo", exposedPorts = List(80))

  override def baseUri() = localhost.withPort(container.mappedPort(80))

  test(GET(uri"ping")) { response =>
    assertEquals(response.status.code, 200)
    assertIOBoolean(response.as[Json].map(_.isObject))
  }

}
```

## Other features

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

Sometimes (mostly while using the `HttpSuite` or `HttpFromContainerSuite`) one test needs some pre-condition in order to be executed (e.g., in order to test the deletion of a user, you need to create it first). In such cases, once the request has been passed to the `test` method, we can call `andThen` to provide nested requests from the response of the previous one:

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

The generated test names can be customized by overriding `http4sMUnitNameCreator`. Allows altering the name of the generated tests.

By default this method generate test names like:

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

class MySuite extends munit.Http4sHttpRoutesSuite {

  val routes: HttpRoutes[IO] = HttpRoutes.pure(Response().withEntity("""{"id": 1, "name": "Jose"}"""))

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

class TestContainersSuite extends munit.HttpSuite {

  override def http4sMUnitClient =
    Resource.fromAutoCloseable(IO(container.start()).as(container)) >> EmberClientBuilder.default[IO].build

  override def http4sMUnitResponseClueCreator(response: Response[IO]) = {
    val id = response.headers.get(ci"x-request-id").map(_.head.value)

    // Here you will filter `container.logs` using the `id`
    val logs = container.logs.split("\n").filter(_.contains(id)).mkString("\n")

    clues(response, logs)
  }

  lazy val container = GenericContainer(dockerImage = "mendhak/http-https-echo", exposedPorts = List(80))

  override def baseUri() = Uri.unsafeFromString(s"http://localhost:${container.mappedPort(80)}")

  test(GET(uri"ping")) { response =>
    assertEquals(response.status.code, 200, response.clues)
    assertIOBoolean(response.as[Json].map(_.isObject), response.clues)
  }

}
```
