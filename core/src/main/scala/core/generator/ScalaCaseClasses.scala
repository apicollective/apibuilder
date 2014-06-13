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
    val classDef: String = {
      s"""case class ${model.name}(${model.argList})"""
    }
    val objectDef: String = {
      val applyArgs = model.fields.map { field =>
        s"${field.name}: ${field.typeName}"
      }.mkString(", ")


      s"""object ${model.name} {
}
"""
    }
s"""${classDef.indent}

${objectDef.indent}"""
  }.mkString(s"package ${ssd.packageName}.models {\n", "\n", "\n}")
}
