package munit

import cats.Show
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.all._

import org.typelevel.vault.Key

/** Represents a request's context, currently this is only used on `Http4sAuthedRoutesSuite`. */
final private[munit] case class RequestContext(private val value: Any, asString: String) {

  def as[A] = value.asInstanceOf[A] // scalafix:ok

}

object RequestContext {

  def apply[A: Show](a: A): RequestContext = RequestContext(a, a.show)

  /** Used to retrieve/store requests' context */
  val key = Key.newKey[IO, RequestContext].unsafeRunSync()

}
