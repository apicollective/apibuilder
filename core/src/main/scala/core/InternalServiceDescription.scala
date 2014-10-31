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
    (json \ "models").asOpt[JsValue] match {
      case Some(models: JsObject) => {
        models.fields.flatMap { v =>
          v match {
            case(key, value) => value.asOpt[JsObject].map(InternalModel(key, _))
          }
        }
      }
      case _ => Seq.empty
    }
  }

  lazy val enums: Seq[InternalEnum] = {
    (json \ "enums").asOpt[JsValue] match {
      case Some(enums: JsObject) => {
        enums.fields.flatMap { v =>
          v match {
            case(key, value) => value.asOpt[JsObject].map(InternalEnum(key, _))
          }
        }
      }
      case _ => Seq.empty
    }
  }

  lazy val headers: Seq[InternalHeader] = {
    (json \ "headers").asOpt[JsArray].map(_.value).getOrElse(Seq.empty).flatMap { el =>
      el match {
        case o: JsObject => {
          val datatype = JsonUtil.asOptString(o, "type").map(InternalParsedDatatype(_))

          Some(
            InternalHeader(
              name = JsonUtil.asOptString(o, "name"),
              datatype = datatype,
              required = JsonUtil.asOptBoolean(o \ "required").getOrElse(true),
              description = JsonUtil.asOptString(o, "description"),
              default = JsonUtil.asOptString(o, "default")
            )
          )
        }
        case _ => None
      }
    }
  }

  lazy val resources: Seq[InternalResource] = {
    (json \ "resources").asOpt[JsValue] match {
      case None => Seq.empty

      // Array is deprecated - JsObject is preferred
      case Some(resources: JsArray) => {
        resources.value.flatMap { v =>
          v match {
            case o: JsObject => {
              val modelName = (o \ "model").asOpt[String]
              Some(InternalResource(modelName, models, o))
            }
            case _ => None
          }
        }
      }

      case Some(resources: JsObject) => {
        resources.fields.map { v =>
          v match {
            case(modelName, value) => InternalResource(Some(modelName), models, value.as[JsObject])
          }
        }
      }

      case _ => Seq.empty
    }
  }

  lazy val typeResolver = TypeResolver(
    enumNames = enums.map(_.name),
    modelNames = models.map(_.name)
  )
}

case class InternalModel(name: String,
                         plural: String,
                         description: Option[String],
                         fields: Seq[InternalField])

case class InternalEnum(
  name: String,
  description: Option[String],
  values: Seq[InternalEnumValue]
)

case class InternalEnumValue(
  name: Option[String],
  description: Option[String]
)

case class InternalHeader(
  name: Option[String],
  datatype: Option[InternalParsedDatatype],
  required: Boolean,
  description: Option[String],
  default: Option[String]
)

case class InternalResource(modelName: Option[String],
                            path: String,
                            operations: Seq[InternalOperation])

case class InternalOperation(method: Option[String],
                             path: String,
                             description: Option[String],
                             namedPathParameters: Seq[String],
                             parameters: Seq[InternalParameter],
                             body: Option[InternalParsedDatatype],
                             responses: Seq[InternalResponse],
                             warnings: Seq[String] = Seq.empty) {

  lazy val label = "%s %s".format(method.getOrElse(""), path)

}

case class InternalField(
  name: Option[String] = None,
  datatype: Option[InternalParsedDatatype] = None,
  description: Option[String] = None,
  required: Boolean = true,
  default: Option[String] = None,
  example: Option[String] = None,
  minimum: Option[Long] = None,
  maximum: Option[Long] = None,
  warnings: Seq[String] = Seq.empty
)

case class InternalParameter(
  name: Option[String] = None,
  datatype: Option[InternalParsedDatatype] = None,
  description: Option[String] = None,
  required: Boolean,
  default: Option[String] = None,
  example: Option[String] = None,
  minimum: Option[Long] = None,
  maximum: Option[Long] = None
)


case class InternalResponse(
  code: String,
  datatype: Option[InternalParsedDatatype] = None,
  warnings: Seq[String] = Seq.empty
) {

  lazy val datatypeLabel: Option[String] = datatype.map { dt =>
    dt match {
      case InternalParsedDatatype(TypeContainer.List, name) => s"[$name]"
      case InternalParsedDatatype(TypeContainer.Map, name) => s"map[$name]"
      case InternalParsedDatatype(TypeContainer.Singleton, name) => name
    }
  }

}

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

object InternalEnum {

  def apply(name: String, value: JsObject): InternalEnum = {
    val description = (value \ "description").asOpt[String]
    val values = (value \ "values").asOpt[JsArray] match {
       case None => Seq.empty
       case Some(a: JsArray) => {
         a.value.map { json =>
           InternalEnumValue(
             name = (json \ "name").asOpt[String],
             description = (json \ "description").asOpt[String]
           )
         }
       }
    }

    InternalEnum(
      name = name,
      description = description,
      values = values
    )
  }

}

object InternalResource {

