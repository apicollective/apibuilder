package core.generator

import core.{ Field, Model, ServiceDescription, Text }

object Play2Enums {

  def build(model: Model): Option[String] = {
    val fields = model.fields.filter { !_.values.isEmpty }
    if (fields.isEmpty) {
      None
    } else {
      val className = Text.initCap(Text.snakeToCamelCase(model.name))
      Some(s"  object $className {\n\n" +
        fields.map { field =>
          val traitName = Text.initCap(Text.snakeToCamelCase(field.name))
            s"    sealed trait ${traitName}\n\n" +
            buildEnumForField(traitName, field)
        }.mkString("\n") +
        "  }")
    }
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
