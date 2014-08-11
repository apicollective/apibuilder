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
        val classDef = s"case class ${model.name}(${model.argList.getOrElse("")})"
      }.mkString("\n\n"),
      s"}",
      "",
      s"package ${ssd.packageName}.enums {",
      ssd.enums.map { enum =>
        Play2Enums.build(enum)
      }.mkString("\n\n"),
      s"}"
    ).mkString("\n")
  }
}
