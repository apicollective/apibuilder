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

  private def enumName(value: String): String = {
    Text.initCap(Text.snakeToCamelCase(value))
  }

  private def buildEnumForField(traitName: String, field: Field): String = {
    s"    object $traitName {\n\n" +
    field.values.map { value => 
      val name = enumName(value)
      s"""      case object ${name} extends $traitName { override def toString = "$value" }"""
    }.mkString("\n") + "\n" +
    s"""
      /**
       * UNDEFINED captures values that are sent either in error or
       * that were added by the server after this library was
       * generated. We want to make it easy and obvious for users of
       * this library to handle this case gracefully.
       * 
       * We use all CAPS for the variable name to avoid collisions
       * with the camel cased values above.
       */
      case class UNDEFINED(override val toString: String) extends ${traitName}

      /**
       * all returns a list of all the valid, known values. We use
       * lower case to avoid collisions with the camel cased values
       * above.
       */
""" +
    s"      val all = Seq(" + field.values.map { n => enumName(n) }.mkString(", ") + ")\n\n" +
    s"      private[this]\n" +
    s"      val byName = all.map(x => x.toString -> x).toMap\n\n" +
    s"      def apply(value: String): $traitName = byName.get(value).getOrElse(UNDEFINED(value))\n\n" +
    s"    }\n"
  }

}
