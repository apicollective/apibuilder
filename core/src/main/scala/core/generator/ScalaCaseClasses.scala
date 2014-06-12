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
s"""$traitDef

${classDef}
"""
  }.mkString("\n")
}
