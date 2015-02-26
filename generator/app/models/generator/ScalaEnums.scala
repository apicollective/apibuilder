package generator

import lib.Text

object ScalaEnums {

  def build(enum: ScalaEnum): String = {
    import lib.Text._
    Seq(
      enum.description.map { desc => ScalaUtil.textToComment(desc) + "\n" }.getOrElse("") +
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
    val enumName = enum.name
    val readsName = s"jsonReads${prefix}${enumName}"
    val writesName = s"jsonWrites${prefix}${enumName}"
    Seq(
      s"""@deprecated("02/26/2015", "This will be removed after 03/26/2015. Use ${readsName} instead.")""",
      s"""implicit val jsonReads${prefix}Enum_${enumName} = __.read[String].map(${enumName}.apply)""",

      s"""@deprecated("02/26/2015", "This will be removed after 03/26/2015. Use ${writesName} instead.")""",
      s"""implicit val jsonWrites${prefix}Enum_${enumName} = new Writes[${enumName}] {""",
      s"""  def writes(x: ${enumName}) = JsString(x.toString)""",
      s"""}""",

      s"""implicit val ${readsName} = __.read[String].map(${enumName}.apply)""",

      s"""/**""",
      s""" * Reads a valid instance of the enum type, rejecting UNDEFINED results.""",
      s""" * NOTE: Not an implicit as it would conflict with the default permissive""",
      s""" * Reads instance.""",
      s""" */""",
      s"""val ${readsName}_Valid = Reads { jsValue =>""",
      s"""  ${readsName}(jsValue).flatMap {""",
      s"""    case ${enumName}.UNDEFINED(invalid) =>""",
      s"""      JsError(s"invalid event_type[$${invalid}]")""",
      s"""    case legal => JsSuccess(legal)""",
      s"""  }""",
      s"""}""",


      s"""implicit val ${writesName} = new Writes[${enumName}] {""",
      s"""  def writes(x: ${enumName}) = JsString(x.toString)""",
      s"""}"""
    ).mkString("\n")
  }

  private def buildValues(enum: ScalaEnum): String = {
    enum.values.map { value => 
      Seq(
        value.description.map { desc => ScalaUtil.textToComment(desc) },
        Some(s"""case object ${value.name} extends ${enum.name} { override def toString = "${value.originalName}" }""")
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
