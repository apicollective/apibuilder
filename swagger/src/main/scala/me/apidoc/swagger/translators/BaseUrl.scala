package me.apidoc.swagger.translators

object BaseUrl {

  def apply(
    schemes: Seq[String],
    host: String,
    path: Option[String]
  ): Seq[String] = {
    schemes.map(_.toLowerCase).map { scheme => s"$scheme://$host${path.getOrElse("")}" }
  }

}
