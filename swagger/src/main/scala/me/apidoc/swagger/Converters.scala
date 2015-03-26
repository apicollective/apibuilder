package me.apidoc.swagger

object Converters {

  private val PathParams = """\{(.+)\}""".r

  def substitutePathParameters(url: String): String = {
    PathParams.replaceAllIn(url, m => ":" + m.group(1))
  }

  def baseUrl(
    schemes: Seq[String],
    host: String,
    path: Option[String]
  ): Option[String] = {
    val rest = s"://$host${path.getOrElse("")}"
    schemes.map(_.toLowerCase).toList match {
      case Nil => None
      case one :: Nil => Some(s"$one$rest")
      case multiple => {
        // TODO: How to handle multiple schemes
        Some(s"${multiple.head}$rest")
      }
    }
  }

}
