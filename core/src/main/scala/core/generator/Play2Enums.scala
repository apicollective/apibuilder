package core.generator

import core.{ Enum, EnumValue, ServiceDescription, Text }

object Play2Enums {

  def build(enum: ScalaEnum): String = {
    import Text._
    val className = Text.underscoreToInitCap(enum.name)
    Seq(
      s"sealed trait ${enum.name}",
      s"object ${enum.name} {",
      buildValues(enum).indent(2),
      s"}"
    ).mkString("\n\n")
  }

  /**
    * Returns the implicits for json serialization.
    */
  def buildJson(prefix: String, enum: ScalaEnum): String = {
    enum.values.map { value =>
      s"implicit val jsonReads${prefix}${{enum.name}}_${value.name} = __.read[String].map(${enum.name}.${value.name}.apply)\n\n" +
      s"implicit val jsonWrites${prefix}${{enum.name}}_${value.name} = new Writes[${enum.name}.${value.name}] {\n" +
      s"  def writes(x: ${enum.name}.${value.name}) = JsString(x.toString)\n" +
      "}"
    }.mkString("\n\n")
  }

  private def buildValues(enum: ScalaEnum): String = {
    enum.values.map { value => 
      Seq(
        value.description.map { desc => ScalaUtil.textToComment(desc) },
        Some(s"""case object ${value.name} extends ${enum.name} { override def toString = "${value.name}" }""")
      ).flatten.mkString("\n")
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
case class UNDEFINED(override val toString: String) extends ${enum.name}

/**
 * all returns a list of all the valid, known values. We use
 * lower case to avoid collisions with the camel cased values
 * above.
 */
""" +
    s"val all = Seq(" + enum.values.map(_.name).mkString(", ") + ")\n\n" +
    s"private[this]\n" +
    s"val byName = all.map(x => x.toString -> x).toMap\n\n" +
    s"def apply(value: String): ${enum.name} = fromString(value).getOrElse(UNDEFINED(value))\n\n" +
    s"def fromString(value: String): scala.Option[${enum.name}] = byName.get(value)\n\n"
  }

}
