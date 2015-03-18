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
    SchemaType("double", "double"),
    SchemaType("string", "string"),
    SchemaType("byte", "string"), // TODO: apidoc needs support for byte
    SchemaType("boolean", "boolean"),
    SchemaType("date", "date-iso8601"),
    SchemaType("dateTime", "date-time-iso8601")
  )

  def fromSwagger(swaggerType: String): Option[SchemaType] = {
    all.find(_.swagger == swaggerType)
  }

}

