package munit

import cats.Show
import cats.effect.IO
import cats.syntax.all._

import org.http4s.ContextRequest
import org.http4s.Uri

object Http4sMUnitDefaults {

  def http4sMUnitNameCreator[A: Show](
      request: ContextRequest[IO, A],
      followingRequests: List[String],
      testOptions: TestOptions,
      config: Http4sMUnitConfig
  ): String = {
    val clue = followingRequests.+:(testOptions.name).filter(_.nonEmpty) match {
      case Nil                 => ""
      case List(head)          => s" ($head)"
      case List(first, second) => s" ($first and $second)"
      case list                => s"${list.init.mkString(" (", ", ", ", and")} ${list.last})" // scalafix:ok
    }

    val context = request.context match {
      case _: Unit => None
      case context => context.show.some.filterNot(_.isEmpty())
    }

    val reps = config.repetitions match {
      case Some(rep) if rep > 1 =>
        s" - executed $rep times" + config.maxParallel.fold("")(paral => s" with $paral in parallel")
      case _ => ""
    }

    s"${request.req.method.name} -> ${Uri.decode(request.req.uri.renderString)}$clue${context.fold("")(" as " + _)}$reps"
  }

}
