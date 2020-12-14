package munit

import com.dimafeng.testcontainers.GenericContainer
import org.http4s.Uri

trait LowPrecedenceContainer2Uri {

  implicit def GenericContainer2Uri[A <: GenericContainer]: A => Uri = container =>
    Uri.unsafeFromString(s"http://localhost:${container.mappedPort(80)}")

}
