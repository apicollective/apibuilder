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

  lazy val name = (json \ "name").asOpt[String]
  lazy val baseUrl = (json \ "base_url").asOpt[String]
  lazy val basePath = (json \ "base_path").asOpt[String]
  lazy val description = (json \ "description").asOpt[String]

  lazy val models: Seq[InternalModel] = {
    (json \ "models").asOpt[JsObject] match {
      case None => Seq.empty
      case Some(models: JsObject) => {
        models.fields.map { v =>
          v match {
            case(key, value) => InternalModel(key, value.as[JsObject])
          }
        }
      }
    }
  }

  lazy val operations: Seq[InternalOperation] = {
    (json \ "operations").asOpt[JsObject] match {
      case None => Seq.empty
      case Some(operations: JsObject) => {
        operations.fields.flatMap { v =>
          v match {
            case(key, value) => {
              val items = value match {
                case a: JsArray => a.value
                case _: JsValue => Seq.empty
              }
              items.flatMap { _.asOpt[JsObject].map { o => InternalOperation(key, o) } }
            }
          }
        }
      }
    }
  }

}

case class InternalModel(name: String,
                         plural: String,
                         description: Option[String],
                         fields: Seq[InternalField])

case class InternalOperation(resourceName: String,
                             method: Option[String],
                             path: Option[String],
                             description: Option[String],
                             namedParameters: Seq[String],
                             parameters: Seq[InternalField],
                             responses: Seq[InternalResponse])

case class InternalField(name: Option[String] = None,
                         datatype: Option[InternalParsedDatatype] = None,
                         description: Option[String] = None,
                         required: Boolean = true,
                         multiple: Boolean = false,
                         references: Option[InternalReference] = None,
                         default: Option[String] = None,
                         example: Option[String] = None,
                         minimum: Option[Long] = None,
                         maximum: Option[Long] = None)


case class InternalResponse(code: String,
                            datatype: Option[InternalParsedDatatype] = None,
                            fields: Option[Set[String]] = None)

object InternalModel {

  def apply(name: String, value: JsObject): InternalModel = {
     val description = (value \ "description").asOpt[String]
     val plural: String = (value \ "plural").asOpt[String].getOrElse( Text.pluralize(name) )

     val fields = (value \ "fields").asOpt[JsArray] match {

       case None => Seq.empty

       case Some(a: JsArray) => {
         a.value.map { json => InternalField(json.as[JsObject]) }
       }

     }


    InternalModel(name = name,
                  plural = plural,
                  description = description,
                  fields = fields)
  }

}

object InternalOperation {

  private val NoContentResponse = InternalResponse(code = "204", datatype = Some(InternalParsedDatatype(Datatype.Unit.name, false)))

  def apply(resourceName: String, json: JsObject): InternalOperation = {
    val opPath = (json \ "path").asOpt[String]
    val namedParameters = Util.namedParametersInPath(opPath.getOrElse(""))
    val parameters = (json \ "parameters").asOpt[JsArray] match {
      case None => Seq.empty
      case Some(a: JsArray) => {
        a.value.map { data => InternalField(data.as[JsObject]) }
      }
    }

    val responses: Seq[InternalResponse] = {
      (json \ "responses").asOpt[JsObject] match {
        case None => {
          Seq(NoContentResponse)
        }

        case Some(responses: JsObject) => {
          responses.fields.map { v =>
            v match {
              case(code, value) => InternalResponse(code, value.as[JsObject])
            }
                              }
        }
      }
    }

    InternalOperation(resourceName = resourceName,
                      method = (json \ "method").asOpt[String],
                      path = opPath,
                      description = (json \ "description").asOpt[String],
                      responses = responses,
                      namedParameters = namedParameters,
                      parameters = parameters)
  }

}

object InternalResponse {

  def apply(code: String, json: JsObject): InternalResponse = {
    val datatype = (json \ "type").asOpt[String].map( InternalParsedDatatype(_) )
    val fields = (json \ "fields").asOpt[Set[String]]
    InternalResponse(code = code, datatype = datatype, fields = fields)
  }
}

object InternalField {

  def apply(json: JsObject): InternalField = {
    InternalField(name = (json \ "name").asOpt[String],
                  datatype = (json \ "type").asOpt[String].map( InternalParsedDatatype(_) ),
                  description = (json \ "description").asOpt[String],
                  references = (json \ "references").asOpt[String].map { InternalReference(_) },
                  required = (json \ "required").asOpt[Boolean].getOrElse(true),
                  multiple = (json \ "multiple").asOpt[Boolean].getOrElse(false),
                  default = (json \ "default").asOpt[String],
                  minimum = (json \ "minimum").asOpt[Long],
                  maximum = (json \ "maximum").asOpt[Long],
                  example = (json \ "example").asOpt[String])
  }

}

private[core] case class InternalReference(label: String, modelPlural: Option[String], fieldName: Option[String])

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

private[core] case class InternalParsedDatatype(name: String, multiple: Boolean)

private[core] object InternalParsedDatatype {

  def apply(value: String): InternalParsedDatatype = {
    // TODO: Why do we have to drop the first element?
    val letters = value.split("").drop(1)

    if (letters.head == "[" && letters.last == "]") {
      InternalParsedDatatype(value.slice(1, letters.length - 1), true)
    } else {
      InternalParsedDatatype(value, false)
    }
  }

}
