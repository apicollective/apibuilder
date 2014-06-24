package core.generator

import core.{ Field, Model, ServiceDescription, Text }

object Play2Enums {
  def apply(sd: ServiceDescription): Option[String] = {
    val enums = sd.models.flatMap { model =>
      val fields = model.fields.filter { !_.values.isEmpty }
      if (fields.isEmpty) {
        None
      } else {
        fields.map { f => buildEnum(model, f) }
      }
    }

    if (enums.isEmpty) {
      None
    } else {
      val packageName = ScalaUtil.packageName(sd.name)
      Some(s"package $packageName.enums {\n\n" + enums.mkString("\n") + "\n}\n")
    }
  }

  private def buildEnum(model: Model, field: Field): String = {
    val packageName = ScalaUtil.packageName(model.name)
    s"  package $packageName {\n" +
    buildEnumForField(field) +
    "  }\n"
  }

  private def buildEnumForField(field: Field): String = {
    val className = Text.initCap(Text.snakeToCamelCase(field.name))
    s"    object $className extends Enumeration {\n" +
    s"      type $className = Value\n" +
    s"      val " + field.values.mkString(" ,") + " = Value\n" +
    s"    }\n"
  }
}
