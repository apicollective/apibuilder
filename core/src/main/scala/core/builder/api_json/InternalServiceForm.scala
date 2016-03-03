package builder.api_json

import builder.JsonUtil
import core.{ServiceFetcher, Util}
import lib.{Primitives, Text}
import play.api.libs.json._

/**
 * Just parses json with minimal validation - build to provide a way to
 * generate meaningful validation messages back to user. Basic flow
 *
 * JSON => InternalService => Service
 *
 */
private[api_json] case class InternalServiceForm(
  json: JsValue,
  fetcher: ServiceFetcher
) {

  lazy val apidoc = (json \ "apidoc").asOpt[JsValue].map { InternalApidocForm(_) }
  lazy val name = JsonUtil.asOptString(json \ "name")
  lazy val key = JsonUtil.asOptString(json \ "key")
  lazy val namespace = JsonUtil.asOptString(json \ "namespace")
  lazy val baseUrl = JsonUtil.asOptString(json \ "base_url")
  lazy val basePath = JsonUtil.asOptString(json \ "base_path")

  lazy val description = JsonUtil.asOptString(json \ "description")
  lazy val info = (json \ "info").asOpt[JsValue].map { InternalInfoForm(_) }

  lazy val imports: Seq[InternalImportForm] = {
    (json \ "imports").asOpt[JsArray] match {
      case None => Seq.empty
      case Some(values) => {
        values.value.flatMap { _.asOpt[JsObject].map { InternalImportForm(_) } }
      }
    }
  }

  lazy val unions: Seq[InternalUnionForm] = {
    (json \ "unions").asOpt[JsValue] match {
      case Some(unions: JsObject) => {
        unions.fields.flatMap { v =>
          v match {
            case(key, value) => value.asOpt[JsObject].map(InternalUnionForm(key, _))
          }
        }
      }
      case _ => Seq.empty
    }
  }

  lazy val models: Seq[InternalModelForm] = {
    (json \ "models").asOpt[JsValue] match {
      case Some(models: JsObject) => {
        models.fields.flatMap { v =>
          v match {
            case(key, value) => value.asOpt[JsObject].map(InternalModelForm(key, _))
          }
        }
      }
      case _ => Seq.empty
    }
  }

  lazy val enums: Seq[InternalEnumForm] = {
    (json \ "enums").asOpt[JsValue] match {
      case Some(enums: JsObject) => {
        enums.fields.flatMap { v =>
          v match {
            case(key, value) => value.asOpt[JsObject].map(InternalEnumForm(key, _))
          }
        }
      }
      case _ => Seq.empty
    }
  }

  lazy val headers: Seq[InternalHeaderForm] = {
    (json \ "headers").asOpt[JsArray].map(_.value).getOrElse(Seq.empty).flatMap { el =>
      el match {
        case o: JsObject => {
          val datatype = InternalDatatype(o)

          val headerName = JsonUtil.asOptString(o \ "name")
          Some(
            InternalHeaderForm(
              name = headerName,
              datatype = datatype,
              required = datatype.map(_.required).getOrElse(true),
              description = JsonUtil.asOptString(o \ "description"),
              deprecation = InternalDeprecationForm.fromJsValue(o),
              default = JsonUtil.asOptString(o \ "default"),
              attributes = InternalAttributeForm.attributesFromJson((o \ "attributes").asOpt[JsArray]),
              warnings = JsonUtil.validate(
                o,
                strings = Seq("name", "type"),
                optionalBooleans = Seq("required"),
                optionalObjects = Seq("deprecation"),
                optionalStrings = Seq("default", "description"),
                prefix = Some(s"Header[${headerName.getOrElse("")}]".trim)
              )
            )
          )
        }
        case _ => None
      }
    }
  }

  lazy val resources: Seq[InternalResourceForm] = {
    (json \ "resources").asOpt[JsValue] match {
      case None => Seq.empty

      case Some(resources: JsObject) => {
        resources.fields.flatMap { v =>
          v match {
            case(typeName, value) => {
              value.asOpt[JsObject].map(InternalResourceForm(typeName, models, enums, unions, _))
            }
          }
        }
      }

      case _ => Seq.empty
    }
  }

  lazy val attributes: Seq[InternalAttributeForm] = InternalAttributeForm.attributesFromJson((json \ "attributes").asOpt[JsArray])

  lazy val typeResolver = TypeResolver(
    defaultNamespace = namespace,
    RecursiveTypesProvider(this)
  )
}

