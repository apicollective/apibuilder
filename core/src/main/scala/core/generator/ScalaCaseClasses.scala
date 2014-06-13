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
    classDef.indent
  }.mkString(s"package ${ssd.packageName}.models {\n", "\n", "\n}")
}
