package generator

import models.ApidocHeaders
import com.gilt.apidocspec.models.Service
import core._
import lib.Text._

object ScalaCaseClasses extends CodeGenerator {

  def generate(sd: Service): String = {
    generate(new ScalaService(sd))
  }

  def generate(
    ssd: ScalaService,
    genEnums: Seq[ScalaEnum] => String = generatePlayEnums,
    addHeader: Boolean = true
  ): String = {
    val header = addHeader match {
      case false => ""
      case true => ApidocHeaders(ssd.serviceDescription.userAgent).toJavaString() + "\n"
    }

    s"${header}package ${ssd.modelPackageName} {\n\n  " +
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
