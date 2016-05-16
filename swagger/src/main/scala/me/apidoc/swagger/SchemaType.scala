package me.apidoc.swagger

case class SchemaType(
  swagger: String,
  apidoc: String
)

object SchemaType {

  val all = Seq(
    SchemaType("int32", "integer"),
    SchemaType("int64", "long"),
    SchemaType("float", "double"),
    SchemaType("decimal", "decimal"),
    SchemaType("double", "double"),
    SchemaType("string", "string"),
    SchemaType("byte", "string"), // TODO: apidoc needs support for byte
    SchemaType("boolean", "boolean"),
    SchemaType("date", "date-iso8601"),
    SchemaType("dateTime", "date-time-iso8601"),
    SchemaType("uuid", "uuid")
  )

  def fromSwagger(
    swaggerType: String,
    format: Option[String]
  ): Option[String] = {
    format match {
      case None => all.find(_.swagger == swaggerType).map(_.apidoc)
      case Some(format) => all.find(_.swagger == format).map(_.apidoc)
    }
  }

  def fromSwaggerWithError(
    swaggerType: String,
    format: Option[String]
  ): String = {
    fromSwagger(swaggerType, format).getOrElse {
      sys.error(s"Could not resolve swagger type[$swaggerType] format[${format.getOrElse("")}]")
    }
  }

}
