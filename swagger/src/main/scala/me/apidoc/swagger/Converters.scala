package me.apidoc.swagger

object Converters {

  private val PathParams = """\{(.+)\}""".r

  def substitutePathParameters(url: String): String = {
    PathParams.replaceAllIn(url, m => ":" + m.group(1))
  }

  def baseUrls(
    schemes: Seq[String],
    host: String,
    path: Option[String]
  ): Seq[String] = {
    schemes.map(_.toLowerCase).map { scheme => s"$scheme://$host${path.getOrElse("")}" }
  }

  def combine(
    values: Seq[Option[String]],
    connector: String = "\n\n"
  ): Option[String] = {
    values.flatten.filter(!_.isEmpty) match {
      case Nil => None
      case nonEmptyValues => Some(nonEmptyValues.mkString(connector))
    }
  }

  def normalizeUrl(value: String): String = {
    value.toLowerCase.trim.replaceAll("_", "-")
  }

}
