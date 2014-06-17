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
    val patchDef: String = {
      val fields = model.fields.map { field =>
        s"${field.name}: scala.Option[${field.typeName}] = None"
      }.mkString(",\n")

      val methods = model.fields.map { field =>
        s"def ${field.name}(value: ${field.typeName}): Patch = copy(${field.name} = Option(value))"
      }.mkString("\n\n")

      val apply = {
        val fields = model.fields.map { field =>
          s"${field.name} = ${field.name}.getOrElse(x.${field.name})"
        }.mkString(",\n")
        s"""def apply(x: ${model.name}): ${model.name} = x.copy(
${fields.indent}
)"""
      }

      s"""case class Patch(
${fields.indent}
) {

${methods.indent}

${apply.indent}
}"""
    }
    val companionDef: String  = {
      s"""object ${model.name} {
${patchDef.indent}
}"""
    }
    s"""${classDef.indent}

${companionDef.indent}
"""
  }.mkString(s"package ${ssd.packageName}.models {\n", "\n", "\n}")
}