case class InternalImportForm(
  uri: Option[String],
  warnings: Seq[String]
)

case class InternalApidocForm(
  version: Option[String]
)

case class InternalInfoForm(
  contact: Option[InternalInfoContactForm],
  license: Option[InternalInfoLicenseForm],
  warnings: Seq[String]
)

case class InternalInfoContactForm(
  name: Option[String],
  email: Option[String],
  url: Option[String]
)

case class InternalInfoLicenseForm(
  name: Option[String],
  url: Option[String]
)

case class InternalDeprecationForm(
  description: Option[String]
)

case class InternalModelForm(
  name: String,
  plural: String,
  description: Option[String],
  deprecation: Option[InternalDeprecationForm],
  fields: Seq[InternalFieldForm],
  attributes: Seq[InternalAttributeForm],
  warnings: Seq[String]
)

case class InternalEnumForm(
  name: String,
  plural: String,
  description: Option[String],
  deprecation: Option[InternalDeprecationForm],
  values: Seq[InternalEnumValueForm],
  attributes: Seq[InternalAttributeForm],
  warnings: Seq[String]
)

case class InternalEnumValueForm(
  name: Option[String],
  description: Option[String],
  deprecation: Option[InternalDeprecationForm],
  attributes: Seq[InternalAttributeForm],
  warnings: Seq[String]
)

case class InternalUnionForm(
  name: String,
  plural: String,
  discriminator: Option[String],
  description: Option[String],
  deprecation: Option[InternalDeprecationForm],
  types: Seq[InternalUnionTypeForm],
  attributes: Seq[InternalAttributeForm],
  warnings: Seq[String]
)

case class InternalUnionTypeForm(
  datatype: Option[InternalDatatype] = None,
  description: Option[String],
  deprecation: Option[InternalDeprecationForm],
  attributes: Seq[InternalAttributeForm],
  warnings: Seq[String]
)

case class InternalHeaderForm(
  name: Option[String],
  datatype: Option[InternalDatatype],
  required: Boolean,
  description: Option[String],
  deprecation: Option[InternalDeprecationForm],
  default: Option[String],
  attributes: Seq[InternalAttributeForm],
  warnings: Seq[String]
)

case class InternalResourceForm(
  datatype: InternalDatatype,
  description: Option[String],
  deprecation: Option[InternalDeprecationForm],
  path: Option[String],
  operations: Seq[InternalOperationForm],
  attributes: Seq[InternalAttributeForm],
  warnings: Seq[String] = Seq.empty
)

case class InternalOperationForm(
  method: Option[String],
  path: Option[String],
  description: Option[String],
  deprecation: Option[InternalDeprecationForm],
  namedPathParameters: Seq[String],
  parameters: Seq[InternalParameterForm],
  body: Option[InternalBodyForm],
  responses: Seq[InternalResponseForm],
  attributes: Seq[InternalAttributeForm],
  warnings: Seq[String] = Seq.empty
) {

  lazy val label = "%s %s".format(method.getOrElse(""), path).trim

}

case class InternalFieldForm(
  name: Option[String] = None,
  datatype: Option[InternalDatatype] = None,
  description: Option[String] = None,
  deprecation: Option[InternalDeprecationForm],
  required: Boolean = true,
  default: Option[String] = None,
  example: Option[String] = None,
  minimum: Option[Long] = None,
  maximum: Option[Long] = None,
  attributes: Seq[InternalAttributeForm],
  warnings: Seq[String] = Seq.empty
)

case class InternalAttributeForm(
  name: Option[String] = None,
  value: Option[JsObject] = None,
  description: Option[String] = None,
  deprecation: Option[InternalDeprecationForm],
  warnings: Seq[String] = Seq.empty
)

case class InternalParameterForm(
  name: Option[String] = None,
  datatype: Option[InternalDatatype] = None,
  location: Option[String] = None,
  description: Option[String] = None,
  deprecation: Option[InternalDeprecationForm],
  required: Boolean,
  default: Option[String] = None,
  example: Option[String] = None,
  minimum: Option[Long] = None,
  maximum: Option[Long] = None,
  warnings: Seq[String] = Seq.empty
)

