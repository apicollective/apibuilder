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
      val descr: String = f.description.map(desc => ScalaUtil.textToComment(desc) + "\n").getOrElse("")
        s"${descr}def ${f.name}: ${f.typeName}"
      }.mkString("\n\n")
      val descr: String = model.description.map(desc => ScalaUtil.textToComment(desc) + "\n").getOrElse("")
      s"""${descr}trait ${model.name} {
${fields.indent}
}"""
    }
    val classDef: String = {
      s"""case class ${model.name}Impl(${model.argList}) extends ${model.name}"""
    }
    val objectDef: String = {
      val applyArgs = model.fields.map { field =>
        s"${field.name}: ${field.typeName}"
      }.mkString(", ")

      val apply = model.fields.map { field =>
        field.name
      }.mkString(s"new ${model.name}Impl(", ",", ")")

      val unapply: String = model.fields.map { field =>
        s"x.${field.name}"
      }.mkString("Some(", ", ", ")")

      val toImpl = {
        val args = model.fields.map { field =>
          s"x.${field.name}"
        }.mkString(",")
        s"new ${model.name}Impl($args)"
      }

      s"""object ${model.name} {
  def apply($applyArgs): ${model.name}Impl = {
${apply.indent(4)}
  }

  def unapply(x: ${model.name}) = {
${unapply.indent(4)}
  }

  implicit def toImpl(x: ${model.name}): ${model.name}Impl = x match {
    case impl: ${model.name}Impl => impl
    case _ => $toImpl
  }
}
"""
    }
s"""${traitDef.indent}

${classDef.indent}

${objectDef.indent}"""
  }.mkString(s"package ${ssd.packageName}.models {\n", "\n", "\n}")
}
