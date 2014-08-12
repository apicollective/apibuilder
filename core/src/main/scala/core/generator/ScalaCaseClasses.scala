package core.generator

import core._
import Text._

object ScalaCaseClasses {
  def apply(json: String): String = {
    apply(ServiceDescription(json))
  }

  def apply(sd: ServiceDescription): String = {
    apply(new ScalaServiceDescription(sd))
  }

  def apply(ssd: ScalaServiceDescription): String = {
    Seq(
      s"package ${ssd.packageName}.models {",
      ssd.models.map { model =>
        s"case class ${model.name}(${model.argList.getOrElse("")})"
      }.mkString("\n").indent(2),
      "",
      ssd.enums.map { enum =>
        Play2Enums.build(enum)
      }.mkString("\n\n").indent(2),
      s"}"
    ).mkString("\n")
  }
}
