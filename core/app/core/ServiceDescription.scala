package core

import play.api.libs.json._

object ServiceDescription {

  def apply(apiJson: String): ServiceDescription = {
    val jsValue = Json.parse(apiJson)
    ServiceDescription(jsValue)
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
                 references: Option[Reference] = None,
                 default: Option[String] = None,
                 example: Option[String] = None,
                 minimum: Option[Int] = None,
                 maximum: Option[Int] = None)

case class Reference(resource: String, field: String) {

  lazy val label = s"$resource.$field"

}


case class Response(code: Int,
                    resource: Option[String] = None,
                    multiple: Boolean = false)

object Resource {

  def parse(name: String, value: JsObject): Resource = {
     val path = (value \ "path").asOpt[String].getOrElse( s"/${name}" )
     val description = (value \ "description").asOpt[String]

     val fields = (value \ "fields").asOpt[JsArray] match {

       case None => Seq.empty

       case Some(a: JsArray) => {
         a.value.map { json => Field.parse(json.as[JsObject]) }
       }

     }

     val operations = (value \ "operations").asOpt[JsArray] match {

       case None => Seq.empty

       case Some(a: JsArray) => {
         a.value.map { json =>

           val parameters = (json \ "parameters").asOpt[JsArray] match {
             case None => Seq.empty
             case Some(a: JsArray) => {
               a.value.map { data => Field.parse(data.as[JsObject]) }
             }
           }

           Operation(method = (json \ "method").as[String],
                     path = (json \ "path").asOpt[String],
                     description = (json \ "description").asOpt[String],
                     response = Response.parse(json.as[JsObject]),
                     parameters = parameters)
         }
       }

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
          references = (json \ "references").asOpt[String].map { Reference(_) },
          required = (json \ "required").asOpt[Boolean].getOrElse(true),
          default = asOptString(json, "default"),
          minimum = (json \ "minimum").asOpt[Int],
          maximum = (json \ "maximum").asOpt[Int],
          format = (json \ "format").asOpt[String],
          example = asOptString(json, "example"))
  }

  private def asOptString(json: JsValue, field: String): Option[String] = {
    (json \ field) match {
      case (_: JsUndefined) => None
      case (v: JsValue) => Some(v.toString)
    }
  }


}

object Reference {

  def apply(value: String): Reference = {
    val parts = value.split("\\.")
    require(parts.length == 2,
            s"Invalid reference[${value}]. Expected <resource name>.<field name>")
    Reference(parts.head, parts.last)
  }

}
