package munit

import com.dimafeng.testcontainers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

final case class DummyHttpContainer(underlying: GenericContainer) extends GenericContainer(underlying)

object DummyHttpContainer {

  final case class Def()
      extends GenericContainer.Def[DummyHttpContainer](
        new DummyHttpContainer(
          GenericContainer(
            dockerImage = "briceburg/ping-pong",
            exposedPorts = Seq(80),
            waitStrategy = Wait.forHttp("/ping")
          )
        )
      )
}
