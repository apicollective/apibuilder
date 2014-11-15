package models

import com.gilt.apidocgenerator.models.ServiceDescription
import core._
import lib.Text._
import core.generator.{ScalaEnums, ScalaCaseClasses, ScalaServiceDescription, CodeGenerator}

object Play2Models extends CodeGenerator {

  override def generate(sd: ServiceDescription): String = {
    apply(sd)
  }

  def apply(sd: ServiceDescription): String = {
    apply(new ScalaServiceDescription(sd))
  }

  def apply(ssd: ScalaServiceDescription): String = {
    val caseClasses = ScalaCaseClasses.generate(ssd)
    val prefix = underscoreAndDashToInitCap(ssd.name)
    val enumJson: String = ssd.enums.map { enum => ScalaEnums.buildJson(ssd.name, enum) }.mkString("\n\n")
    val modelJson: String = ssd.models.map { model => Play2Json(ssd.name).generate(model) }.mkString("\n\n")

s"""$caseClasses

package ${ssd.modelPackageName} {
  package object json {
    import play.api.libs.json.__
    import play.api.libs.json.JsString
    import play.api.libs.json.Writes
    import play.api.libs.functional.syntax._

    private[${ssd.packageNamePrivate}] implicit val jsonReadsUUID = __.read[String].map(java.util.UUID.fromString)

    private[${ssd.packageNamePrivate}] implicit val jsonWritesUUID = new Writes[java.util.UUID] {
      def writes(x: java.util.UUID) = JsString(x.toString)
    }

    private[${ssd.packageNamePrivate}] implicit val jsonReadsJodaDateTime = __.read[String].map { str =>
      import org.joda.time.format.ISODateTimeFormat.dateTimeParser
      dateTimeParser.parseDateTime(str)
    }

    private[${ssd.packageNamePrivate}] implicit val jsonWritesJodaDateTime = new Writes[org.joda.time.DateTime] {
      def writes(x: org.joda.time.DateTime) = {
        import org.joda.time.format.ISODateTimeFormat.dateTime
        val str = dateTime.print(x)
        JsString(str)
      }
    }

${enumJson.indent(4)}
${modelJson.indent(4)}
  }
}"""
  }
}
