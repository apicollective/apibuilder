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
                             path: String,
                             description: Option[String],
                             namedParameters: Seq[String],
                             parameters: Seq[InternalParameter],
                             responses: Seq[InternalResponse]) {

  lazy val label = "%s %s".format(method.getOrElse(""), path)

}

sealed trait InternalFieldType
case class InternalNamedFieldType(name: String) extends InternalFieldType
case class InternalReferenceFieldType(referencedModelName: String) extends InternalFieldType

case class InternalField(name: Option[String] = None,
                         fieldtype: Option[InternalFieldType] = None,
                         description: Option[String] = None,
                         required: Boolean = true,
                         multiple: Boolean = false,
                         default: Option[String] = None,
                         example: Option[String] = None,
                         minimum: Option[Long] = None,
                         maximum: Option[Long] = None)

case class InternalParameter(name: Option[String] = None,
                             paramtype: Option[String] = None,
                             description: Option[String] = None,
                             required: Boolean = true,
                             multiple: Boolean = false,
                             default: Option[String] = None,
                             example: Option[String] = None,
                             minimum: Option[Long] = None,
                             maximum: Option[Long] = None)


case class InternalResponse(code: String,
                            datatype: Option[String] = None,
                            multiple: Boolean = false,
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

  private val NoContentResponse = InternalResponse(code = "204", datatype = Some(Datatype.UnitType.name))

  def apply(resourceName: String, json: JsObject): InternalOperation = {
    val opPath = (json \ "path").asOpt[String].getOrElse(s"/${resourceName}")
    val namedParameters = Util.namedParametersInPath(opPath)
    val parameters = (json \ "parameters").asOpt[JsArray] match {
      case None => Seq.empty
      case Some(a: JsArray) => {
        a.value.map { data => InternalParameter(data.as[JsObject]) }
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
    val parsedDatatype = (json \ "type").asOpt[String].map( InternalParsedDatatype(_) )
    val fields = (json \ "fields").asOpt[Set[String]]
    InternalResponse(code = code,
                     datatype = parsedDatatype.map(_.name),
                     multiple = parsedDatatype.map(_.multiple).getOrElse(false),
                     fields = fields)
  }
}

object InternalField {

  def apply(json: JsObject): InternalField = {
    val parsedDatatype = (json \ "type").asOpt[String].map( InternalParsedDatatype(_) )

    InternalField(name = (json \ "name").asOpt[String],
                  fieldtype = parsedDatatype.map(_.toInternalFieldType),
                  description = (json \ "description").asOpt[String],
                  required = (json \ "required").asOpt[Boolean].getOrElse(true),
                  multiple = parsedDatatype.map(_.multiple).getOrElse(false),
                  default = (json \ "default").asOpt[String],
                  minimum = (json \ "minimum").asOpt[Long],
                  maximum = (json \ "maximum").asOpt[Long],
                  example = (json \ "example").asOpt[String])
  }

}

object InternalParameter {

  def apply(json: JsObject): InternalParameter = {
    val dt = (json \ "type").asOpt[String].map( InternalParsedDatatype(_) )

    InternalParameter(name = (json \ "name").asOpt[String],
                      paramtype = dt.map(_.name),
                      description = (json \ "description").asOpt[String],
                      required = (json \ "required").asOpt[Boolean].getOrElse(true),
                      multiple = dt.map(_.multiple).getOrElse(false),
                      default = (json \ "default").asOpt[String],
                      minimum = (json \ "minimum").asOpt[Long],
                      maximum = (json \ "maximum").asOpt[Long],
                      example = (json \ "example").asOpt[String])
  }

}

private[core] case class InternalParsedDatatype(name: String, multiple: Boolean, referencedModelName: Option[String]) {

  def toInternalFieldType: InternalFieldType = {
    referencedModelName match {
      case None => InternalNamedFieldType(name)
      case Some(referencedName: String) => InternalReferenceFieldType(referencedName)
    }
  }

}

private[core] object InternalParsedDatatype {

  private val ArrayRx = "^\\[(.*)\\]$".r

  def apply(value: String): InternalParsedDatatype = {
    value match {
      case ArrayRx(word) => parseReference(word, true)
      case _ => parseReference(value, false)
    }
  }

  private val ReferencesRx = "^reference\\[(.+)\\]$".r

  private def parseReference(value: String, multiple: Boolean): InternalParsedDatatype = {
    value match {
      case ReferencesRx(modelName) => {
        InternalParsedDatatype("reference", multiple, Some(modelName))
      }
      case _ => {
        println(" - not a refrence")
        InternalParsedDatatype(value, multiple, None)
      }
    }
  }

}
