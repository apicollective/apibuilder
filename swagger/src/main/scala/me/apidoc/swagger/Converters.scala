package me.apidoc.swagger

object Converters {

  def baseUrls(
    schemes: Seq[String],
    host: String,
    path: Option[String]
  ): Seq[String] = {
    schemes.map(_.toLowerCase).map { scheme => s"$scheme://$host${path.getOrElse("")}" }
  }

}
