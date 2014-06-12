package core.generator

import core._
import Text._

object Play2Models {
  def apply(json: String): String = {
    val sd = ServiceDescription(json)
    val ssd = new ScalaServiceDescription(sd)
    apply(ssd)
  }

  def apply(ssd: ScalaServiceDescription): String = {
    val caseClasses = ScalaCaseClasses(ssd)
    val modelJson: String = ssd.models.map { model =>
s"""implicit val reads${model.name}: play.api.libs.json.Reads[${model.name}] =
${Play2Util.jsonReads(model).indent(2)}

implicit val writes${model.name}: play.api.libs.json.Writes[${model.name}] =
${Play2Util.jsonWrites(model).indent(2)}"""
    }.mkString("\n\n")

s"""$caseClasses

package ${ssd.packageName}.models {
  package object json {
    import play.api.libs.json._
    import play.api.libs.functional.syntax._

    implicit val jsonReadsUUID = __.read[String].map(java.util.UUID.fromString)

    implicit val jsonWritesUUID = new Writes[java.util.UUID] {
      def writes(x: java.util.UUID) = JsString(x.toString)
    }

    implicit val jsonReadsJodaDateTime = __.read[String].map { str =>
      import org.joda.time.format.ISODateTimeFormat.dateTimeParser
      dateTimeParser.parseDateTime(str)
    }

    implicit val jsonWritesJodaDateTime = new Writes[org.joda.time.DateTime] {
      def writes(x: org.joda.time.DateTime) = {
        import org.joda.time.format.ISODateTimeFormat.dateTime
        val str = dateTime.print(x)
        JsString(str)
      }
    }

${modelJson.indent(4)}
  }
}"""
  }
}