  def apply(modelName: Option[String], models: Seq[InternalModel], value: JsObject): InternalResource = {
    val path = (value \ "path").asOpt[String].getOrElse {
      models.find(m => Some(m.name) == modelName) match {
        case Some(model: InternalModel) => "/" + model.plural
        case None => "/"
      }
    }

    val operations = (value \ "operations").asOpt[JsArray] match {
      case None => Seq.empty
      case Some(a: JsArray) => {
        a.value.map { json => InternalOperation(path, json.as[JsObject]) }
      }
    }

    InternalResource(modelName = modelName,
                     path = path,
                     operations = operations)
  }

}

object InternalOperation {

  private val NoContentResponse = InternalResponse(code = "204", datatype = Some(InternalParsedDatatype("unit")))

  def apply(resourcePath: String, json: JsObject): InternalOperation = {
    val path = resourcePath + (json \ "path").asOpt[String].getOrElse("")
    val namedPathParameters = Util.namedParametersInPath(path)
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
          responses.fields.map {
            case(code, value) => {
              value match {
                case o: JsObject => InternalResponse(code, o)
                case other => {
                  InternalResponse(
                    code = code,
                    warnings = Seq("Value must be a JsObject")
                  )
                }
              }
            }
          }
        }
      }
    }

    var warnings: Seq[String] = Seq.empty

    val body = (json \ "body") match {
      case o: JsObject => (o \ "type").asOpt[String]
      case u: JsUndefined => None
      case v: JsValue => {
        warnings = Seq(s"""body declaration must be an object, e.g. { "type": $v }""")
        None
      }
    }

    InternalOperation(method = (json \ "method").asOpt[String].map(_.toUpperCase),
                      path = path,
                      body = body.map( InternalParsedDatatype(_) ),
                      description = (json \ "description").asOpt[String],
                      responses = responses,
                      namedPathParameters = namedPathParameters,
                      parameters = parameters,
                      warnings = warnings)
  
  }
}

object InternalResponse {

  def apply(code: String, json: JsObject): InternalResponse = {
    InternalResponse(
      code = code,
      datatype = (json \ "type").asOpt[String].map( InternalParsedDatatype(_) )
    )
  }
}

object InternalField {

  def apply(json: JsObject): InternalField = {
    val parsedDatatype = (json \ "type").asOpt[String].map( InternalParsedDatatype(_) )

    val warnings = if (JsonUtil.hasKey(json, "enum") || JsonUtil.hasKey(json, "values")) {
      Seq("Enumerations are now first class objects and must be defined in an explicit enum section")
    } else {
      Seq.empty
    }

    InternalField(
      name = (json \ "name").asOpt[String],
      datatype = parsedDatatype,
      description = (json \ "description").asOpt[String],
      required = JsonUtil.asOptBoolean(json \ "required").getOrElse(true),
      default = JsonUtil.asOptString(json, "default"),
      minimum = (json \ "minimum").asOpt[Long],
      maximum = (json \ "maximum").asOpt[Long],
      example = JsonUtil.asOptString(json, "example"),
      warnings = warnings
    )
  }

}

object InternalParameter {

  def apply(json: JsObject): InternalParameter = {
    InternalParameter(name = (json \ "name").asOpt[String],
                      datatype = (json \ "type").asOpt[String].map( InternalParsedDatatype(_) ),
                      description = (json \ "description").asOpt[String],
                      required = JsonUtil.asOptBoolean(json \ "required").getOrElse(true),
                      default = JsonUtil.asOptString(json, "default"),
                      minimum = (json \ "minimum").asOpt[Long],
                      maximum = (json \ "maximum").asOpt[Long],
                      example = JsonUtil.asOptString(json, "example"))
  }

}

/**
 * Parse numbers and string json values as strings
 */
private[core] object JsonUtil {

  def asOptString(json: JsValue, field: String): Option[String] = {
    val value = (json \ field)
    asOptString(value)
  }

  def asOptString(value: JsValue): Option[String] = {
    value match {
      case (_: JsUndefined) => None
      case (v: JsString) => Some(v.value)
      case (v: JsValue) => Some(v.toString)
    }
  }

  def asOptBoolean(value: JsValue): Option[Boolean] = {
    asOptString(value).map(_ == "true")
  }

  def hasKey(json: JsValue, field: String): Boolean = {
    (json \ field) match {
      case (_: JsUndefined) => false
      case _ => true
    }
  }
}

private[core] case class InternalParsedDatatype(
  container: TypeContainer,
  name: String
)

private[core] object InternalParsedDatatype {

  private val ListRx = "^\\[(.*)\\]$".r
  private val MapRx = "^\\map[(.*)\\]$".r

  def apply(value: String): InternalParsedDatatype = {
    value match {
      case ListRx(name) => InternalParsedDatatype(TypeContainer.List, name)
      case MapRx(name) => InternalParsedDatatype(TypeContainer.Map, name)
      case _ => InternalParsedDatatype(TypeContainer.Singleton, value)
    }
  }

}
