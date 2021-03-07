package munit

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import cats.syntax.option._

final case class Http4sMunitConfig(repetitions: Option[Int], maxParallel: Option[Int])

object Http4sMunitConfig {

  lazy val default = Http4sMunitConfig(
    sys.props
      .get("http4s.munit.repetitions")
      .map(toIntOption)
      .flatten
      .orElse(sys.env.get("HTTP4S_MUNIT_REPETITIONS").map(toIntOption).flatten),
    sys.props
      .get("http4s.munit.maxParallel")
      .map(toIntOption)
      .flatten
      .orElse(sys.env.get("HTTP4S_MAX_CONCURRENT").map(toIntOption).flatten)
  )

  private def toIntOption(intStr: String): Option[Int] = Try(intStr.toInt) match {
    case Success(value) => value.some
    case Failure(_)     => None
  }
}
