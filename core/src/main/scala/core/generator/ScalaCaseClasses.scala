package core.generator

import core._
import Text._

object ScalaCaseClasses {
  def apply(json: String): String = {
    val sd = ServiceDescription(json)
    val ssd = new ScalaServiceDescription(sd)
    apply(ssd)
  }

  def apply(ssd: ScalaServiceDescription): String = ssd.models.map { model =>
    val traitDef: String = {
      val fields: String = model.fields.map { f =>
        val descr = ScalaUtil.textToComment(f.description)
        s"${descr}\ndef ${f.name}: ${f.typeName}"
      }.mkString("\n\n")
      val descr: String = ScalaUtil.textToComment(model.description)
      s"""${descr}trait ${model.name} {
${fields.indent}
}"""
    }
    val classDef: String = {
      s"""${model.scaladoc}\ncase class ${model.name}Impl(${model.argList}) extends ${model.name}"""
    }
s"""$traitDef

package ${model.name.toLowerCase} {
${classDef.indent}
}"""
  }.mkString("\n")
}
