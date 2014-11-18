package generator

import com.gilt.apidocgenerator.models.ServiceDescription
import core._
import lib.Text._

object ScalaCaseClasses extends CodeGenerator {

  def generate(sd: ServiceDescription): String = {
    generate(new ScalaServiceDescription(sd))
  }

  def generate(ssd: ScalaServiceDescription, genEnums: Seq[ScalaEnum] => String = generatePlayEnums): String = {
    s"package ${ssd.modelPackageName} {\n\n  " +
    Seq(
      ssd.models.map { model =>
        generateCaseClass(model)
      }.mkString("\n\n").indent(2),
      "",
      genEnums(ssd.enums).indent(2)
    ).mkString("\n").trim +
    s"\n\n}"
  }

  def generateCaseClass(model: ScalaModel): String = {
    model.description.map { desc => ScalaUtil.textToComment(desc) + "\n" }.getOrElse("") +
    s"case class ${model.name}(${model.argList.getOrElse("")})"
  }

  private def generatePlayEnums(enums: Seq[ScalaEnum]): String = {
    enums.map { enum =>
      ScalaEnums.build(enum)
    }.mkString("\n\n")
  }

}
