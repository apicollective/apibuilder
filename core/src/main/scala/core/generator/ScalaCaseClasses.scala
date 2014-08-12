package core.generator

import core._
import Text._

object ScalaCaseClasses {
  def generate(sd: ServiceDescription): String = {
    generate(new ScalaServiceDescription(sd))
  }

  def generate(ssd: ScalaServiceDescription): String = {
    Seq(
      s"package ${ssd.packageName}.models {",
      ssd.models.map { model =>
        model.description.map { desc => ScalaUtil.textToComment(desc) }.getOrElse("") +
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
