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
s"""${model.description}case class ${model.name}(${model.argList})
"""
  }.mkString("\n")
}
