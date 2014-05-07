package core

import play.api.libs.json._

/**
 * Just parses json with minimal validation - build to provide a way to
 * generate meaningful validation messages back to user. Basic flow
 *
 * JSON => InternalServiceDescription => ServiceDescription
 *
 */
private[core] object InternalServiceDescription {

  def apply(apiJson: String): InternalServiceDescription = {
    val jsValue = Json.parse(apiJson)
    InternalServiceDescription(jsValue)
  }

}

private[core] case class InternalServiceDescription(json: JsValue) {

  lazy val resources: Seq[InternalResource] = {
    (json \ "resources").as[JsObject].fields.map { v =>
      v match {
        case(key, value) => InternalResource(key, value.as[JsObject])
      }
    }
  }

  lazy val baseUrl = (json \ "base_url").asOpt[String]
  lazy val basePath = (json \ "base_path").asOpt[String]
  lazy val name = (json \ "name").asOpt[String]
  lazy val description = (json \ "description").asOpt[String]

}

case class InternalResource(name: String,
                            path: String,
                            description: Option[String],
                            fields: Seq[InternalField],
                            operations: Seq[InternalOperation])

case class InternalOperation(method: Option[String],
                             path: Option[String],
                             description: Option[String],
                             parameters: Seq[InternalField],
                             responses: Seq[InternalResponse])

case class InternalField(name: Option[String],
                         datatype: Option[String],
                         description: Option[String] = None,
                         required: Boolean = true,
                         multiple: Boolean = false,
                         format: Option[String] = None,
                         references: Option[InternalReference] = None,
                         default: Option[String] = None,
                         example: Option[String] = None,
                         minimum: Option[Long] = None,
                         maximum: Option[Long] = None)


case class InternalResponse(code: String,
                            datatype: Option[String],
                            fields: Option[Set[String]])

object InternalResource {

  def apply(name: String, value: JsObject): InternalResource = {
     val path = (value \ "path").asOpt[String].getOrElse( s"/${name}" )
     val description = (value \ "description").asOpt[String]

     val fields = (value \ "fields").asOpt[JsArray] match {

       case None => Seq.empty

       case Some(a: JsArray) => {
         a.value.map { json => InternalField(json.as[JsObject]) }
       }

     }

     val operations = (value \ "operations").asOpt[JsArray] match {

       case None => Seq.empty

       case Some(a: JsArray) => {
         a.value.map { json =>

           // TODO: Add in path parameters
           val parameters = (json \ "parameters").asOpt[JsArray] match {
             case None => Seq.empty
             case Some(a: JsArray) => {
               a.value.map { data => InternalField(data.as[JsObject]) }
             }
           }

           val responses = (json \ "responses").asOpt[JsArray] match {
             case None => Seq.empty
             case Some(a: JsArray) => {
               a.value.flatMap { data => InternalResponse(data.as[JsObject]) }
             }
           }

           InternalOperation(method = (json \ "method").asOpt[String],
                             path = (json \ "path").asOpt[String],
                             description = (json \ "description").asOpt[String],
                             responses = responses,
                             parameters = parameters)
         }
       }

    }

    InternalResource(name = name,
                     path = path,
                     description = description,
                     fields = fields,
                     operations = operations)
  }

}

object InternalResponse {

  def apply(json: JsObject): Seq[InternalResponse] = {
    json.fields.map { v =>
      v match {
        case(code, data) => {
          val datatype = (json \ "type").asOpt[String]
          val fields = (json \ "fields").asOpt[Set[String]]
          InternalResponse(code = code, datatype = datatype, fields = fields)
        }
      }
    }
  }
}

object InternalField {

  def apply(json: JsObject): InternalField = {
    InternalField(name = (json \ "name").asOpt[String],
                  datatype = (json \ "type").asOpt[String],
                  description = (json \ "description").asOpt[String],
                  references = (json \ "references").asOpt[String].map { InternalReference(_) },
                  required = (json \ "required").asOpt[Boolean].getOrElse(true),
                  multiple = (json \ "multiple").asOpt[Boolean].getOrElse(false),
                  default = (json \ "default").asOpt[String],
                  minimum = (json \ "minimum").asOpt[Long],
                  maximum = (json \ "maximum").asOpt[Long],
                  format = (json \ "format").asOpt[String],
                  example = (json \ "example").asOpt[String])
  }

}

private[core] case class InternalReference(label: String, resource: Option[String], field: Option[String])

private[core] object InternalReference {

  def apply(value: String): InternalReference = {
    val parts = value.split("\\.", 2)

    if (parts.length == 0) {
      InternalReference(value, None, None)

    } else if (parts.length == 1) {
      InternalReference(value, Some(parts.head), None)

    } else {
      InternalReference(value, Some(parts.head), Some(parts.last))
    }
  }

}