case class InternalBodyForm(
  datatype: Option[InternalDatatype] = None,
  description: Option[String] = None,
  deprecation: Option[InternalDeprecationForm],
  attributes: Seq[InternalAttributeForm],
  warnings: Seq[String]
)

case class InternalResponseForm(
  code: String,
  datatype: Option[InternalDatatype] = None,
  description: Option[String] = None,
  deprecation: Option[InternalDeprecationForm] = None,
  warnings: Seq[String] = Seq.empty
) {

  lazy val datatypeLabel: Option[String] = datatype.map(_.label)

}

object InternalApidocForm {

  def apply(value: JsValue): InternalApidocForm = {
    InternalApidocForm(
      version = JsonUtil.asOptString(value \ "version")
    )
  }

}

object InternalInfoForm {

  def apply(
    value: JsValue
  ): InternalInfoForm = {
    InternalInfoForm(
      contact = (value \ "contact").asOpt[JsValue].map { o =>
        InternalInfoContactForm(
          name = JsonUtil.asOptString(o \ "name"),
          email = JsonUtil.asOptString(o \ "email"),
          url = JsonUtil.asOptString(o \ "url")
        )
      },
      license = (value \ "license").asOpt[JsValue].map { o =>
        InternalInfoLicenseForm(
          name = JsonUtil.asOptString(o \ "name"),
          url = JsonUtil.asOptString(o \ "url")
        )
      },
      warnings = JsonUtil.validate(
        value,
        optionalObjects = Seq("contact", "license"),
        optionalStrings = Seq("description")
      )
    )
  }

}

object InternalDeprecationForm {

  def apply(value: JsValue): InternalDeprecationForm = {
    InternalDeprecationForm(
      description = JsonUtil.asOptString(value \ "description")
    )
  }

  def fromJsValue(json: JsValue): Option[InternalDeprecationForm] = {
    (json \ "deprecation").asOpt[JsValue].map(InternalDeprecationForm(_))
  }

}

object InternalUnionForm {

  def apply(name: String, value: JsObject): InternalUnionForm = {
    val description = JsonUtil.asOptString(value \ "description")
    val types = (value \ "types").asOpt[JsArray] match {
       case None => Seq.empty
       case Some(a: JsArray) => {
         a.value.flatMap { value =>
           value.asOpt[JsObject].map { json =>
             val typeName = JsonUtil.asOptString(json \ "type").map(InternalDatatype(_))

             InternalUnionTypeForm(
               datatype = typeName,
               description = JsonUtil.asOptString(json \ "description"),
               deprecation = InternalDeprecationForm.fromJsValue(json),
               attributes = InternalAttributeForm.attributesFromJson((value \ "attributes").asOpt[JsArray]),
               warnings = JsonUtil.validate(
                 json,
                 strings = Seq("type"),
                 optionalStrings = Seq("description"),
                 optionalObjects = Seq("deprecation"),
                 prefix = Some(s"Union[$name] type[${typeName.getOrElse("")}]")
               )
             )
           }
         }
       }
    }

    InternalUnionForm(
      name = name,
      plural = JsonUtil.asOptString(value \ "plural").getOrElse( Text.pluralize(name) ),
      discriminator = JsonUtil.asOptString(value \ "discriminator"),
      description = description,
      deprecation = InternalDeprecationForm.fromJsValue(value),
      types = types,
      attributes = InternalAttributeForm.attributesFromJson((value \ "attributes").asOpt[JsArray]),
      warnings = JsonUtil.validate(
        value,
        optionalStrings = Seq("discriminator", "description", "plural"),
        arraysOfObjects = Seq("types"),
        optionalObjects = Seq("deprecation"),
        prefix = Some(s"Union[$name]")
      )
    )
  }

}

object InternalImportForm {

  def apply(value: JsObject): InternalImportForm = {
    InternalImportForm(
      uri = JsonUtil.asOptString(value \ "uri"),
      warnings = JsonUtil.validate(
        value,
        strings = Seq("uri"),
        prefix = Some("Import")
      )
    )
  }

}

object InternalModelForm {

