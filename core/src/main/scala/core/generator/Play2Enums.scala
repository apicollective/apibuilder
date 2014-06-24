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
      println(enums.mkString("\n"))
      Some(s"package $packageName.enums {\n\n" + enums.mkString("\n") + "\n}\n")
    }
  }

  private def buildEnum(model: Model, field: Field): String = {
    val className = Text.initCap(Text.snakeToCamelCase(model.name))
    val traitName = Text.initCap(Text.snakeToCamelCase(field.name))
    s"  object $className {\n\n" +
    s"    sealed trait ${traitName}\n\n" +
    buildEnumForField(traitName, field) + "\n" +
    "  }\n"
  }

  private def buildEnumForField(traitName: String, field: Field): String = {
    val allName = "All" + Text.pluralize(traitName)
    s"    object $traitName {\n\n" +
    field.values.map { value => s"      case object ${value} extends $traitName" }.mkString("\n") + "\n\n" +
    s"      val $allName = Seq(" + field.values.mkString(", ") + ")\n" +
    s"      private[this]\n" +
    s"      val NameLookup = $allName.map(x => x.toString -> x).toMap\n\n" +
    s"      def apply(value: String): Option[$traitName] = NameLookup.get(value)\n\n" +
    s"    }\n"
  }
}
