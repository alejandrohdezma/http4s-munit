package munit

package object config {

  lazy val values = Config(
    sys.props
      .get("http4s.munit.repetitions")
      .map(_.toIntOption)
      .flatten
      .orElse(sys.env.get("HTTP4S_MUNIT_REPETITIONS").map(_.toIntOption).flatten),
    sys.props
      .get("http4s.munit.maxConcurrent")
      .map(_.toIntOption)
      .flatten
      .orElse(sys.env.get("HTTP4S_MAX_CONCURRENT").map(_.toIntOption).flatten)
  )
}
