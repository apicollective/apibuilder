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
s"""object ${model.name} {
  implicit val reads: play.api.libs.json.Reads[${model.name}] =
${Play2Util.jsonReads(model).indent(4)}

  implicit val writes: play.api.libs.json.Writes[${model.name}] =
${Play2Util.jsonWrites(model).indent(4)}
}
"""
    }.mkString("\n")

s"""package ${ssd.name.toLowerCase}.models {
${caseClasses.indent}

${companions.indent}
}"""
  }
}
