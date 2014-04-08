package core

import play.api.libs.json._

object ServiceDescription {

  def apply(apiJson: String): ServiceDescription = {
    val jsValue = Json.parse(apiJson)
    ServiceDescription(jsValue)
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
      (sd.json \ field).asOpt[JsValue] match {
        case None => Some(s"Missing field named[$field]")
        case Some(_) => None
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
case class ServiceDescription(json: JsValue) {

  lazy val resources: Seq[Resource] = {
    (json \ "resources").as[JsObject].fields.map { v =>
      v match {
        case(key, value) => Resource.parse(key, value.as[JsObject])
      }
    }
  }

  lazy val baseUrl = (json \ "base_url").as[String]
  lazy val basePath = (json \ "base_path").asOpt[String]
  lazy val name = (json \ "name").as[String]
  lazy val description = (json \ "description").asOpt[String]

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

  def parse(name: String, value: JsObject): Resource = {
     val path = (value \ "path").asOpt[String].getOrElse( s"/${name}" )
     val description = (value \ "description").asOpt[String]
     val fields = (value \ "fields").as[JsArray].value.map { json => Field.parse(json.as[JsObject]) }

     val operations = (value \ "operations").as[JsArray].value.map { json =>
      Operation(method = (json \ "method").as[String],
                path = (json \ "path").asOpt[String],
                description = (json \ "description").asOpt[String],
                response = Response.parse(json.as[JsObject]),
                parameters = (json \ "parameters").as[JsArray].value.map { data => Field.parse(data.as[JsObject]) })
    }

    Resource(name = name,
             path = path,
             description = description,
             fields = fields,
             operations = operations)
  }

}

object Response {

  def parse(json: JsObject): Response = {
    (json \ "response_code").asOpt[Int] match {

      case Some(code: Int) => {
        Response(code = code)
      }

      case None => {
        (json \ "response").asOpt[JsValue] match {

          case None => {
            sys.error("Missing response. Must contain either 'response' or 'response_code' key")
          }

          case Some(v: JsArray) => {
            assert(v.value.size == 1,
                   "When an array, response must contain exactly 1 element: %s".format(v.value.mkString(", ")))
            Response(code = 200,
                     resource = Some(v.value.head.as[JsString].value),
                     multiple = true)
          }

          case Some(v: JsString) => {
            Response(code = 200,
                     resource = Some(v.value))
          }

          case Some(v: Any) => {
            sys.error(s"Could not parse response: $v")
          }
        }

      }

    }
  }

}


object Field {

  def parse(json: JsObject): Field = {
    Field(name = (json \ "name").as[String],
          dataType = (json \ "type").as[String],
          description = (json \ "description").asOpt[String],
          references = (json \ "references").asOpt[String],
          required = (json \ "required").asOpt[Boolean].getOrElse(true),
          default = (json \ "default").asOpt[String],
          minimum = (json \ "minimum").asOpt[Int],
          maximum = (json \ "maximum").asOpt[Int],
          format = (json \ "format").asOpt[String],
          example = (json \ "example").asOpt[String])
  }
}
