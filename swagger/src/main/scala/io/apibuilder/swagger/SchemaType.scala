package io.apibuilder.swagger

import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import cats.data.ValidatedNec

case class SchemaType(
  swaggerType: String,
  swaggerFormat: Option[String],
  apiBuilderType: String
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
    SchemaType("string", Some("byte"), "string"), // TODO: API Builder needs support for byte
    SchemaType("string", Some("binary"), "string"),
    SchemaType("boolean", None, "boolean"),
    SchemaType("string", Some("date"), "date-iso8601"),
    SchemaType("string", Some("date-time"), "date-time-iso8601"),
    SchemaType("string", Some("uuid"), "uuid"),
    SchemaType("object", None, "object")
  )

  def validateFromSwagger(
    swaggerType: String,
    format: Option[String]
  ): ValidatedNec[String, String] = {
    fromSwagger(swaggerType, format) match {
      case None => s"Unable to convert swagger type '$swaggerType' with format '$format' to API Builder type".invalidNec
      case Some(t) => t.validNec
    }
  }

  def fromSwagger(
    swaggerType: String,
    format: Option[String]
  ): Option[String] = {
    all
      .find(schemaType => schemaType.swaggerFormat == format && schemaType.swaggerType == swaggerType)
      .map(_.apiBuilderType)
      .orElse {
        swaggerType match {
          /*
           Format is an open-valued property (can have any value, such as "email", "html", etc). In the case of type string
           and we want to always map it to an ApiBuilder string for all the case not explicitly mapped (e.g. date-time, uuid).
           */
          case "string" => Some("string")
          case _ => None
        }
      }
  }

  def mustConvert(
    swaggerType: String,
    format: Option[String]
  ): String = {
    validateFromSwagger(swaggerType, format) match {
      case Invalid(e) => sys.error(e.toList.mkString(", "))
      case Valid(t) => t
    }
  }

}
