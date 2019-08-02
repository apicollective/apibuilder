package me.apidoc.swagger

case class SchemaType(
  swaggerType: String,
  swaggerFormat: Option[String],
  apidoc: String
)

object SchemaType {

  // Following the swagger specification here:
  // http://swagger.io/specification/#dataTypeFormat
  val all = Seq(
    SchemaType("integer", None, "integer"),
    SchemaType("integer", Some("int32"), "integer"),
    SchemaType("integer", Some("int64"), "long"),
    SchemaType("number", Some("float"), "double"),
    SchemaType("number", Some("double"), "double"),
    SchemaType("string", None, "string"),
    SchemaType("string", Some("string"), "string"),
    SchemaType("string", Some("byte"), "string"), // TODO: apidoc needs support for byte
    SchemaType("string", Some("binary"), "string"),
    SchemaType("boolean", None, "boolean"),
    SchemaType("string", Some("date"), "date-iso8601"),
    SchemaType("string", Some("date-time"), "date-time-iso8601"),
    SchemaType("string", Some("uuid"), "uuid"),
    SchemaType("object", None, "object")
  )

  def fromSwagger(
    swaggerType: String,
    format: Option[String]
  ): Option[String] = {
    all.find(schemaType => schemaType.swaggerFormat == format && schemaType.swaggerType == swaggerType).map(_.apidoc)
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
