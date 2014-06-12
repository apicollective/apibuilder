package models

import play.api.libs.json._

case class Code(source: String)

object Code {
  implicit val reads = Json.reads[Code]

  val targets = {
    List(
      "ruby-client",
      "play-2.3-routes",
      "play-2.3-client",
      "play-2.3-json",
      "scalacheck-generators",
      "scala-models"
    )
  }

  def humanTarget(target: String) = target.split('-').map(_.capitalize).mkString(" ")
}
