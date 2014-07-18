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

  def apply(ssd: ScalaServiceDescription): String = ssd.models.map { model =>
    val classDef = s"case class ${model.name}(${model.argList.getOrElse("")})"
    Play2Enums.build(model.model) match {
      case None => classDef.indent
      case Some(enums) => classDef.indent + "\n" + enums
    }
  }.mkString(s"package ${ssd.packageName}.models {\n", "\n", "\n}")
}