  def apply(name: String, value: JsObject): InternalModelForm = {
    val description = JsonUtil.asOptString(value \ "description")
    val plural: String = JsonUtil.asOptString(value \ "plural").getOrElse( Text.pluralize(name) )

    val fields = (value \ "fields").asOpt[JsArray] match {

      case None => Seq.empty

      case Some(a: JsArray) => {
        a.value.flatMap { _.asOpt[JsObject].map(InternalFieldForm(_)) }
      }

    }

    InternalModelForm(
      name = name,
      plural = plural,
      description = description,
      deprecation = InternalDeprecationForm.fromJsValue(value),
      fields = fields,
      attributes = InternalAttributeForm.attributesFromJson((value \ "attributes").asOpt[JsArray]),
      warnings = JsonUtil.validate(
        value,
        optionalStrings = Seq("description", "plural"),
        arraysOfObjects = Seq("fields"),
        optionalArraysOfObjects = Seq("attributes"),
        optionalObjects = Seq("deprecation"),
        prefix = Some(s"Model[$name]")
      )
    )
  }

}

object InternalEnumForm {

  def apply(name: String, value: JsObject): InternalEnumForm = {
    val description = JsonUtil.asOptString(value \ "description")
    val values = (value \ "values").asOpt[JsArray] match {
       case None => Seq.empty
       case Some(a: JsArray) => {
         a.value.flatMap { value =>
           value.asOpt[JsObject].map { json =>
             val valueName = JsonUtil.asOptString(json \ "name")
             InternalEnumValueForm(
               name = valueName,
               description = JsonUtil.asOptString(json \ "description"),
               deprecation = InternalDeprecationForm.fromJsValue(json),
               attributes = InternalAttributeForm.attributesFromJson((json \ "attributes").asOpt[JsArray]),
               warnings = JsonUtil.validate(
                 json,
                 strings = Seq("name"),
                 optionalStrings = Seq("description"),
                 optionalObjects = Seq("deprecation"),
                 prefix = Some(s"Enum[$name] value[${valueName.getOrElse("")}]")
               )
             )
           }
         }
       }
    }

    InternalEnumForm(
      name = name,
      plural = JsonUtil.asOptString(value \ "plural").getOrElse( Text.pluralize(name) ),
      description = description,
      deprecation = InternalDeprecationForm.fromJsValue(value),
      values = values,
      attributes = InternalAttributeForm.attributesFromJson((value \ "attributes").asOpt[JsArray]),
      warnings = JsonUtil.validate(
        value,
        optionalStrings = Seq("name", "description", "plural"),
        arraysOfObjects = Seq("values"),
        optionalObjects = Seq("deprecation"),
        prefix = Some(s"Enum[$name]")
      )
    )
  }

}

object InternalResourceForm {

  def apply(
    typeName: String,
    models: Seq[InternalModelForm],
    enums: Seq[InternalEnumForm],
    unions: Seq[InternalUnionForm],
    value: JsObject
  ): InternalResourceForm = {
    val path: Option[String] = (value \ "path").asOpt[JsString] match {
      case Some(v) => {
        Some(v.value)
      }
      case None => {
        enums.find(e => e.name == typeName) match {
          case Some(enum) => Some("/" + enum.plural)
          case None => {
            models.find(m => m.name == typeName) match {
              case Some(model) => Some("/" + model.plural)
              case None => {
                unions.find(u => u.name == typeName) match {
                  case Some(union) => Some("/" + union.plural)
                  case None => None
                }
              }
            }
          }
        }
      }
    }

    val operations = (value \ "operations").asOpt[JsArray] match {
      case None => Seq.empty
      case Some(a: JsArray) => {
        a.value.flatMap { _.asOpt[JsObject].map(InternalOperationForm(path, _)) }
      }
    }

    InternalResourceForm(
      datatype = InternalDatatype(typeName),
      description = JsonUtil.asOptString(value \ "description"),
      deprecation = InternalDeprecationForm.fromJsValue(value),
      path = path,
      operations = operations,
      attributes = InternalAttributeForm.attributesFromJson((value \ "attributes").asOpt[JsArray]),
      warnings = JsonUtil.validate(
        value,
        optionalStrings = Seq("path", "description"),
        optionalObjects = Seq("deprecation"),
        arraysOfObjects = Seq("operations")
      )
    )
  }

}

object InternalOperationForm {

  private val NoContentResponse = InternalResponseForm(code = "204", datatype = Some(InternalDatatype("unit")))

