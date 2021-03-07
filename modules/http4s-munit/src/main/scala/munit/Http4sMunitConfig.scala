package munit

final case class Http4sMunitConfig(repetitions: Option[Int], maxParallel: Option[Int])

object Http4sMunitConfig {

  lazy val default = Http4sMunitConfig(
    sys.props
      .get("http4s.munit.repetitions")
      .map(_.toIntOption)
      .flatten
      .orElse(sys.env.get("HTTP4S_MUNIT_REPETITIONS").map(_.toIntOption).flatten),
    sys.props
      .get("http4s.munit.maxParallel")
      .map(_.toIntOption)
      .flatten
      .orElse(sys.env.get("HTTP4S_MAX_CONCURRENT").map(_.toIntOption).flatten)
  )
}
