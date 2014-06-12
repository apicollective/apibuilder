// TODO code generate this file from api.json
package apidoc.models

import play.api.libs.json._

import db.DetailedVersion

case class Code(version: DetailedVersion,
                target: String,
                source: String)

object Code {
  implicit val writes = Json.writes[Code]
}