  def apply(resourcePath: Option[String], json: JsObject): InternalOperationForm = {
    val operationPath = JsonUtil.asOptString(json \ "path")

    val knownPath = Seq(resourcePath, operationPath).flatten.mkString("/")

    val namedPathParameters = Util.namedParametersInPath(knownPath)
    val parameters = (json \ "parameters").asOpt[JsArray] match {
      case None => Seq.empty
      case Some(a: JsArray) => {
        a.value.flatMap { _.asOpt[JsObject].map(InternalParameterForm(_)) }
      }
    }

    val responses: Seq[InternalResponseForm] = {
      (json \ "responses").asOpt[JsObject] match {
        case None => {
          Seq(NoContentResponse)
        }

        case Some(responses: JsObject) => {
          responses.fields.map {
            case(code, value) => {
              value match {
                case o: JsObject => {
                  InternalResponseForm(code, o)
                }
                case other => {
                  InternalResponseForm(
                    code = code,
                    warnings = Seq("value must be an object")
                  )
                }
              }
            }
          }
        }
      }
    }

    val body = (json \ "body").asOpt[JsObject].map { o =>
      InternalBodyForm(
        datatype = JsonUtil.asOptString(o \ "type").map(InternalDatatype(_)),
        description = JsonUtil.asOptString(o \ "description"),
        deprecation = InternalDeprecationForm.fromJsValue(o),
        attributes = InternalAttributeForm.attributesFromJson((o \ "attributes").asOpt[JsArray]),
        warnings = JsonUtil.validate(
          o,
          strings = Seq("type"),
          optionalStrings = Seq("description"),
          optionalObjects = Seq("deprecation")
        )
      )
    }

    InternalOperationForm(
      method = JsonUtil.asOptString(json \ "method").map(_.toUpperCase),
      path = operationPath,
      body = body,
      description = JsonUtil.asOptString(json \ "description"),
      deprecation = InternalDeprecationForm.fromJsValue(json),
      responses = responses,
      namedPathParameters = namedPathParameters,
      parameters = parameters,
      attributes = InternalAttributeForm.attributesFromJson((json \ "attributes").asOpt[JsArray]),
      warnings = JsonUtil.validate(
        json,
        strings = Seq("method"),
        optionalStrings = Seq("description", "path"),
        optionalArraysOfObjects = Seq("parameters"),
        optionalObjects = Seq("body", "responses", "deprecation")
      )
    )
  
  }
}

object InternalResponseForm {

  def apply(code: String, json: JsObject): InternalResponseForm = {
    InternalResponseForm(
      code = code,
      datatype = JsonUtil.asOptString(json \ "type").map(InternalDatatype(_)),
      description = JsonUtil.asOptString(json \ "description"),
      deprecation = InternalDeprecationForm.fromJsValue(json),
      warnings = JsonUtil.validate(
        json,
        strings = Seq("type"),
        optionalStrings = Seq("description"),
        optionalObjects = Seq("deprecation")
      )
    )
  }
}

object InternalFieldForm {

  def apply(json: JsObject): InternalFieldForm = {
    val warnings = if (JsonUtil.hasKey(json, "enum") || JsonUtil.hasKey(json, "values")) {
      Seq("Enumerations are now first class objects and must be defined in an explicit enum section")
    } else {
      Seq.empty
    }

    val datatype = InternalDatatype(json)

    InternalFieldForm(
      name = JsonUtil.asOptString(json \ "name"),
      datatype = datatype,
      description = JsonUtil.asOptString(json \ "description"),
      deprecation = InternalDeprecationForm.fromJsValue(json),
      required = datatype.map(_.required).getOrElse(true),
      default = JsonUtil.asOptString(json \ "default"),
      minimum = JsonUtil.asOptLong(json \ "minimum"),
      maximum = JsonUtil.asOptLong(json \ "maximum"),
      example = JsonUtil.asOptString(json \ "example"),
      attributes = InternalAttributeForm.attributesFromJson((json \ "attributes").asOpt[JsArray]),
      warnings = warnings ++ JsonUtil.validate(
        json,
        strings = Seq("name", "type"),
        optionalStrings = Seq("description", "example"),
        optionalObjects = Seq("deprecation"),
        optionalBooleans = Seq("required"),
        optionalNumbers = Seq("minimum", "maximum"),
        optionalArraysOfObjects = Seq("attributes"),
        optionalAnys = Seq("default")
      )
    )
  }

}

object InternalAttributeForm {

