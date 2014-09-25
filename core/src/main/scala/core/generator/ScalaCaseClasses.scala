package core.generator

import core._
import Text._

sealed trait ClientType

case object Play22 extends ClientType
case object Play23 extends ClientType
case object Commons6 extends ClientType

object ScalaCaseClasses {
  def generate(sd: ServiceDescription): String = {
    generate(new ScalaServiceDescription(sd))
  }

  def generate(ssd: ScalaServiceDescription, clientType: ClientType = Play23): String = {
    Seq(
      s"package ${ssd.modelPackageName} {",
      ssd.models.map { model =>
        model.description.map { desc => ScalaUtil.textToComment(desc) + "\n" }.getOrElse("") +
        s"case class ${model.name}(${model.argList.getOrElse("")})"
      }.mkString("\n\n").indent(2),
      "",
      generateEnums(ssd, clientType).mkString("\n\n").indent(2),
      s"}"
    ).mkString("\n")
  }

  private def generateEnums(ssd: ScalaServiceDescription, clientType: ClientType = Play23): Seq[String] = {
    clientType match {
      case Commons6 =>
        ssd.enums.headOption.fold(Seq.empty[String])(_ => Seq("import com.fasterxml.jackson.annotation.{JsonCreator, JsonValue}")) ++
        ssd.enums.map { enum =>
          Commons6Enums.build(enum)
        }
      case _ =>
        ssd.enums.map { enum =>
          Play2Enums.build(enum)
        }
    }
  }
}
