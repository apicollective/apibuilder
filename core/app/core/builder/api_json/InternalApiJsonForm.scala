package builder.api_json

import cats.implicits._
import cats.data.ValidatedNec
import builder.JsonUtil
import builder.api_json.upgrades.AllUpgrades
import cats.data.Validated.{Invalid, Valid}
import core.ServiceFetcher
import lib.{Text, ValidatedHelpers}
import play.api.libs.json._

/**
 * Just parses json with minimal validation - build to provide a way to
 * generate meaningful validation messages back to user. Basic flow
 *
 * JSON => InternalApiJsonForm => Service
 *
 */
private[api_json] case class InternalApiJsonForm(
  original: JsValue,
  fetcher: ServiceFetcher,
) {
  val json: JsValue = AllUpgrades.apply(original)
  val internalDatatypeBuilder: InternalDatatypeBuilder = InternalDatatypeBuilder()

  lazy val name: Option[String] = JsonUtil.asOptString(json \ "name")
  lazy val key: Option[String] = JsonUtil.asOptString(json \ "key")
  lazy val namespace: Option[String] = JsonUtil.asOptString(json \ "namespace")
  lazy val baseUrl: Option[String] = JsonUtil.asOptString(json \ "base_url")
  lazy val description: Option[String] = JsonUtil.asOptString(json \ "description")
  lazy val info: Option[InternalInfoForm] = (json \ "info").asOpt[JsValue].map { InternalInfoForm(_) }

  lazy val imports: Seq[InternalImportForm] = {
    (json \ "imports").asOpt[JsArray] match {
      case None => Seq.empty
      case Some(values) => {
        values.value.flatMap { _.asOpt[JsObject].map { InternalImportForm(_) } }
      }.toSeq
    }
  }

  private lazy val declaredUnions: Seq[InternalUnionForm] = {
    (json \ "unions").asOpt[JsValue] match {
      case Some(unions: JsObject) => {
        unions.fields.flatMap { v =>
          v match {
            case(k, value) => value.asOpt[JsObject].map(InternalUnionForm(internalDatatypeBuilder, k, _))
          }
        }
      }.toSeq
      case _ => Seq.empty
    }
  }

  def unions: Seq[InternalUnionForm] = declaredUnions ++ internalDatatypeBuilder.unionForms

  private def parseModels(js: JsValue, prefix: Option[String]): Seq[InternalModelForm] = {
    (js \ "models").asOpt[JsObject] match {
      case Some(models) => {
        models.fields.flatMap { v =>
          v match {
            case (k, value) => value.asOpt[JsObject].map(InternalModelForm(internalDatatypeBuilder, k, _, prefix = prefix))
          }
        }
      }.toSeq
      case _ => Seq.empty
    }
  }

  private lazy val declaredModels: Seq[InternalModelForm] = parseModels(json, prefix = None)

  def templates: InternalTemplateForm = {
    (json \ "templates").asOpt[JsObject] match {
      case None => {
        InternalTemplateForm(models = Nil, resources = Nil)
      }
      case Some(o) => {
        InternalTemplateForm(
          models = parseModels(o, prefix = Some("Templates")),
          resources = parseResources(o)
        )
      }
    }
  }

  def interfaces: Seq[InternalInterfaceForm] = {
    (json \ "interfaces").asOpt[JsValue] match {
      case Some(interfaces: JsObject) => {
        interfaces.fields.flatMap { v =>
          v match {
            case(k, value) => value.asOpt[JsObject].map(InternalInterfaceForm(internalDatatypeBuilder, k, _))
          }
        }
      }.toSeq
      case _ => Seq.empty
    }
  } ++ internalDatatypeBuilder.interfaceForms

  def models: Seq[InternalModelForm] = declaredModels ++ internalDatatypeBuilder.modelForms

  private lazy val declaredEnums: Seq[InternalEnumForm] = {
    (json \ "enums").asOpt[JsValue] match {
      case Some(enums: JsObject) => {
        enums.fields.flatMap { v =>
          v match {
            case(k, value) => value.asOpt[JsObject].map(InternalEnumForm(k, _))
          }
        }
      }.toSeq
      case _ => Seq.empty
    }
  }

  lazy val annotations: Seq[InternalAnnotationForm] = {
    (json \ "annotations").asOpt[JsValue] match {
      case Some(anno: JsObject) => {
        anno.fields.flatMap {
          case(k, value) => value.asOpt[JsObject].map(InternalAnnotationForm(k, _))
        }
      }.toSeq
      case _ => Seq.empty
    }
  }

  def enums: Seq[InternalEnumForm] = declaredEnums ++ internalDatatypeBuilder.enumForms

  lazy val headers: Seq[InternalHeaderForm] = InternalHeaderForm(internalDatatypeBuilder, json)

  private def parseResources(json: JsValue): Seq[InternalResourceForm] = {
    (json \ "resources").asOpt[JsValue] match {
      case None => Seq.empty

      case Some(resources: JsObject) => {
        resources.fields.flatMap { v =>
          v match {
            case (typeName, value) => {
              value.asOpt[JsObject].map(InternalResourceForm(internalDatatypeBuilder, typeName, declaredModels, declaredEnums, unions, _))
            }
          }
        }
      }.toSeq

      case _ => Seq.empty
    }
  }

  lazy val resources: Seq[InternalResourceForm] = parseResources(json)

  lazy val attributes: Seq[InternalAttributeForm] = InternalAttributeForm.fromJson((json \ "attributes").asOpt[JsArray])

  lazy val typeResolver: TypeResolver = TypeResolver(
    defaultNamespace = namespace,
    RecursiveTypesProvider(this)
  )
}

