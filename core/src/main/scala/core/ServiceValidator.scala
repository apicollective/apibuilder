package core

import lib.{Datatype, Methods, Primitives, Text, Type, TypeKind, UrlKey}
import com.gilt.apidocspec.models.{Enum, Field, Service}
import play.api.libs.json.{JsObject, Json, JsValue}
import com.fasterxml.jackson.core.{ JsonParseException, JsonProcessingException }
import com.fasterxml.jackson.databind.JsonMappingException
import scala.util.{Failure, Success, Try}

case class ServiceValidator(apiJson: String) {

  private val RequiredFields = Seq("name")

  private var parseError: Option[String] = None

  lazy val service: Option[Service] = {
    internalService.map { ServiceBuilder(_) }
  }

  def validate(): Either[Seq[String], Service] = {
    if (isValid) {
      Right(service.get)
    } else {
      Left(errors)
    }
  }

  private lazy val internalService: Option[InternalServiceForm] = {
    val tryDescription = Try(Some(InternalServiceForm(apiJson))) recover {
      case e: JsonParseException => {
        parseError = Some(e.getMessage)
        None
      }
      case e: JsonProcessingException => {
        parseError = Some(e.getMessage)
        None
      }
    }

    // Throw any unhandled exceptions
    tryDescription.get
  }

  lazy val errors: Seq[String] = {
    internalService match {

      case None => {
        if (apiJson == "") {
          Seq("No Data")
        } else {
          Seq(parseError.getOrElse("Invalid JSON"))
        }
      }

      case Some(sd: InternalServiceForm) => {
        val requiredFieldErrors = validateRequiredFields()

        if (requiredFieldErrors.isEmpty) {
          validateName ++
          validateKey ++
          validateBaseUrl ++
          validateModels ++
          validateEnums ++
          validateHeaders ++
          validateModelAndEnumNamesAreDistinct ++
          validateFields ++
          validateFieldTypes ++
          validateFieldDefaults ++
          validateOperations ++
          validateResources ++
          validateParameterBodies ++
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

  def isValid: Boolean = {
    errors.isEmpty
  }


  private def validateName(): Seq[String] = {
    val name = internalService.get.name.getOrElse(sys.error("Missing name"))
    if (Text.startsWithLetter(name)) {
      Seq.empty
    } else {
      Seq(s"Name[${name}] must start with a letter")
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

  private def validateBaseUrl(): Seq[String] = {
    internalService.get.baseUrl match {
      case Some(url) => { 
        if(url.endsWith("/")){
          Seq(s"base_url[$url] must not end with a '/'")  
        } else {
          Seq.empty
        } 
      }
      case None => Seq.empty
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
        field.datatype.get.names.flatMap { name =>
          internalService.get.typeResolver.toType(name) match {
            case None => Some(s"${model.name}.${field.name.get} has invalid type. There is no model, enum, nor datatype named[$name]")
            case _ => None
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
          internalService.get.typeValidator.validate(pd, field.default.get, Some(s"${model.name}.${field.name.get}"))
        }
      }
    }
  }

  private def validateModels(): Seq[String] = {
    val nameErrors = internalService.get.models.flatMap { model =>
      Text.validateName(model.name) match {
        case Nil => None
        case errors => {
          Some(s"Model[${model.name}] name is invalid: ${errors.mkString(" ")}")
        }
      }
    }

    val fieldErrors = internalService.get.models.filter { _.fields.isEmpty }.map { model =>
      s"Model[${model.name}] must have at least one field"
    }

    val duplicates = internalService.get.models.groupBy(_.name.toLowerCase).filter { _._2.size > 1 }.keys.map { modelName =>
      s"Model[$modelName] appears more than once"
    }

    nameErrors ++ fieldErrors ++ duplicates
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
      header.datatype.get.names.flatMap { htype =>
        if (htype == Primitives.String.toString || enumNames.contains(htype)) {
          None
        } else {
          Some(s"Header[${header.name.get}] type[$htype] is invalid: Must be a string or the name of an enum")
        }
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
  private def validateModelAndEnumNamesAreDistinct(): Seq[String] = {
    val modelNames = internalService.get.models.map(_.name.toLowerCase)
    val enumNames = internalService.get.enums.map(_.name.toLowerCase).toSet

    modelNames.filter { enumNames.contains(_) }.map { name =>
      s"Name[$name] cannot be used as the name of both a model and an enum"
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
    val invalidCodes = internalService.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.flatMap { r =>
          Try(r.code.toInt) match {
            case Success(v) => None
            case Failure(ex) => ex match {
              case e: java.lang.NumberFormatException => {
                Some(s"Resource[${resource.modelName.getOrElse("")}] ${op.label}: Response code is not an integer[${r.code}]")
              }
            }
          }
        }
      }
    }

    val modelNames = internalService.get.models.map { _.name }.toSet
    val enumNames = internalService.get.enums.map { _.name }.toSet

    val missingMethods = internalService.get.resources.flatMap { resource =>
      resource.operations.filter(_.method.isEmpty).map { op =>
        s"Resource[${resource.modelName.getOrElse("")}] ${op.label}: Missing method"
      }
    }

    val missingTypes = internalService.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.flatMap { r =>
          r.datatype.map(_.names).getOrElse(Seq.empty) match {
            case Nil => {
              Some(s"Resource[${resource.modelName.getOrElse("")}] ${op.label} with response code[${r.code}]: Missing type")
            }
            case typeNames => {
              typeNames.flatMap { typeName =>
                internalService.get.typeResolver.toType(typeName) match {
                  case None => Some(s"Resource[${resource.modelName.getOrElse("")}] ${op.label} with response code[${r.code}] has an invalid type[$typeName].")
                  case Some(_) => None
                }
              }
            }
          }
        }
      }
    }

    val mixed2xxResponseTypes = if (invalidCodes.isEmpty) {
      internalService.get.resources.filter { !_.modelName.isEmpty }.flatMap { resource =>
        resource.operations.flatMap { op =>
          val types = op.responses.filter { r => !r.datatypeLabel.isEmpty && r.code.toInt >= 200 && r.code.toInt < 300 }.map(_.datatypeLabel.get).distinct
          if (types.size <= 1) {
            None
          } else {
            Some(s"Resource[${resource.modelName.get}] cannot have varying response types for 2xx response codes: ${types.sorted.mkString(", ")}")
          }
        }
      }
    } else {
      Seq.empty
    }

    val typesNotAllowed = Seq(404) // also >= 500
    val responsesWithDisallowedTypes = if (invalidCodes.isEmpty) {
      internalService.get.resources.filter { !_.modelName.isEmpty }.flatMap { resource =>
        resource.operations.flatMap { op =>
          op.responses.find { r => typesNotAllowed.contains(r.code.toInt) || r.code.toInt >= 500 } match {
            case None => {
              None
            }
            case Some(r) => {
              Some(s"Resource[${resource.modelName.get}] ${op.label} has a response with code[${r.code}] - this code cannot be explicitly specified")
            }
          }
        }
      }
    } else {
      Seq.empty
    }

    val typesRequiringUnit = Seq(204, 304)
    val noContentWithTypes = if (invalidCodes.isEmpty) {
      internalService.get.resources.filter { !_.modelName.isEmpty }.flatMap { resource =>
        resource.operations.flatMap { op =>
          op.responses.filter(r => typesRequiringUnit.contains(r.code.toInt) && !r.datatype.isEmpty && r.datatype.get.names != Seq(Primitives.Unit.toString)).map { r =>
            s"""Resource[${resource.modelName.get}] ${op.label} Responses w/ code[${r.code}] must return unit and not[${r.datatype.get.label}]"""
          }
        }
      }
    } else {
      Seq.empty
    }

    val warnings = internalService.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.filter(r => !r.warnings.isEmpty).map { r =>
          s"Resource[${resource.modelName.get}] ${op.method.getOrElse("")} ${r.code}: " + r.warnings.mkString(", ")
        }
      }
    }

    invalidCodes ++ missingMethods ++ missingTypes ++ mixed2xxResponseTypes ++ responsesWithDisallowedTypes ++ noContentWithTypes ++ warnings
  }

  private def validateParameterBodies(): Seq[String] = {
    val typesNotFound = internalService.get.resources.flatMap { resource =>
      resource.operations.filter(!_.body.isEmpty).flatMap { op =>
        op.body.flatMap(_.datatype) match {
          case None => Seq(s"${opLabel(resource, op)}: Body missing type")
          case Some(datatype) => {
            internalService.get.typeResolver.parse(datatype) match {
              case None => {
                if (datatype.names.isEmpty || (datatype.names.size == 1 && datatype.names.head.trim == "")) {
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

  private def opLabel(resource: InternalResourceForm, op: InternalOperationForm): String = {
    s"Resource[${resource.modelName.getOrElse("")}] ${op.method.get} ${op.path}"
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
      resource.operations.filter(!_.method.isEmpty).filter(op => !op.body.isEmpty || op.method == Some("GET") ).flatMap { op =>
        op.parameters.filter(!_.name.isEmpty).filter(!_.datatype.isEmpty).flatMap { p =>
          p.datatype.map(_.names).getOrElse(Seq.empty) match {
            case Nil => {
              Some(s"${opLabel(resource, op)}: Parameter[${p.name.get}] is missing a type.")
            }
            case typeNames => {
              typeNames.flatMap { typeName =>
                internalService.get.typeResolver.toType(typeName) match {
                  case Some(Type(TypeKind.Primitive | TypeKind.Enum, _)) => None
                  case Some(Type(TypeKind.Model, name)) => {
                    Some(s"${opLabel(resource, op)}: Parameter[${p.name.get}] has an invalid type[$typeName]. Models are not supported as query parameters.")
                  }
                  case None => {
                    Some(s"${opLabel(resource, op)}: Parameter[${p.name.get}] has an invalid type[$typeName]")
                  }
                }
              }
            }
          }
        }
      }
    }

    val unknownTypes = internalService.get.resources.flatMap { resource =>
      resource.operations.filter(!_.method.isEmpty).flatMap { op =>
        op.parameters.filter(!_.name.isEmpty).filter(!_.datatype.isEmpty).flatMap { p =>
          p.datatype.map(_.names).getOrElse(Seq.empty) match {
            case Nil => {
              Some(s"${opLabel(resource, op)}: Parameter[${p.name.get}] is missing a type.")
            }
            case datatypeNames => {
              datatypeNames.flatMap { datatypeName =>
                internalService.get.typeResolver.toType(datatypeName) match {
                  case Some(Type(TypeKind.Model | TypeKind.Primitive | TypeKind.Enum, _)) => None
                  case None => {
                    Some(s"${opLabel(resource, op)}: Parameter[${p.name.get}] has an invalid type[$datatypeName]")
                  }
                }
              }
            }
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
    val modelNameErrors = internalService.get.resources.flatMap { res =>
      res.modelName match {
        case None => Some("All resources must have a model")
        case Some(name: String) => {
          internalService.get.models.find { _.name == name } match {
            case None => Some(s"Resource[${res.modelName.getOrElse("")}] model[$name] not found")
            case Some(_) => None
          }
        }
      }
    }

    val missingOperations = internalService.get.resources.filter { _.operations.isEmpty }.map { res =>
      s"Resource[${res.modelName.getOrElse("")}] must have at least one operation"
    }

    val duplicateModels = internalService.get.resources.filter { !_.modelName.isEmpty }.flatMap { r =>
      val numberResources = internalService.get.resources.filter { _.modelName == r.modelName }.size
      if (numberResources <= 1) {
        None
      } else {
        Some(s"Model[${r.modelName.get}] cannot be mapped to more than one resource")
      }
    }.distinct

    modelNameErrors ++ missingOperations ++ duplicateModels
  }

  private def validatePathParameters(): Seq[String] = {
    internalService.get.resources.filter(!_.modelName.isEmpty).flatMap { resource =>
      internalService.get.models.find(_.name == resource.modelName.get) match {
        case None => None
        case Some(model: InternalModelForm) => {
          resource.operations.filter(!_.namedPathParameters.isEmpty).flatMap { op =>
            val fieldMap = model.fields.filter(f => !f.name.isEmpty && !f.datatype.map(_.names).isEmpty).map(f => (f.name.get -> f.datatype.get)).toMap
            val paramMap = op.parameters.filter(p => !p.name.isEmpty && !p.datatype.map(_.names).isEmpty).map(p => (p.name.get -> p.datatype.get)).toMap

            op.namedPathParameters.flatMap { name =>
              val parsedDatatype = paramMap.get(name).getOrElse {
                fieldMap.get(name).getOrElse {
                  InternalDatatype(Primitives.String.toString)
                }
              }
              val errorTemplate = s"Resource[${resource.modelName.get}] ${op.method.getOrElse("")} path parameter[$name] has an invalid type[%s]. Valid types for path parameters are: ${Primitives.ValidInPath.mkString(", ")}"

              internalService.get.typeResolver.parse(parsedDatatype) match {
                case None => Some(errorTemplate.format(name))

                case Some(Datatype.List(_)) => Some(errorTemplate.format("list"))
                case Some(Datatype.Map(_)) => Some(errorTemplate.format("map"))
                case Some(Datatype.Option(_)) => Some(errorTemplate.format("option"))
                case Some(Datatype.Singleton(types)) => {
                  types.filter( t => !isTypeValidInPath(t) ) match {
                    case Nil => None
                    case invalidTypes => {
                      Some(errorTemplate.format(invalidTypes.map(_.name).mkString(", ")))
                    }
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
      case TypeKind.Primitive => {
        Primitives.validInPath(t.name)
      }
      case TypeKind.Model => {
        // We do not support models in path parameters
        false
      }
      case TypeKind.Enum => {
        // Serializes as a string
        true
      }
    }
  }

  private def validatePathParametersAreRequired(): Seq[String] = {
    internalService.get.resources.filter(!_.modelName.isEmpty).flatMap { resource =>
      internalService.get.models.find(_.name == resource.modelName.get) match {
        case None => None
        case Some(model: InternalModelForm) => {
          resource.operations.filter(!_.namedPathParameters.isEmpty).flatMap { op =>
            val fieldMap = model.fields.filter(f => !f.name.isEmpty && !f.datatype.map(_.names).isEmpty).map(f => (f.name.get -> f.required)).toMap
            val paramMap = op.parameters.filter(p => !p.name.isEmpty && !p.datatype.map(_.names).isEmpty).map(p => (p.name.get -> p.required)).toMap

            op.namedPathParameters.flatMap { name =>
              val isRequired = paramMap.get(name).getOrElse {
                fieldMap.get(name).getOrElse {
                  true
                }
              }

              if (isRequired) {
                None
              } else {
                Some(s"Resource[${resource.modelName.get}] ${op.method.getOrElse("")} path parameter[$name] is specified as optional. All path parameters are required")
              }
            }
          }
        }
      }
    }
  }
}
