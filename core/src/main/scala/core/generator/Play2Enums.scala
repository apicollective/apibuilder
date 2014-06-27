package core.generator

import core.{ EnumerationFieldType, Field, Model, ServiceDescription, Text }

object Play2Enums {

  def build(model: Model): Option[String] = {
    val fields = enumFields(model)

    if (fields.isEmpty) {
      None
    } else {
      val className = Text.underscoreToInitCap(model.name)
      Some(
        s"  object $className {\n\n" +
          fields.map { field =>
            val traitName = Text.underscoreToInitCap(field.name)
            s"    sealed trait ${traitName}\n\n" +
            buildEnumForField(traitName, field.fieldtype.asInstanceOf[EnumerationFieldType])
          }.mkString("\n") +
        "  }"
      )
    }
  }

  /**
    * If this model has any enumerations, returns the implicits for
    * json serialization.
    */
  def buildJson(model: Model): Option[String] = {
    val fields = enumFields(model)

    if (fields.isEmpty) {
      None
    } else {
      val className = Text.underscoreToInitCap(model.name)
      Some(
        // TODO: datatype instead of string
        fields.map { field =>
          val traitName = Text.underscoreToInitCap(field.name)
          s"implicit val jsonReads$className$traitName = __.read[String].map($className.$traitName.apply)\n\n" +
          s"implicit val jsonWrites$className$traitName = new Writes[$className.$traitName] {\n" +
          s"  def writes(x: $className.$traitName) = JsString(x.toString)\n" +
          "}"
        }.mkString("\n\n")
      )
    }
  }

  private def enumFields(model: Model): Seq[Field] = {
    model.fields.filter { _.fieldtype.isInstanceOf[EnumerationFieldType] }
  }

  private def enumName(value: String): String = {
    Text.underscoreToInitCap(value)
  }

  private def buildEnumForField(traitName: String, enumType: EnumerationFieldType): String = {
    s"    object $traitName {\n\n" +
    enumType.values.map { value => 
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
    s"      val all = Seq(" + enumType.values.map { n => enumName(n) }.mkString(", ") + ")\n\n" +
    s"      private[this]\n" +
    s"      val byName = all.map(x => x.toString -> x).toMap\n\n" +
    s"      def apply(value: String): $traitName = fromString(value).getOrElse(UNDEFINED(value))\n\n" +
    s"      def fromString(value: String): Option[$traitName] = byName.get(value)\n\n" +
    s"    }\n"
  }

}
