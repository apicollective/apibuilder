package models

import core.Text
import core.generator.{ScalaEnum, ScalaServiceDescription}

object Play2Bindables {

  def build(
    ssd: ScalaServiceDescription
  ): Option[String] = {
    import Text._

    ssd.enums match {
      case Nil => None
      case enums => {
        Some(
          Seq(
            "object Bindables {",
            "",
            "  import play.api.mvc.{PathBindable, QueryStringBindable}",
            "  import org.joda.time.{DateTime, LocalDate}",
            "  import org.joda.time.format.ISODateTimeFormat",
            s"  import ${ssd.packageName}.models._",
            "",
            buildDefaults().indent(2),
            "",
            enums.map( buildImplicit(_) ).mkString("\n\n").indent(2),
            "",
            "}"
          ).mkString("\n")
        )
      }
    }
  }

  private def buildDefaults(): String = {
    """
// Type: date-time-iso8601
implicit val pathBindableTypeDateTimeIso8601 = new PathBindable.Parsing[DateTime](
  ISODateTimeFormat.dateTimeParser.parseDateTime(_), _.toString, (key: String, e: Exception) => s"Error parsing date time $key. Example: 2014-04-29T11:56:52Z"
)

// Type: date-iso8601
implicit val pathBindableTypeDateIso8601 = new PathBindable.Parsing[LocalDate](
  ISODateTimeFormat.yearMonthDay.parseLocalDate(_), _.toString, (key: String, e: Exception) => s"Error parsing date time $key. Example: 2014-04-29"
)
""".trim
  }

  private[models] def buildImplicit(
    enum: ScalaEnum
  ): String = {
    s"// Enum: ${enum.name}\n" +
    """private val enum%sNotFound = (key: String, e: Exception) => s"Unrecognized $key, should be one of ${%s.all.mkString(", ")}"""".format(enum.name, enum.name) +
    s"""

implicit val pathBindableEnum${enum.name} = new PathBindable.Parsing[${enum.name}] (
  ${enum.name}.fromString(_).get, _.toString, enum${enum.name}NotFound
)

implicit val queryStringBindableEnum${enum.name} = new QueryStringBindable.Parsing[${enum.name}](
  ${enum.name}.fromString(_).get, _.toString, enum${enum.name}NotFound
)"""
  }

}
