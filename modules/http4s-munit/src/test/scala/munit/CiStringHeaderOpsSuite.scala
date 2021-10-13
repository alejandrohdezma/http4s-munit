package munit

import cats.effect.IO
import cats.effect.SyncIO
import cats.effect.kernel.Resource

import org.http4s.Header
import org.http4s.Response
import org.typelevel.ci._

class HeaderInterpolatorSuite extends Http4sSuite[String] {

  override def http4sMUnitNameCreator(
      request: String,
      followingRequests: List[String],
      testOptions: TestOptions,
      config: Http4sMUnitConfig
  ): String = fail("This should no be called")

  override def http4sMUnitFunFixture: SyncIO[FunFixture[String => Resource[IO, Response[IO]]]] =
    fail("This should no be called")

  test("header interpolator creates a valid raw header") {
    val header = ci"my-header" := "my-value"

    assertEquals(header, Header.Raw(ci"my-header", "my-value"))
  }

}
