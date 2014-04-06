package core

import play.api.libs.json._

object ServiceDescription {

  def apply(apiJson: String): ServiceDescription = {
    val parser = JsonParser(apiJson)
    ServiceDescription(parser)
  }

  def apply(jsValue: JsValue): ServiceDescription = {
    val parser = JsonParser(jsValue)
    ServiceDescription(parser)
  }

}

case class ServiceDescriptionValidator(sd: ServiceDescription) {

  private val RequiredFields = Seq("base_url", "name", "resources")

  /**
   * Validate basic structure, returning a list of error messages
   */
  def validate(): Seq[String] = {
    val errors = validateRequiredFields()

    if (errors.isEmpty) {
      validateResources
    } else {
      errors
    }
  }

  private def validateRequiredFields(): Seq[String] = {
    RequiredFields.flatMap { field =>
      val value = sd.parser.getOptionalValue(sd.parser.json, field)
      if (value.isEmpty) {
        Some(s"Missing field named[$field]")
      } else {
        None
      }
    }
  }

  private def validateResources(): Seq[String] = {
    try {
      if (sd.resources.isEmpty) {
        Seq("Must have at least one resource")
      } else {
        Seq.empty
      }
    } catch {
      case e: Throwable => {
        Seq("Error parsing resources: "  + e.toString)
      }
    }
  }


}

/**
 * Parses api.json file into a set of case classes
 */
case class ServiceDescription(parser: JsonParser) {

  lazy val resources: Seq[Resource] = {
    parser.getValue(parser.json, "resources").as[JsObject].fields.map { v =>
      v match {
        case(key, value) => Resource.parse(parser, key, value.as[JsObject])
      }
    }
  }

  lazy val baseUrl = parser.getValue(parser.json, "base_url").as[String]
  lazy val basePath = parser.getOptionalValue(parser.json, "base_path").map(v => v.as[String])
  lazy val name = parser.getValue(parser.json, "name").as[String]
  lazy val description = parser.getOptionalValue(parser.json, "description").map(v => v.as[String])

}

case class Resource(name: String,
                    path: String,
                    description: Option[String],
                    fields: Seq[Field],
                    operations: Seq[Operation])

case class Operation(method: String,
                     path: Option[String],
                     description: Option[String],
                     parameters: Seq[Field],
                     response: Response)

case class Field(name: String,
                 dataType: String,
                 description: Option[String] = None,
                 required: Boolean = true,
                 format: Option[String] = None,
                 references: Option[String] = None,
                 default: Option[String] = None,
                 example: Option[String] = None,
                 minimum: Option[Int] = None,
                 maximum: Option[Int] = None)

case class Response(code: Int,
                    resource: Option[String] = None,
                    multiple: Boolean = false)

object Resource {

  def parse(parser: JsonParser, name: String, value: JsObject): Resource = {
     val path = parser.getOptionalValue(value, "path").map(_.as[JsString].value).getOrElse( s"/${name}" )
     val description = parser.getOptionalValue(value, "description").map(_.as[JsString].value)
     val fields = parser.getArray(value, "fields").map { json => Field.parse(parser, json.as[JsObject]) }

     val operations = parser.getArray(value, "operations").map { json =>
      Operation(method = parser.getValue(json, "method").as[JsString].value,
                path = parser.getOptionalValue(json, "path").map(_.as[JsString].value),
                description = parser.getOptionalValue(json, "description").map(_.as[JsString].value),
                response = Response.parse(parser, json.as[JsObject]),
                parameters = parser.getArray(json, "parameters").map { data => Field.parse(parser, data.as[JsObject]) })
    }

    Resource(name = name,
             path = path,
             description = description,
             fields = fields,
             operations = operations)
  }

}

object Response {

  def parse(parser: JsonParser, json: JsObject): Response = {
    parser.getOptionalValue(json, "response_code") match {

      case Some(v) => {
        Response(code = v.as[JsNumber].value.toInt)
      }

      case None => {
        parser.getValue(json, "response") match {

          case v: JsArray => {
            assert(v.value.size == 1,
                   "When an array, response must contain exactly 1 element: %s".format(v.value.mkString(", ")))
            Response(code = 200,
                     resource = Some(v.value.head.as[JsString].value),
                     multiple = true)
          }

          case v: JsString => {
            Response(code = 200,
                     resource = Some(v.value))
          }

          case v: Any => {
            sys.error(s"Could not parse response: $v")
          }
        }

      }

    }
  }

}


object Field {

  def parse(parser: JsonParser, json: JsObject): Field = {
    Field(name = parser.getValue(json, "name").as[JsString].value,
          dataType = parser.getValue(json, "type").as[JsString].value,
          description = parser.getOptionalValue(json, "description").map(_.as[JsString].value),
          references = parser.getOptionalValue(json, "references").map(_.as[JsString].value),
          required = parser.getOptionalValue(json, "required").map(_.as[JsBoolean].value).getOrElse(true),
          default = parser.getOptionalValue(json, "default").map(_.toString),
          minimum = parser.getOptionalValue(json, "minimum").map(_.as[Int]),
          maximum = parser.getOptionalValue(json, "maximum").map(_.as[Int]),
          format = parser.getOptionalValue(json, "format").map(_.as[JsString].value),
          example = parser.getOptionalValue(json, "example").map(_.as[JsString].value))
  }
}
