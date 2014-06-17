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
s"""${Play2Util.jsonReads(model)}

${Play2Util.jsonWrites(model)}"""
    }.mkString("\n\n")

s"""$caseClasses

package ${ssd.packageName}.models {
  package object json {
    import play.api.libs.json._
    import play.api.libs.functional.syntax._

    implicit val readsUUID = __.read[String].map(java.util.UUID.fromString)

    implicit val writesUUID = new Writes[java.util.UUID] {
      def writes(x: java.util.UUID) = JsString(x.toString)
    }

    implicit val readsJodaDateTime = __.read[String].map { str =>
      import org.joda.time.format.ISODateTimeFormat.dateTimeParser
      dateTimeParser.parseDateTime(str)
    }

    implicit val writesJodaDateTime = new Writes[org.joda.time.DateTime] {
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
