package builder.api_json

import builder.ServiceValidator
import core.{ClientFetcher, ServiceConfiguration, ServiceFetcher}
import lib.{Datatype, Methods, Primitives, Text, Type, Kind, UrlKey}
import com.gilt.apidoc.spec.v0.models.{Enum, Field, Method, Service}
import play.api.libs.json.{JsObject, Json, JsValue}
import com.fasterxml.jackson.core.{ JsonParseException, JsonProcessingException }
import com.fasterxml.jackson.databind.JsonMappingException
import scala.util.{Failure, Success, Try}

case class ApiJsonServiceValidator(
  config: ServiceConfiguration,
  apiJson: String,
  fetcher: ServiceFetcher = new ClientFetcher()
) extends ServiceValidator {

  private val RequiredFields = Seq("name")

  lazy val service: Service = ServiceBuilder(config, internalService.get)

  def validate(): Either[Seq[String], Service] = {
    if (isValid) {
      Right(service)
    } else {
      Left(errors)
    }
  }

  private var parseError: Option[String] = None

  lazy val serviceForm: Option[JsObject] = {
    Try(Json.parse(apiJson)) match {
      case Success(v) => {
        v.asOpt[JsObject] match {
          case Some(o) => {
            Some(o)
          }
          case None => {
            parseError = Some("Must upload a Json Object")
            None
          }
        }
      }
      case Failure(ex) => ex match {
        case e: JsonParseException => {
          parseError = Some(e.getMessage)
          None
        }
        case e: JsonProcessingException => {
          parseError = Some(e.getMessage)
          None
        }
      }
    }
  }

  private lazy val internalService: Option[InternalServiceForm] = serviceForm.map(InternalServiceForm(_, fetcher))

  lazy val errors: Seq[String] = internalErrors match {
    case Nil => builder.ServiceSpecValidator(service, fetcher).errors
    case e => e
  }

  private lazy val internalErrors: Seq[String] = {
    internalService match {

      case None => {
        if (apiJson.trim == "") {
          Seq("No Data")
        } else {
          Seq(parseError.getOrElse("Invalid JSON"))
        }
      }

      case Some(sd: InternalServiceForm) => {
        val requiredFieldErrors = validateRequiredFields()

        if (requiredFieldErrors.isEmpty) {
          validateKey ++
          validateImports ++
          validateEnums ++
          validateUnions ++
          validateHeaders ++
          validateModelAndEnumAndUnionNamesAreDistinct ++
          validateFields ++
          validateFieldTypes ++
          validateFieldDefaults ++
          validateOperations ++
          validateResources ++
          validateParameterBodies ++
          validateParameterDefaults ++
          validateParameters ++
          validateResponses ++
          validatePathParameters ++
          validatePathParametersAreRequired

        } else {
          requiredFieldErrors
        }
      }
    }
  }

  private def validateKey(): Seq[String] = {
    internalService.get.key match {
      case None => Seq.empty
      case Some(key) => {
        val generated = UrlKey.generate(key)
        if (generated == key) {
          Seq.empty
        } else {
          Seq(s"Invalid url key. A valid key would be $generated")
        }
      }
    }
  }

  /**
   * Validate basic structure, returning a list of error messages
   */
  private def validateRequiredFields(): Seq[String] = {
    val missing = RequiredFields.filter { field =>
      (internalService.get.json \ field).asOpt[JsValue] match {
        case None => true
        case Some(_) => false
      }
    }
    if (missing.isEmpty) {
      Seq.empty
    } else {
      Seq("Missing: " + missing.mkString(", "))
    }
  }

  /**
   * Validate references to ensure they refer to proper data
   */
  private def validateFieldTypes(): Seq[String] = {
    internalService.get.models.flatMap { model =>
      model.fields.filter(!_.datatype.isEmpty).filter(!_.name.isEmpty).flatMap { field =>
        internalService.get.typeResolver.toType(field.datatype.get.name) match {
          case None => {
            Some(s"${model.name}.${field.name.get} has invalid type. There is no model, enum, nor datatype named[${field.datatype.get.name}]")
          }
          case _ => {
            None
          }
        }
      }
    }
  }

  /**
   * Validates that any defaults specified for fields are valid:
   *   Valid based on the datatype
   *   If an enum, the default is listed as a value for that enum
   */
  private def validateFieldDefaults(): Seq[String] = {
    internalService.get.models.flatMap { model =>
      model.fields.filter(!_.datatype.isEmpty).filter(!_.name.isEmpty).filter(!_.default.isEmpty).flatMap { field =>
        internalService.get.typeResolver.parse(field.datatype.get).flatMap { pd =>
          internalService.get.typeResolver.validate(pd, field.default.get, Some(s"${model.name}.${field.name.get}"))
        }
      }
    }
  }

  private def validateImports(): Seq[String] = {
    internalService.get.imports.flatMap { imp =>
      imp.uri match {
        case None => Seq("imports.uri is required")
        case Some(uri) => Seq.empty
      }
    }
  }

  private def validateEnums(): Seq[String] = {
    val nameErrors = internalService.get.enums.flatMap { enum =>
      Text.validateName(enum.name) match {
        case Nil => None
        case errors => {
          Some(s"Enum[${enum.name}] name is invalid: ${errors.mkString(" ")}")
        }
      }
    }

    val valueErrors = internalService.get.enums.filter { _.values.isEmpty }.map { enum =>
      s"Enum[${enum.name}] must have at least one value"
    }

    val valuesWithoutNames = internalService.get.enums.flatMap { enum =>
      enum.values.filter(_.name.isEmpty).map { value =>
        s"Enum[${enum.name}] - all values must have a name"
      }
    }

    val valuesWithInvalidNames = internalService.get.enums.flatMap { enum =>
      enum.values.filter(v => !v.name.isEmpty && !Text.startsWithLetter(v.name.get)).map { value =>
        s"Enum[${enum.name}] value[${value.name.get}] is invalid: must start with a letter"
      }
    }

    val duplicates = internalService.get.enums.groupBy(_.name.toLowerCase).filter { _._2.size > 1 }.keys.map { enumName =>
      s"Enum[$enumName] appears more than once"
    }

    nameErrors ++ valueErrors ++ valuesWithoutNames ++ valuesWithInvalidNames ++ duplicates
  }

  private def validateUnions(): Seq[String] = {
    val nameErrors = internalService.get.unions.flatMap { union =>
      Text.validateName(union.name) match {
        case Nil => None
        case errors => {
          Some(s"Union[${union.name}] name is invalid: ${errors.mkString(" ")}")
        }
      }
    }

    val typeErrors = internalService.get.unions.filter { _.types.isEmpty }.map { union =>
      s"Union[${union.name}] must have at least one type"
    }

    val invalidTypes = internalService.get.unions.filter(!_.name.isEmpty).flatMap { union =>
      union.types.flatMap { t =>
        t.datatype match {
          case None => Seq(s"Union[${union.name}] all types must have a name")
          case Some(dt) => {
            internalService.get.typeResolver.parse(dt) match {
              case None => Seq(s"Union[${union.name}] type[${dt.label}] not found")
              case Some(t: Datatype) => {
                t.`type` match {
                  case Type(Kind.Primitive, "unit") => {
                    Seq("Union types cannot contain unit. To make a particular field optional, use the required: true|false property.")
                  }
                  case _ => {
                    Seq.empty
                  }
                }
              }
            }
          }
        }
      }
    }

    val duplicates = internalService.get.unions.groupBy(_.name.toLowerCase).filter { _._2.size > 1 }.keys.map { unionName =>
      s"Union[$unionName] appears more than once"
    }

    nameErrors ++ typeErrors ++ invalidTypes ++ duplicates
  }

  private def validateHeaders(): Seq[String] = {
    val enumNames = internalService.get.enums.map(_.name).toSet

    val headersWithoutNames = internalService.get.headers.filter(_.name.isEmpty) match {
      case Nil => Seq.empty
      case headers => Seq("All headers must have a name")
    }

    val headersWithoutTypes = internalService.get.headers.filter(_.datatype.isEmpty) match {
      case Nil => Seq.empty
      case headers => Seq("All headers must have a type")
    }

    val headersWithInvalidTypes = internalService.get.headers.filter(h => !h.name.isEmpty && !h.datatype.isEmpty).flatMap { header =>
      val htype = header.datatype.get.name
      if (htype == Primitives.String.toString || enumNames.contains(htype)) {
        None
      } else {
        Some(s"Header[${header.name.get}] type[$htype] is invalid: Must be a string or the name of an enum")
      }
    }

    val duplicates = internalService.get.headers.filter(!_.name.isEmpty).groupBy(_.name.get.toLowerCase).filter { _._2.size > 1 }.keys.map { headerName =>
      s"Header[$headerName] appears more than once"
    }

    headersWithoutNames ++ headersWithoutTypes ++ headersWithInvalidTypes ++ duplicates
  }

  /**
    * While not strictly necessary, we do this to reduce
    * confusion. Otherwise we would require an extension to api.json
    * to indicate if a type referenced a model or an enum. By keeping
    * them distinct, we can avoid any confusion.
    */
  private def validateModelAndEnumAndUnionNamesAreDistinct(): Seq[String] = {
    val modelNames = internalService.get.models.map(_.name.toLowerCase)
    val enumNames = internalService.get.enums.map(_.name.toLowerCase)
    val unionNames = internalService.get.unions.map(_.name.toLowerCase)

    modelNames.filter { enumNames.contains(_) }.map { name =>
      s"Name[$name] cannot be used as the name of both a model and an enum"
    } ++ modelNames.filter { unionNames.contains(_) }.map { name =>
      s"Name[$name] cannot be used as the name of both a model and a union type"
    } ++ enumNames.filter { unionNames.contains(_) }.map { name =>
      s"Name[$name] cannot be used as the name of both an enum and a union type"
    }
  }

  private def validateFields(): Seq[String] = {
    val missingNames = internalService.get.models.flatMap { model =>
      model.fields.filter(_.name.isEmpty).map { f =>
        s"Model[${model.name}] field[${f.name}] must have a name"
      }
    }
    val missingTypes = internalService.get.models.flatMap { model =>
      model.fields.filter(!_.name.isEmpty).filter(_.datatype.isEmpty).map { f =>
        s"Model[${model.name}] field[${f.name.get}] must have a type"
      }
    }
    val badNames = internalService.get.models.flatMap { model =>
      model.fields.flatMap { f =>
        f.name.map { n => n -> Text.validateName(n) }
      }.filter(_._2.nonEmpty).flatMap { case (name, errors) =>
          errors.map { e =>
            s"Model[${model.name}] field[${name}]: $e"
          }
      }
    }

    val warnings = internalService.get.models.flatMap { model =>
      model.fields.filter(f => !f.warnings.isEmpty && !f.name.isEmpty).map { f =>
        s"Model[${model.name}] field[${f.name.get}]: " + f.warnings.mkString(", ")
      }
    }

    missingTypes ++ missingNames ++ badNames ++ warnings
  }

  private def validateResponses(): Seq[String] = {
    val invalidMethods = internalService.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.method match {
          case None => Seq(s"Resource[${resource.datatype.label}] ${op.path} Missing HTTP method")
          case Some(m) => {
            Method.fromString(m) match {
              case None => Seq(s"Resource[${resource.datatype.label}] ${op.path} Invalid HTTP method[$m]. Must be one of: " + Method.all.mkString(", "))
              case Some(_) => Seq.empty
            }
          }
        }
      }
    }

    val invalidCodes = internalService.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.flatMap { r =>
          Try(r.code.toInt) match {
            case Success(v) => None
            case Failure(ex) => ex match {
              case e: java.lang.NumberFormatException => {
                Some(s"Resource[${resource.datatype.label}] ${op.label}: Response code is not an integer[${r.code}]")
              }
            }
          }
        }
      }
    }

    val modelNames = internalService.get.models.map { _.name }.toSet
    val enumNames = internalService.get.enums.map { _.name }.toSet

    val missingOrInvalidTypes = internalService.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.flatMap { r =>
          r.datatype.map(_.name) match {
            case None => {
              Some(s"Resource[${resource.datatype.label}] ${op.label} with response code[${r.code}]: Missing type")
            }
            case Some(typeName) => {
              internalService.get.typeResolver.toType(typeName) match {
                case None => Some(s"Resource[${resource.datatype.label}] ${op.label} with response code[${r.code}] has an invalid type[$typeName].")
                case Some(t) => None
              }
            }
          }
        }
      }
    }

    val mixed2xxResponseTypes = if (invalidCodes.isEmpty) {
      internalService.get.resources.flatMap { resource =>
        resource.operations.flatMap { op =>
          val types = op.responses.filter { r => !r.datatypeLabel.isEmpty && r.code.toInt >= 200 && r.code.toInt < 300 }.map(_.datatypeLabel.get).distinct
          if (types.size <= 1) {
            None
          } else {
            Some(s"Resource[${resource.datatype.label}] cannot have varying response types for 2xx response codes: ${types.sorted.mkString(", ")}")
          }
        }
      }
    } else {
      Seq.empty
    }

    val typesNotAllowed = Seq(404) // also >= 500
    val responsesWithDisallowedTypes = if (invalidCodes.isEmpty) {
      internalService.get.resources.flatMap { resource =>
        resource.operations.flatMap { op =>
          op.responses.find { r => typesNotAllowed.contains(r.code.toInt) || r.code.toInt >= 500 } match {
            case None => {
              None
            }
            case Some(r) => {
              Some(s"Resource[${resource.datatype.label}] ${op.label} has a response with code[${r.code}] - this code cannot be explicitly specified")
            }
          }
        }
      }
    } else {
      Seq.empty
    }

    val typesRequiringUnit = Seq(204, 304)
    val noContentWithTypes = if (invalidCodes.isEmpty) {
      internalService.get.resources.flatMap { resource =>
        resource.operations.flatMap { op =>
          op.responses.filter(r => typesRequiringUnit.contains(r.code.toInt) && !r.datatype.isEmpty && r.datatype.get.name != Primitives.Unit.toString).map { r =>
            s"""Resource[${resource.datatype.label}] ${op.label} Responses w/ code[${r.code}] must return unit and not[${r.datatype.get.label}]"""
          }
        }
      }
    } else {
      Seq.empty
    }

    val warnings = internalService.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.filter(r => !r.warnings.isEmpty).map { r =>
          s"Resource[${resource.datatype.label}] ${op.method.getOrElse("")} ${r.code}: " + r.warnings.mkString(", ")
        }
      }
    }

    invalidMethods ++ invalidCodes ++ missingOrInvalidTypes ++ mixed2xxResponseTypes ++ responsesWithDisallowedTypes ++ noContentWithTypes ++ warnings
  }

  private def validateParameterBodies(): Seq[String] = {
    val typesNotFound = internalService.get.resources.flatMap { resource =>
      resource.operations.filter(!_.body.isEmpty).flatMap { op =>
        op.body.flatMap(_.datatype) match {
          case None => Seq(s"${opLabel(resource, op)}: Body missing type")
          case Some(datatype) => {
            internalService.get.typeResolver.parse(datatype) match {
              case None => {
                if (datatype.name.isEmpty || datatype.name.trim == "") {
                  Seq(s"${opLabel(resource, op)}: Body missing type")
                } else {
                  Seq(s"${opLabel(resource, op)} body: Type[${datatype.label}] not found")
                }
              }
              case Some(ti) => Seq.empty
            }
          }
        }
      }
    }

    val invalidMethods = internalService.get.resources.flatMap { resource =>
      resource.operations.filter(op => !op.body.isEmpty && !op.method.isEmpty && !Methods.isJsonDocumentMethod(op.method.get)).map { op =>
        s"${opLabel(resource, op)}: Cannot specify body for HTTP method[${op.method.get}]"
      }
    }

    typesNotFound ++ invalidMethods
  }

  private def validateParameterDefaults(): Seq[String] = {
    internalService.get.resources.flatMap { resource =>
      resource.operations.filter(!_.parameters.isEmpty).flatMap { op =>
        op.parameters.filter(!_.datatype.isEmpty).filter(!_.name.isEmpty).flatMap { param =>
          internalService.get.typeResolver.parse(param.datatype.get).flatMap { pd =>
            param.default match {
              case None => None
              case Some(default) => {
                internalService.get.typeResolver.validate(pd, default, Some(s"${opLabel(resource, op)} param[${param.name.get}]"))
              }
            }
          }
        }
      }
    }
  }

  private def opLabel(resource: InternalResourceForm, op: InternalOperationForm): String = {
    s"Resource[${resource.datatype.label}] ${op.method.get} ${op.path}"
  }

  private def validateParameters(): Seq[String] = {
    val missingNames = internalService.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.parameters.filter(_.name.isEmpty).map { p =>
          s"${opLabel(resource, op)}: Missing name"
        }
        op.parameters.filter(_.datatype.isEmpty).map { p =>
          s"${opLabel(resource, op)}: Missing type"
        }
      }
    }

    // Query parameters can only be primitives or enums
    val invalidQueryTypes = internalService.get.resources.flatMap { resource =>
      resource.operations.filter(!_.method.isEmpty).filter(op => !op.body.isEmpty || op.method.map(Method(_)) == Some(Method.Get) ).flatMap { op =>
        op.parameters.filter(!_.name.isEmpty).filter(!_.datatype.isEmpty).flatMap { p =>
          val dt = p.datatype.get
          internalService.get.typeResolver.parse(dt) match {
            case None => {
              Some(s"${opLabel(resource, op)}: Parameter[${p.name.get}] has an invalid type: ${dt.label}")
            }
            case Some(Datatype.List(Type(Kind.Primitive | Kind.Enum, name))) => {
              None
            }
            case Some(Datatype.List(Type(Kind.Model | Kind.Union, name))) => {
              Some(s"${opLabel(resource, op)}: Parameter[${p.name.get}] has an invalid type[${dt.name}]. Model and union types are not supported as query parameters.")
            }

            case Some(Datatype.Singleton(Type(Kind.Primitive | Kind.Enum, name))) => {
              None
            }
            case Some(Datatype.Singleton(Type(Kind.Model | Kind.Union, name))) => {
              Some(s"${opLabel(resource, op)}: Parameter[${p.name.get}] has an invalid type[${dt.name}]. Model and union types are not supported as query parameters.")
            }

            case Some(Datatype.Map(_)) => {
              Some(s"${opLabel(resource, op)}: Parameter[${p.name.get}] has an invalid type[${dt.label}]. Maps are not supported as query parameters.")
            }
          }
        }
      }
    }

    val unknownTypes = internalService.get.resources.flatMap { resource =>
      resource.operations.filter(!_.method.isEmpty).flatMap { op =>
        op.parameters.filter(!_.name.isEmpty).filter(!_.datatype.isEmpty).flatMap { p =>
          p.datatype.map(_.name) match {
            case None => Some(s"${opLabel(resource, op)}: Parameter[${p.name.get}] is missing a type.")
            case Some(typeName) => None
          }
        }
      }
    }

    missingNames ++ invalidQueryTypes ++ unknownTypes
  }

  private def validateOperations(): Seq[String] = {
    internalService.get.resources.flatMap { resource =>
      resource.operations.filter(!_.warnings.isEmpty).map { op =>
        s"${opLabel(resource, op)}: ${op.warnings.mkString(", ")}"
      }
    }
  }

  private def validateResources(): Seq[String] = {
    val datatypeErrors = internalService.get.resources.flatMap { resource =>
      resource.datatype match {
        case InternalDatatype.List(_, _) | InternalDatatype.Map(_, _) => {
          Some(s"Resource[${resource.datatype.label}] has an invalid type: must be a singleton (not a list nor map)")
        }
        case InternalDatatype.Singleton(name, required) => {
          internalService.get.resources.flatMap { resource =>
            internalService.get.typeResolver.toType(resource.datatype.label) match {
              case None => {
                Some(s"Resource[${resource.datatype.label}] has an invalid type")
              }
              case Some(t) => {
                t match {
                  case Type(Kind.Model | Kind.Enum | Kind.Union, name) => None
                  case Type(Kind.Primitive, name) => {
                    Some(s"Resource[${resource.datatype.label}] has an invalid type: Primitives cannot be mapped to resources")
                  }
                }
              }
            }
          }
        }
      }
    }

    val missingOperations = internalService.get.resources.filter { _.operations.isEmpty }.map { resource =>
      s"Resource[${resource.datatype.label}] must have at least one operation"
    }

    val duplicateModels = internalService.get.resources.flatMap { resource =>
      val numberResources = internalService.get.resources.filter { _.datatype.label == resource.datatype.label }.size
      if (numberResources <= 1) {
        None
      } else {
        Some(s"Resource[${resource.datatype.label}] cannot appear multiple times")
      }
    }.distinct

    datatypeErrors ++ missingOperations ++ duplicateModels
  }

  private def validatePathParameters(): Seq[String] = {
    internalService.get.resources.flatMap { resource =>
      internalService.get.models.find(_.name == resource.datatype.label) match {
        case None => None
        case Some(model: InternalModelForm) => {
          resource.operations.filter(!_.namedPathParameters.isEmpty).flatMap { op =>
            val fieldMap = model.fields.filter(f => !f.name.isEmpty && !f.datatype.map(_.name).isEmpty).map(f => (f.name.get -> f.datatype.get)).toMap
            val paramMap = op.parameters.filter(p => !p.name.isEmpty && !p.datatype.map(_.name).isEmpty).map(p => (p.name.get -> p.datatype.get)).toMap

            op.namedPathParameters.flatMap { name =>
              val parsedDatatype = paramMap.get(name).getOrElse {
                fieldMap.get(name).getOrElse {
                  InternalDatatype(Primitives.String.toString)
                }
              }
              val errorTemplate = s"Resource[${resource.datatype.label}] ${op.method.getOrElse("")} path parameter[$name] has an invalid type[%s]. Valid types for path parameters are: ${Primitives.ValidInPath.mkString(", ")}"

              internalService.get.typeResolver.parse(parsedDatatype) match {
                case None => Some(errorTemplate.format(name))

                case Some(Datatype.List(_)) => Some(errorTemplate.format("list"))
                case Some(Datatype.Map(_)) => Some(errorTemplate.format("map"))
                case Some(Datatype.Singleton(t)) => {
                  isTypeValidInPath(t) match {
                    case true => None
                    case false => Some(errorTemplate.format(t.name))
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private def isTypeValidInPath(t: Type): Boolean = {
    t.typeKind match {
      case Kind.Primitive => {
        Primitives.validInPath(t.name)
      }
      case Kind.Model | Kind.Union => {
        // We do not support models in path parameters
        false
      }
      case Kind.Enum => {
        // Serializes as a string
        true
      }
    }
  }

  private def validatePathParametersAreRequired(): Seq[String] = {
    internalService.get.resources.flatMap { resource =>
      internalService.get.models.find(_.name == resource.datatype.label) match {
        case None => None
        case Some(model: InternalModelForm) => {
          resource.operations.filter(!_.namedPathParameters.isEmpty).flatMap { op =>
            val fieldMap = model.fields.filter(f => !f.name.isEmpty && !f.datatype.map(_.name).isEmpty).map(f => (f.name.get -> f.required)).toMap
            val paramMap = op.parameters.filter(p => !p.name.isEmpty && !p.datatype.map(_.name).isEmpty).map(p => (p.name.get -> p.required)).toMap

            op.namedPathParameters.flatMap { name =>
              val isRequired = paramMap.get(name).getOrElse {
                fieldMap.get(name).getOrElse {
                  true
                }
              }

              if (isRequired) {
                None
              } else {
                Some(s"Resource[${resource.datatype.label}] ${op.method.getOrElse("")} path parameter[$name] is specified as optional. All path parameters are required")
              }
            }
          }
        }
      }
    }
  }
}