  def apply(json: JsObject): InternalAttributeForm = {

    InternalAttributeForm (
      name = JsonUtil.asOptString(json \ "name"),
      value = (json \ "value").asOpt[JsObject],
      description = JsonUtil.asOptString(json \ "description"),
      deprecation = InternalDeprecationForm.fromJsValue(json),
      warnings = JsonUtil.validate(
        json,
        strings = Seq("name"),
        objects = Seq("value"),
        optionalStrings = Seq("description")
      )

    )
  }

  def attributesFromJson(a: Option[JsArray]): Seq[InternalAttributeForm] = a match {
    case None => Seq.empty
    case Some(a: JsArray) => {
      a.value.flatMap { _.asOpt[JsObject].map(InternalAttributeForm(_)) }
    }
  }

}

object InternalParameterForm {

  def apply(json: JsObject): InternalParameterForm = {
    val datatype = InternalDatatype(json)

    InternalParameterForm(
      name = JsonUtil.asOptString(json \ "name"),
      datatype = datatype,
      location = JsonUtil.asOptString(json \ "location"),
      description = JsonUtil.asOptString(json \ "description"),
      deprecation = InternalDeprecationForm.fromJsValue(json),
      required = datatype.map(_.required).getOrElse(true),
      default = JsonUtil.asOptString(json \ "default"),
      minimum = JsonUtil.asOptLong(json \ "minimum"),
      maximum = JsonUtil.asOptLong(json \ "maximum"),
      example = JsonUtil.asOptString(json \ "example"),
      warnings = JsonUtil.validate(
        json,
        strings = Seq("name", "type"),
        optionalStrings = Seq("description", "example", "location"),
        optionalObjects = Seq("deprecation"),
        optionalBooleans = Seq("required"),
        optionalNumbers = Seq("minimum", "maximum"),
        optionalAnys = Seq("default")
      )
    )
  }

}

sealed trait InternalDatatype {

  def name: String
  def required: Boolean
  def label: String

  protected def makeLabel(prefix: String = "", postfix: String = ""): String = {
    prefix + name + postfix
  }

}

private[api_json] object InternalDatatype {

  case class List(name: String, required: Boolean) extends InternalDatatype {
    override def label = makeLabel("[", "]")
  }

  case class Map(name: String, required: Boolean) extends InternalDatatype {
    override def label = makeLabel("map[", "]")
  }

  case class Singleton(name: String, required: Boolean) extends InternalDatatype {
    override def label = makeLabel()
  }

  private val ListRx = "^\\[(.*)\\]$".r
  private val MapRx = "^map\\[(.*)\\]$".r
  private val DefaultMapRx = "^map$".r

  def apply(value: String): InternalDatatype = {
    value match {
      case ListRx(name) => InternalDatatype.List(formatName(name), true)
      case MapRx(name) => InternalDatatype.Map(formatName(name), true)
      case DefaultMapRx() => InternalDatatype.Map(Primitives.String.toString, true)
      case _ => InternalDatatype.Singleton(formatName(value), true)
    }
  }

  /**
    * Make primitive datatype names case insensitive to user
    * input. e.g. accept both 'UUID' and 'uuid' as the uuid type.
    */
  private def formatName(name: String): String = {
    Primitives(name) match {
      case None => name
      case Some(p) => p.toString
    }
  }

  def apply(json: JsObject): Option[InternalDatatype] = {
    JsonUtil.asOptString(json \ "type").map(InternalDatatype(_)).map { dt =>
      JsonUtil.asOptBoolean(json \ "required") match {
        case None => {
          dt
        }

        case Some(true) => {
          // User explicitly marked this required
          dt match {
            case InternalDatatype.List(name, _) => InternalDatatype.List(formatName(name), true)
            case InternalDatatype.Map(name, _) => InternalDatatype.Map(formatName(name), true)
            case InternalDatatype.Singleton(name, _) => InternalDatatype.Singleton(formatName(name), true)
          }
        }

        case Some(false) => {
          // User explicitly marked this optional
          dt match {
            case InternalDatatype.List(name, _) => InternalDatatype.List(formatName(name), false)
            case InternalDatatype.Map(name, _) => InternalDatatype.Map(formatName(name), false)
            case InternalDatatype.Singleton(name, _) => InternalDatatype.Singleton(formatName(name), false)
          }
        }
      }
    }
  }

}
