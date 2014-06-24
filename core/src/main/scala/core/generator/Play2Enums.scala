package core.generator

import core.{ Field, Model, ServiceDescription }

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
      Some(s"package ${packageName}.enums {\n\n" + enums + "\n}\n")
    }
  }

  private def buildEnum(model: Model, field: Field): String = {
    "// %s.%s\n".format(model.name, field.name)
  }
}
