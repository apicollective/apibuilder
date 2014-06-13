package models

import play.api.libs.json._

case class Code(source: String)

object Code {
  implicit val reads = Json.reads[Code]
}
