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
    val companions: String = ssd.models.map { model =>
      val unapply: String = model.fields.map { field =>
        s"x.${field.name}"
      }.mkString("Some(", ", ", ")")
s"""object ${model.name} {
  def unapply(x: ${model.name}) = {
${unapply.indent(4)}
  }

  implicit val reads: play.api.libs.json.Reads[${model.name}] =
${Play2Util.jsonReads(model).indent(4)}

  implicit val writes: play.api.libs.json.Writes[${model.name}] =
${Play2Util.jsonWrites(model).indent(4)}
}
"""
    }.mkString("\n")

s"""package ${ssd.packageName}.models {
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
  }

  import json._

${caseClasses.indent}

${companions.indent}
}"""
  }
}
