package models

import play.api.libs.json._

case class Code(source: String)

object Code {
  implicit val reads = Json.reads[Code]

  val targets = {
    List(
      "ruby-client",
      "play-2.2-client",
      "play-2.2-json",
      "scala-models"
    )
  }

  def humanTarget(target: String) = target.split('-').map(_.capitalize).mkString(" ")
}
