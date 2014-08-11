package core.generator

import core._
import Text._

object Play2Models {

  def apply(sd: ServiceDescription): String = {
    apply(new ScalaServiceDescription(sd))
  }

  def apply(ssd: ScalaServiceDescription): String = {
    val caseClasses = ScalaCaseClasses(ssd)
    val prefix = underscoreToInitCap(ssd.name)
    val enumJson: String = ssd.enums.flatMap { enum => Play2Enums.buildJson(prefix, enum) }.mkString("\n\n")
    val modelJson: String = ssd.models.map { model => Play2Json(ssd.name).generate(model) }.mkString("\n\n")

s"""$caseClasses

package ${ssd.packageName}.models {
  package object json {
    import play.api.libs.json._
    import play.api.libs.functional.syntax._

    private implicit val jsonReadsUUID = __.read[String].map(java.util.UUID.fromString)

    private implicit val jsonWritesUUID = new Writes[java.util.UUID] {
      def writes(x: java.util.UUID) = JsString(x.toString)
    }

    private implicit val jsonReadsJodaDateTime = __.read[String].map { str =>
      import org.joda.time.format.ISODateTimeFormat.dateTimeParser
      dateTimeParser.parseDateTime(str)
    }

    private implicit val jsonWritesJodaDateTime = new Writes[org.joda.time.DateTime] {
      def writes(x: org.joda.time.DateTime) = {
        import org.joda.time.format.ISODateTimeFormat.dateTime
        val str = dateTime.print(x)
        JsString(str)
      }
    }

${modelJson.indent(4)}
  }
}

package ${ssd.packageName}.enums {
${enumJson.indent(2)}
}"""
  }
}