case class InternalImportForm(
  uri: Option[String],
  warnings: ValidatedNec[String, Unit]
)

case class InternalApidocForm(
  version: Option[String]
)

case class InternalInfoForm(
  contact: Option[InternalInfoContactForm],
  license: Option[InternalInfoLicenseForm],
  warnings: ValidatedNec[String, Unit]
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

case class InternalInterfaceForm(
  name: String,
  plural: String,
  description: Option[String],
  deprecation: Option[InternalDeprecationForm],
  fields: Seq[InternalFieldForm],
  attributes: Seq[InternalAttributeForm],
  warnings: ValidatedNec[String, Unit]
)

case class InternalTemplateForm(
  models: Seq[InternalModelForm],
  resources: Seq[InternalResourceForm]
)
case class InternalTemplateDeclarationForm(
  name: Option[String],
  warnings: ValidatedNec[String, Unit]
)

object InternalTemplateDeclarationForm {
  def apply(json: JsObject): InternalTemplateDeclarationForm = {
    InternalTemplateDeclarationForm(
      name = JsonUtil.asOptString(json \ "name"),
      warnings = JsonUtil.validate(
        json,
        strings = Seq("name"),
      )
    )
  }

  def fromJson(a: Option[JsArray]): Seq[InternalTemplateDeclarationForm] = a match {
    case None => Seq.empty
    case Some(a: JsArray) => {
      a.value.flatMap {
        _.asOpt[JsObject].map(InternalTemplateDeclarationForm(_))
      }
    }.toSeq
  }
}

case class InternalModelForm(
  name: String,
  plural: String,
  description: Option[String],
  deprecation: Option[InternalDeprecationForm],
  fields: Seq[InternalFieldForm],
  attributes: Seq[InternalAttributeForm],
  interfaces: Seq[String],
  templates: Seq[InternalTemplateDeclarationForm],
  warnings: ValidatedNec[String, Unit]
)

case class InternalEnumForm(
  name: String,
  plural: String,
  description: Option[String],
  deprecation: Option[InternalDeprecationForm],
  values: Seq[InternalEnumValueForm],
  attributes: Seq[InternalAttributeForm],
  warnings: ValidatedNec[String, Unit]
)

case class InternalEnumValueForm(
  name: Option[String],
  value: Option[String],
  description: Option[String],
  deprecation: Option[InternalDeprecationForm],
  attributes: Seq[InternalAttributeForm],
  warnings: ValidatedNec[String, Unit]
)

case class InternalUnionForm(
  name: String,
  plural: String,
  discriminator: Option[String],
  description: Option[String],
  deprecation: Option[InternalDeprecationForm],
  types: Seq[InternalUnionTypeForm],
  interfaces: Seq[String],
  attributes: Seq[InternalAttributeForm],
  warnings: ValidatedNec[String, Unit]
)

case class InternalUnionTypeForm(
  datatype: ValidatedNec[String, InternalDatatype],
  description: Option[String],
  deprecation: Option[InternalDeprecationForm],
  attributes: Seq[InternalAttributeForm],
  default: Option[Boolean],
  discriminatorValue: Option[String],
  warnings: ValidatedNec[String, Unit]
)

case class InternalHeaderForm(
  name: Option[String],
  datatype: ValidatedNec[String, InternalDatatype],
  required: Boolean,
  description: Option[String],
  deprecation: Option[InternalDeprecationForm],
  default: Option[String],
  attributes: Seq[InternalAttributeForm],
  warnings: ValidatedNec[String, Unit]
)

case class InternalResourceForm(
  datatype: InternalDatatype,
  description: Option[String],
  deprecation: Option[InternalDeprecationForm],
  path: Option[String],
  operations: Seq[InternalOperationForm],
  attributes: Seq[InternalAttributeForm],
  templates: Seq[InternalTemplateDeclarationForm],
  warnings: ValidatedNec[String, Unit]
)

case class InternalOperationForm(
  method: Option[String],
  path: Option[String],
  description: Option[String],
  deprecation: Option[InternalDeprecationForm],
  parameters: Seq[InternalParameterForm],
  body: Option[InternalBodyForm],
  declaredResponses: Seq[InternalResponseForm],
  attributes: Seq[InternalAttributeForm],
  warnings: ValidatedNec[String, Unit]
) {

  lazy val label: String = "%s %s".format(method.getOrElse(""), path).trim

}

case class InternalFieldForm(
  name: Option[String] = None,
  datatype: ValidatedNec[String, InternalDatatype],
  description: Option[String] = None,
  deprecation: Option[InternalDeprecationForm],
  required: Boolean = true,
  default: Option[String] = None,
  example: Option[String] = None,
  minimum: Option[Long] = None,
  maximum: Option[Long] = None,
  attributes: Seq[InternalAttributeForm] = Nil,
  annotations: Seq[String],
  warnings: ValidatedNec[String, Unit]
)

case class InternalAttributeForm(
  name: Option[String] = None,
  value: Option[JsObject] = None,
  description: Option[String] = None,
  deprecation: Option[InternalDeprecationForm],
  warnings: ValidatedNec[String, Unit]
)

case class InternalAnnotationForm(
  name: String,
  description: Option[String] = None,
  deprecation: Option[InternalDeprecationForm],
  warnings: ValidatedNec[String, Unit]
)

case class InternalParameterForm(
  name: Option[String] = None,
  datatype: ValidatedNec[String, InternalDatatype],
  location: Option[String] = None,
  description: Option[String] = None,
  deprecation: Option[InternalDeprecationForm],
  required: Boolean,
  default: Option[String] = None,
  example: Option[String] = None,
  minimum: Option[Long] = None,
  maximum: Option[Long] = None,
  attributes: Seq[InternalAttributeForm] = Nil,
  warnings: ValidatedNec[String, Unit]
)

case class InternalBodyForm(
  datatype: ValidatedNec[String, InternalDatatype],
  description: Option[String] = None,
  deprecation: Option[InternalDeprecationForm],
  attributes: Seq[InternalAttributeForm],
  warnings: ValidatedNec[String, Unit]
)

case class InternalResponseForm(
  code: String,
  datatype: ValidatedNec[String, InternalDatatype],
  headers: Seq[InternalHeaderForm] = Nil,
  description: Option[String] = None,
  deprecation: Option[InternalDeprecationForm] = None,
  attributes: Seq[InternalAttributeForm] = Nil,
  warnings: ValidatedNec[String, Unit]
) {

  lazy val datatypeLabel: Option[String] = datatype.toOption.map(_.name)

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

  def apply(internalDatatypeBuilder: InternalDatatypeBuilder, name: String, value: JsObject): InternalUnionForm = {
    val description = JsonUtil.asOptString(value \ "description")
    val types = (value \ "types").asOpt[JsArray] match {
       case None => Seq.empty
       case Some(a: JsArray) => {
         a.value.flatMap { value =>
           value.asOpt[JsObject].map { json =>
             val internalDatatype = internalDatatypeBuilder.parseTypeFromObject(json)
             val datatypeName = internalDatatype.toOption.map(_.name)

             InternalUnionTypeForm(
               datatype = internalDatatype,
               description = JsonUtil.asOptString(json \ "description"),
               deprecation = InternalDeprecationForm.fromJsValue(json),
               default = JsonUtil.asOptBoolean(json \ "default"),
               discriminatorValue = JsonUtil.asOptString(json \ "discriminator_value"),
               attributes = InternalAttributeForm.fromJson((value \ "attributes").asOpt[JsArray]),
               warnings = JsonUtil.validate(
                 json,
                 anys = Seq("type"),
                 optionalStrings = Seq("description", "discriminator_value"),
                 optionalBooleans = Seq("default"),
                 optionalObjects = Seq("deprecation"),
                 optionalArraysOfObjects = Seq("attributes"),
                 prefix = Some(s"Union[$name] type[${datatypeName.getOrElse("")}]")
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
      types = types.toSeq,
      interfaces = (value \ "interfaces").asOpt[Seq[String]].getOrElse(Nil),
      attributes = InternalAttributeForm.fromJson((value \ "attributes").asOpt[JsArray]),
      warnings = JsonUtil.validate(
        value,
        optionalStrings = Seq("discriminator", "description", "plural"),
        arrayOfObjects = Seq("types"),
        optionalObjects = Seq("deprecation"),
        optionalArraysOfStrings = Seq("interfaces"),
        optionalArraysOfObjects = Seq("attributes"),
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

object InternalInterfaceForm {

  def apply(internalDatatypeBuilder: InternalDatatypeBuilder, name: String, value: JsObject): InternalInterfaceForm = {
    InternalInterfaceForm(
      name = name,
      plural = JsonUtil.asOptString(value \ "plural").getOrElse( Text.pluralize(name) ),
      description = JsonUtil.asOptString(value \ "description"),
      deprecation = InternalDeprecationForm.fromJsValue(value),
      fields = InternalFieldForm.parse(internalDatatypeBuilder, value),
      attributes = InternalAttributeForm.fromJson((value \ "attributes").asOpt[JsArray]),
      warnings = JsonUtil.validate(
        value,
        optionalStrings = Seq("description", "plural"),
        optionalArraysOfObjects = Seq("fields", "attributes"),
        optionalObjects = Seq("deprecation"),
        prefix = Some(s"Interface[$name]")
      )
    )
  }

}

object InternalModelForm {

  def apply(internalDatatypeBuilder: InternalDatatypeBuilder, name: String, value: JsObject, prefix: Option[String]): InternalModelForm = {
    InternalModelForm(
      name = name,
      plural = JsonUtil.asOptString(value \ "plural").getOrElse( Text.pluralize(name) ),
      description = JsonUtil.asOptString(value \ "description"),
      deprecation = InternalDeprecationForm.fromJsValue(value),
      fields = InternalFieldForm.parse(internalDatatypeBuilder, value),
      attributes = InternalAttributeForm.fromJson((value \ "attributes").asOpt[JsArray]),
      templates = InternalTemplateDeclarationForm.fromJson((value \ "templates").asOpt[JsArray]),
      interfaces = (value \ "interfaces").asOpt[Seq[String]].getOrElse(Nil),
      warnings = JsonUtil.validate(
        value,
        optionalStrings = Seq("description", "plural"),
        optionalArraysOfStrings = Seq("interfaces"),
        optionalArraysOfObjects = Seq("fields", "attributes", "templates"),
        optionalObjects = Seq("deprecation"),
        prefix = Some(addPrefix(s"Model[$name]", prefix = prefix))
      )
    )
  }

  private def addPrefix(label: String, prefix: Option[String]): String = {
    prefix match {
      case None => label
      case Some(p) => s"$p $label"
    }
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
               value = JsonUtil.asOptString(json \ "value"),
               description = JsonUtil.asOptString(json \ "description"),
               deprecation = InternalDeprecationForm.fromJsValue(json),
               attributes = InternalAttributeForm.fromJson((json \ "attributes").asOpt[JsArray]),
               warnings = JsonUtil.validate(
                 json,
                 strings = Seq("name"),
                 optionalStrings = Seq("value", "description"),
                 optionalObjects = Seq("deprecation"),
                 optionalArraysOfObjects = Seq("attributes"),
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
      values = values.toSeq,
      attributes = InternalAttributeForm.fromJson((value \ "attributes").asOpt[JsArray]),
      warnings = JsonUtil.validate(
        value,
        optionalStrings = Seq("name", "description", "plural"),
        arrayOfObjects = Seq("values"),
        optionalObjects = Seq("deprecation"),
        optionalArraysOfObjects = Seq("attributes"),
        prefix = Some(s"Enum[$name]")
      )
    )
  }

}

object InternalHeaderForm {
  def apply(internalDatatypeBuilder: InternalDatatypeBuilder, json: JsValue): Seq[InternalHeaderForm] = {
    (json \ "headers").asOpt[JsArray].map(_.value).getOrElse(Seq.empty).flatMap { el =>
      el match {
        case o: JsObject => {
          val datatype = internalDatatypeBuilder.parseTypeFromObject(o)

          val headerName = JsonUtil.asOptString(o \ "name")
          Some(
            InternalHeaderForm(
              name = headerName,
              datatype = datatype,
              required = InternalDatatype.isRequired(datatype),
              description = JsonUtil.asOptString(o \ "description"),
              deprecation = InternalDeprecationForm.fromJsValue(o),
              default = JsonUtil.asOptString(o \ "default"),
              attributes = InternalAttributeForm.fromJson((o \ "attributes").asOpt[JsArray]),
              warnings = JsonUtil.validate(
                o,
                strings = Seq("name"),
                anys = Seq("type"),
                optionalBooleans = Seq("required"),
                optionalObjects = Seq("deprecation"),
                optionalStrings = Seq("default", "description"),
                optionalArraysOfObjects = Seq("attributes"),
                prefix = Some(s"Header[${headerName.getOrElse("")}]".trim)
              )
            )
          )
        }
        case _ => None
      }
    }
  }.toSeq
}

object InternalResourceForm extends ValidatedHelpers {

  def apply(
    internalDatatypeBuilder: InternalDatatypeBuilder,
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
        a.value.flatMap { _.asOpt[JsObject].map(InternalOperationForm(internalDatatypeBuilder, path, _)) }
      }
    }

    internalDatatypeBuilder.fromString(typeName) match {
      case Invalid(errors) => sys.error(s"Invalid datatype[$typeName]: ${formatErrors(errors)}")
      case Valid(datatype) => {
        InternalResourceForm(
          datatype = datatype,
          description = JsonUtil.asOptString(value \ "description"),
          deprecation = InternalDeprecationForm.fromJsValue(value),
          path = path,
          operations = operations.toSeq,
          attributes = InternalAttributeForm.fromJson((value \ "attributes").asOpt[JsArray]),
          templates = InternalTemplateDeclarationForm.fromJson((value \ "templates").asOpt[JsArray]),
          warnings = JsonUtil.validate(
            value,
            optionalStrings = Seq("path", "description"),
            optionalObjects = Seq("deprecation"),
            optionalArraysOfObjects = Seq("attributes", "templates", "operations")
          )
        )
      }
    }
  }

}

object InternalOperationForm {

  def apply(internalDatatypeBuilder: InternalDatatypeBuilder, resourcePath: Option[String], json: JsObject): InternalOperationForm = {
    val path = JsonUtil.asOptString(json \ "path")

    val parameters = (json \ "parameters").asOpt[JsArray] match {
      case None => Seq.empty
      case Some(a: JsArray) => {
        a.value.flatMap { _.asOpt[JsObject].map(InternalParameterForm(internalDatatypeBuilder, _)) }
      }
    }

    val responses: Seq[InternalResponseForm] = {
      (json \ "responses").asOpt[JsObject] match {
        case None => Nil
        case Some(responses: JsObject) => {
          responses.fields.map {
            case(code, value) => {
              value match {
                case o: JsObject => {
                  InternalResponseForm(internalDatatypeBuilder, code, o)
                }
                case _ => {
                  InternalResponseForm(
                    datatype = InternalDatatype.Unit.validNec,
                    code = code,
                    warnings = "value must be an object".invalidNec
                  )
                }
              }
            }
          }
        }.toSeq
      }
    }

    val body = (json \ "body").asOpt[JsObject].map { o =>
      InternalBodyForm(
        datatype = internalDatatypeBuilder.parseTypeFromObject(o),
        description = JsonUtil.asOptString(o \ "description"),
        deprecation = InternalDeprecationForm.fromJsValue(o),
        attributes = InternalAttributeForm.fromJson((o \ "attributes").asOpt[JsArray]),
        warnings = JsonUtil.validate(
          o,
          anys = Seq("type"),
          optionalStrings = Seq("description"),
          optionalObjects = Seq("deprecation"),
          optionalArraysOfObjects = Seq("attributes")
        )
      )
    }

    InternalOperationForm(
      method = JsonUtil.asOptString(json \ "method").map(_.toUpperCase),
      path = path,
      body = body,
      description = JsonUtil.asOptString(json \ "description"),
      deprecation = InternalDeprecationForm.fromJsValue(json),
      declaredResponses = responses,
      parameters = parameters.toSeq,
      attributes = InternalAttributeForm.fromJson((json \ "attributes").asOpt[JsArray]),
      warnings = JsonUtil.validate(
        json,
        strings = Seq("method"),
        optionalStrings = Seq("description", "path"),
        optionalArraysOfObjects = Seq("parameters", "attributes"),
        optionalObjects = Seq("body", "responses", "deprecation")
      )
    )

  }
}

object InternalResponseForm {

  def apply(internalDatatypeBuilder: InternalDatatypeBuilder, code: String, json: JsObject): InternalResponseForm = {
    InternalResponseForm(
      code = code,
      datatype = internalDatatypeBuilder.parseTypeFromObject(json),
      headers = InternalHeaderForm(internalDatatypeBuilder, json),
      description = JsonUtil.asOptString(json \ "description"),
      deprecation = InternalDeprecationForm.fromJsValue(json),
      attributes = InternalAttributeForm.fromJson((json \ "attributes").asOpt[JsArray]),
      warnings = JsonUtil.validate(
        json,
        anys = Seq("type"),
        optionalStrings = Seq("description"),
        optionalArraysOfObjects = Seq("headers", "attributes"),
        optionalObjects = Seq("deprecation")
      )
    )
  }
}

object InternalFieldForm extends ValidatedHelpers {

  def parse(internalDatatypeBuilder: InternalDatatypeBuilder, value: JsValue): Seq[InternalFieldForm] = {
    (value \ "fields").asOpt[JsArray] match {
      case None => Seq.empty
      case Some(a: JsArray) => {
        a.value.flatMap { _.asOpt[JsObject].map { v =>
          InternalFieldForm(internalDatatypeBuilder, v)
        }}.toSeq
      }
    }
  }

  def apply(internalDatatypeBuilder: InternalDatatypeBuilder, json: JsObject): InternalFieldForm = {
    val warnings = if (JsonUtil.hasKey(json, "enum") || JsonUtil.hasKey(json, "values")) {
      "Enumerations are now first class objects and must be defined in an explicit enum section".invalidNec
    } else {
      ().validNec
    }

    val datatype = internalDatatypeBuilder.parseTypeFromObject(json)

    InternalFieldForm(
      name = JsonUtil.asOptString(json \ "name"),
      datatype = datatype,
      description = JsonUtil.asOptString(json \ "description"),
      deprecation = InternalDeprecationForm.fromJsValue(json),
      required = InternalDatatype.isRequired(datatype),
      default = JsonUtil.asOptString(json \ "default"),
      minimum = JsonUtil.asOptLong(json \ "minimum"),
      maximum = JsonUtil.asOptLong(json \ "maximum"),
      example = JsonUtil.asOptString(json \ "example"),
      attributes = InternalAttributeForm.fromJson((json \ "attributes").asOpt[JsArray]),
      annotations = JsonUtil.asSeqOfString(json \ "annotations"),
      warnings = sequenceUnique(Seq(warnings, JsonUtil.validate(
        json,
        strings = Seq("name"),
        anys = Seq("type"),
        optionalStrings = Seq("description", "example"),
        optionalObjects = Seq("deprecation"),
        optionalBooleans = Seq("required"),
        optionalNumbers = Seq("minimum", "maximum"),
        optionalArraysOfObjects = Seq("attributes"),
        optionalAnys = Seq("default", "annotations")
      )))
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

  def fromJson(a: Option[JsArray]): Seq[InternalAttributeForm] = a match {
    case None => Seq.empty
    case Some(a: JsArray) => {
      a.value.flatMap { _.asOpt[JsObject].map(InternalAttributeForm(_)) }
    }.toSeq
  }

}

object InternalAnnotationForm {

  def apply(name: String, json: JsObject): InternalAnnotationForm = InternalAnnotationForm (
    name = name,
    description = JsonUtil.asOptString(json \ "description"),
    deprecation = InternalDeprecationForm.fromJsValue(json),
    warnings = JsonUtil.validate(
      json,
      optionalStrings = Seq("description"),
      optionalObjects = Seq("deprecation")
    )
  )
}


object InternalParameterForm {

  def apply(internalDatatypeBuilder: InternalDatatypeBuilder, json: JsObject): InternalParameterForm = {
    val datatype = internalDatatypeBuilder.parseTypeFromObject(json)

    InternalParameterForm(
      name = JsonUtil.asOptString(json \ "name"),
      datatype = datatype,
      location = JsonUtil.asOptString(json \ "location"),
      description = JsonUtil.asOptString(json \ "description"),
      deprecation = InternalDeprecationForm.fromJsValue(json),
      required = InternalDatatype.isRequired(datatype),
      default = JsonUtil.asOptString(json \ "default"),
      minimum = JsonUtil.asOptLong(json \ "minimum"),
      maximum = JsonUtil.asOptLong(json \ "maximum"),
      example = JsonUtil.asOptString(json \ "example"),
      attributes = InternalAttributeForm.fromJson((json \ "attributes").asOpt[JsArray]),
      warnings = JsonUtil.validate(
        json,
        strings = Seq("name"),
        anys = Seq("type"),
        optionalStrings = Seq("description", "example", "location"),
        optionalObjects = Seq("deprecation"),
        optionalBooleans = Seq("required"),
        optionalNumbers = Seq("minimum", "maximum"),
        optionalArraysOfObjects = Seq("attributes"),
        optionalAnys = Seq("default")
      )
    )
  }

}
