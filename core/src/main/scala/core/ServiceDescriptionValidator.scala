package core

import lib.{Methods, Primitives, Text}
import com.gilt.apidocgenerator.models.{Container, Field, Datatype, ServiceDescription, Type, TypeKind}
import play.api.libs.json.{JsObject, Json, JsValue}
import com.fasterxml.jackson.core.{ JsonParseException, JsonProcessingException }
import com.fasterxml.jackson.databind.JsonMappingException
import scala.util.{Failure, Success, Try}

case class ServiceDescriptionValidator(apiJson: String) {

  private val RequiredFields = Seq("name")

  private var parseError: Option[String] = None

  lazy val serviceDescription: Option[ServiceDescription] = {
    internalServiceDescription.map { ServiceDescriptionBuilder(_) }
  }

  def validate(): Either[Seq[String], ServiceDescription] = {
    if (isValid) {
      Right(serviceDescription.get)
    } else {
      Left(errors)
    }
  }

  private lazy val internalServiceDescription: Option[InternalServiceDescription] = {
    val tryDescription = Try(Some(InternalServiceDescription(apiJson))) recover {
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
    internalServiceDescription match {

      case None => {
        if (apiJson == "") {
          Seq("No Data")
        } else {
          Seq(parseError.getOrElse("Invalid JSON"))
        }
      }

      case Some(sd: InternalServiceDescription) => {
        val requiredFieldErrors = validateRequiredFields()

        if (requiredFieldErrors.isEmpty) {
          validateName ++
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
    val name = internalServiceDescription.get.name.getOrElse(sys.error("Missing name"))
    if (Text.startsWithLetter(name)) {
      Seq.empty
    } else {
      Seq(s"Name[${name}] must start with a letter")
    }
  }

  private def validateBaseUrl(): Seq[String] = {
    internalServiceDescription.get.baseUrl match {
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
      (internalServiceDescription.get.json \ field).asOpt[JsValue] match {
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
    internalServiceDescription.get.models.flatMap { model =>
      model.fields.filter(!_.datatype.isEmpty).filter(!_.name.isEmpty).flatMap { field =>
        field.datatype.get.names.flatMap { name =>
          internalServiceDescription.get.typeResolver.toType(name) match {
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
    internalServiceDescription.get.models.flatMap { model =>
      model.fields.filter(!_.datatype.isEmpty).filter(!_.name.isEmpty).filter(!_.default.isEmpty).flatMap { field =>
        internalServiceDescription.get.typeResolver.parse(field.datatype.get).flatMap { pd =>
          internalServiceDescription.get.typeValidator.validate(pd, field.default.get, Some(s"${model.name}.${field.name.get}"))
        }
      }
    }
  }

  private def validateModels(): Seq[String] = {
    val nameErrors = internalServiceDescription.get.models.flatMap { model =>
      Text.validateName(model.name) match {
        case Nil => None
        case errors => {
          Some(s"Model[${model.name}] name is invalid: ${errors.mkString(" ")}")
        }
      }
    }

    val fieldErrors = internalServiceDescription.get.models.filter { _.fields.isEmpty }.map { model =>
      s"Model[${model.name}] must have at least one field"
    }

    val duplicates = internalServiceDescription.get.models.groupBy(_.name.toLowerCase).filter { _._2.size > 1 }.keys.map { modelName =>
      s"Model[$modelName] appears more than once"
    }

    nameErrors ++ fieldErrors ++ duplicates
  }

  private def validateEnums(): Seq[String] = {
    val nameErrors = internalServiceDescription.get.enums.flatMap { enum =>
      Text.validateName(enum.name) match {
        case Nil => None
        case errors => {
          Some(s"Enum[${enum.name}] name is invalid: ${errors.mkString(" ")}")
        }
      }
    }

    val valueErrors = internalServiceDescription.get.enums.filter { _.values.isEmpty }.map { enum =>
      s"Enum[${enum.name}] must have at least one value"
    }

    val valuesWithoutNames = internalServiceDescription.get.enums.flatMap { enum =>
      enum.values.filter(_.name.isEmpty).map { value =>
        s"Enum[${enum.name}] - all values must have a name"
      }
    }

    val valuesWithInvalidNames = internalServiceDescription.get.enums.flatMap { enum =>
      enum.values.filter(v => !v.name.isEmpty && !Text.startsWithLetter(v.name.get)).map { value =>
        s"Enum[${enum.name}] value[${value.name.get}] is invalid: must start with a letter"
      }
    }

    val duplicates = internalServiceDescription.get.enums.groupBy(_.name.toLowerCase).filter { _._2.size > 1 }.keys.map { enumName =>
      s"Enum[$enumName] appears more than once"
    }

    nameErrors ++ valueErrors ++ valuesWithoutNames ++ valuesWithInvalidNames ++ duplicates
  }

  private def validateHeaders(): Seq[String] = {
    val enumNames = internalServiceDescription.get.enums.map(_.name).toSet

    val headersWithoutNames = internalServiceDescription.get.headers.filter(_.name.isEmpty) match {
      case Nil => Seq.empty
      case headers => Seq("All headers must have a name")
    }

    val headersWithoutTypes = internalServiceDescription.get.headers.filter(_.datatype.isEmpty) match {
      case Nil => Seq.empty
      case headers => Seq("All headers must have a type")
    }

    val headersWithInvalidTypes = internalServiceDescription.get.headers.filter(h => !h.name.isEmpty && !h.datatype.isEmpty).flatMap { header =>
      header.datatype.get.names.flatMap { htype =>
        if (htype == Primitives.String.toString || enumNames.contains(htype)) {
          None
        } else {
          Some(s"Header[${header.name.get}] type[$htype] is invalid: Must be a string or the name of an enum")
        }
      }
    }

    val duplicates = internalServiceDescription.get.headers.filter(!_.name.isEmpty).groupBy(_.name.get.toLowerCase).filter { _._2.size > 1 }.keys.map { headerName =>
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
    val modelNames = internalServiceDescription.get.models.map(_.name.toLowerCase)
    val enumNames = internalServiceDescription.get.enums.map(_.name.toLowerCase).toSet

    modelNames.filter { enumNames.contains(_) }.map { name =>
      s"Name[$name] cannot be used as the name of both a model and an enum"
    }
  }

  private def validateFields(): Seq[String] = {
    val missingNames = internalServiceDescription.get.models.flatMap { model =>
      model.fields.filter(_.name.isEmpty).map { f =>
        s"Model[${model.name}] field[${f.name}] must have a name"
      }
    }
    val missingTypes = internalServiceDescription.get.models.flatMap { model =>
      model.fields.filter(!_.name.isEmpty).filter(_.datatype.isEmpty).map { f =>
        s"Model[${model.name}] field[${f.name.get}] must have a type"
      }
    }
    val badNames = internalServiceDescription.get.models.flatMap { model =>
      model.fields.flatMap { f =>
        f.name.map { n => n -> Text.validateName(n) }
      }.filter(_._2.nonEmpty).flatMap { case (name, errors) =>
          errors.map { e =>
            s"Model[${model.name}] field[${name}]: $e"
          }
      }
    }

    val warnings = internalServiceDescription.get.models.flatMap { model =>
      model.fields.filter(f => !f.warnings.isEmpty && !f.name.isEmpty).map { f =>
        s"Model[${model.name}] field[${f.name.get}]: " + f.warnings.mkString(", ")
      }
    }

    missingTypes ++ missingNames ++ badNames ++ warnings
  }

  private def validateResponses(): Seq[String] = {
    val invalidCodes = internalServiceDescription.get.resources.flatMap { resource =>
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

    val modelNames = internalServiceDescription.get.models.map { _.name }.toSet
    val enumNames = internalServiceDescription.get.enums.map { _.name }.toSet

    val missingMethods = internalServiceDescription.get.resources.flatMap { resource =>
      resource.operations.filter(_.method.isEmpty).map { op =>
        s"Resource[${resource.modelName.getOrElse("")}] ${op.label}: Missing method"
      }
    }

    val missingTypes = internalServiceDescription.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.flatMap { r =>
          r.datatype.map(_.names).getOrElse(Seq.empty) match {
            case Nil => {
              Some(s"Resource[${resource.modelName.getOrElse("")}] ${op.label} with response code[${r.code}]: Missing type")
            }
            case typeNames => {
              typeNames.flatMap { typeName =>
                internalServiceDescription.get.typeResolver.toType(typeName) match {
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
      internalServiceDescription.get.resources.filter { !_.modelName.isEmpty }.flatMap { resource =>
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
      internalServiceDescription.get.resources.filter { !_.modelName.isEmpty }.flatMap { resource =>
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
      internalServiceDescription.get.resources.filter { !_.modelName.isEmpty }.flatMap { resource =>
        resource.operations.flatMap { op =>
          op.responses.filter(r => typesRequiringUnit.contains(r.code.toInt) && !r.datatype.isEmpty && r.datatype.get.names != Seq(Primitives.Unit.toString)).map { r =>
            s"""Resource[${resource.modelName.get}] ${op.label} Responses w/ code[${r.code}] must return unit and not[${r.datatype.get.label}]"""
          }
        }
      }
    } else {
      Seq.empty
    }

    val warnings = internalServiceDescription.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.filter(r => !r.warnings.isEmpty).map { r =>
          s"Resource[${resource.modelName.get}] ${op.method.getOrElse("")} ${r.code}: " + r.warnings.mkString(", ")
        }
      }
    }

    invalidCodes ++ missingMethods ++ missingTypes ++ mixed2xxResponseTypes ++ responsesWithDisallowedTypes ++ noContentWithTypes ++ warnings
  }

  private def validateParameterBodies(): Seq[String] = {
    val typesNotFound = internalServiceDescription.get.resources.flatMap { resource =>
      resource.operations.filter(!_.body.isEmpty).flatMap { op =>
        op.body.flatMap(_.datatype) match {
          case None => Seq(s"${opLabel(resource, op)}: Body missing type")
          case Some(datatype) => {
            internalServiceDescription.get.typeResolver.parse(datatype) match {
              case None => {
                if (datatype.names.isEmpty) {
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

    val invalidMethods = internalServiceDescription.get.resources.flatMap { resource =>
      resource.operations.filter(op => !op.body.isEmpty && !op.method.isEmpty && !Methods.isJsonDocumentMethod(op.method.get)).map { op =>
        s"${opLabel(resource, op)}: Cannot specify body for HTTP method[${op.method.get}]"
      }
    }

    typesNotFound ++ invalidMethods
  }

  private def opLabel(resource: InternalResource, op: InternalOperation): String = {
    s"Resource[${resource.modelName.getOrElse("")}] ${op.method.get} ${op.path}"
  }

  private def validateParameters(): Seq[String] = {
    val missingNames = internalServiceDescription.get.resources.flatMap { resource =>
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
    val invalidQueryTypes = internalServiceDescription.get.resources.flatMap { resource =>
      resource.operations.filter(!_.method.isEmpty).filter(op => !op.body.isEmpty || op.method == Some("GET") ).flatMap { op =>
        op.parameters.filter(!_.name.isEmpty).filter(!_.datatype.isEmpty).flatMap { p =>
          p.datatype.map(_.names).getOrElse(Seq.empty) match {
            case Nil => {
              Some(s"${opLabel(resource, op)}: Parameter[${p.name.get}] is missing a type.")
            }
            case typeNames => {
              typeNames.flatMap { typeName =>
                internalServiceDescription.get.typeResolver.toType(typeName) match {
                  case Some(Type(TypeKind.Primitive | TypeKind.Enum, _)) => None
                  case Some(Type(TypeKind.Model, name)) => {
                    Some(s"${opLabel(resource, op)}: Parameter[${p.name.get}] has an invalid type[$typeName]. Models are not supported as query parameters.")
                  }
                  case None => {
                    Some(s"${opLabel(resource, op)}: Parameter[${p.name.get}] has an invalid type[$typeName]")
                  }
                  case Some(Type(TypeKind.UNDEFINED(kind), name)) => {
                    Some(s"${opLabel(resource, op)}: Parameter[${p.name.get}] has an invalid typeKind[$kind]")
                  }
                }
              }
            }
          }
        }
      }
    }

    val unknownTypes = internalServiceDescription.get.resources.flatMap { resource =>
      resource.operations.filter(!_.method.isEmpty).flatMap { op =>
        op.parameters.filter(!_.name.isEmpty).filter(!_.datatype.isEmpty).flatMap { p =>
          p.datatype.map(_.names).getOrElse(Seq.empty) match {
            case Nil => {
              Some(s"${opLabel(resource, op)}: Parameter[${p.name.get}] is missing a type.")
            }
            case datatypeNames => {
              datatypeNames.flatMap { datatypeName =>
                internalServiceDescription.get.typeResolver.toType(datatypeName) match {
                  case Some(Type(TypeKind.Model | TypeKind.Primitive | TypeKind.Enum, _)) => None
                  case None => {
                    Some(s"${opLabel(resource, op)}: Parameter[${p.name.get}] has an invalid type[$datatypeName]")
                  }
                  case Some(Type(TypeKind.UNDEFINED(kind), name)) => {
                    Some(s"${opLabel(resource, op)}: Parameter[${p.name.get}] has an invalid typeKind[$kind]")
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
    internalServiceDescription.get.resources.flatMap { resource =>
      resource.operations.filter(!_.warnings.isEmpty).map { op =>
        s"${opLabel(resource, op)}: ${op.warnings.mkString(", ")}"
      }
    }
  }

  private def validateResources(): Seq[String] = {
    val modelNameErrors = internalServiceDescription.get.resources.flatMap { res =>
      res.modelName match {
        case None => Some("All resources must have a model")
        case Some(name: String) => {
          internalServiceDescription.get.models.find { _.name == name } match {
            case None => Some(s"Resource[${res.modelName.getOrElse("")}] model[$name] not found")
            case Some(_) => None
          }
        }
      }
    }

    val missingOperations = internalServiceDescription.get.resources.filter { _.operations.isEmpty }.map { res =>
      s"Resource[${res.modelName.getOrElse("")}] must have at least one operation"
    }

    val duplicateModels = internalServiceDescription.get.resources.filter { !_.modelName.isEmpty }.flatMap { r =>
      val numberResources = internalServiceDescription.get.resources.filter { _.modelName == r.modelName }.size
      if (numberResources <= 1) {
        None
      } else {
        Some(s"Model[${r.modelName.get}] cannot be mapped to more than one resource")
      }
    }.distinct

    modelNameErrors ++ missingOperations ++ duplicateModels
  }

  private def validatePathParameters(): Seq[String] = {
    internalServiceDescription.get.resources.filter(!_.modelName.isEmpty).flatMap { resource =>
      internalServiceDescription.get.models.find(_.name == resource.modelName.get) match {
        case None => None
        case Some(model: InternalModel) => {
          resource.operations.filter(!_.namedPathParameters.isEmpty).flatMap { op =>
            val fieldMap = model.fields.filter(f => !f.name.isEmpty && !f.datatype.map(_.names).isEmpty).map(f => (f.name.get -> f.datatype.get)).toMap
            val paramMap = op.parameters.filter(p => !p.name.isEmpty && !p.datatype.map(_.names).isEmpty).map(p => (p.name.get -> p.datatype.get)).toMap

            op.namedPathParameters.flatMap { name =>
              val parsedDatatype = paramMap.get(name).getOrElse {
                fieldMap.get(name).getOrElse {
                  InternalDatatype.Singleton(Primitives.String.toString)
                }
              }
              val errorTemplate = s"Resource[${resource.modelName.get}] ${op.method.getOrElse("")} path parameter[$name] has an invalid type[%s]. Valid types for path parameters are: ${Primitives.ValidInPath.mkString(", ")}"

              internalServiceDescription.get.typeResolver.parse(parsedDatatype) match {
                case None => Some(errorTemplate.format(name))

                case Some(Datatype.List(_)) => Some(errorTemplate.format("list"))
                case Some(Datatype.Map(_)) => Some(errorTemplate.format("map"))
                case Some(Datatype.Option(_)) => Some(errorTemplate.format("option"))
                case Some(Datatype.Singleton(Type(TypeKind.Model, name))) => Some(errorTemplate.format(name))
                case Some(Datatype.Singleton(Type(TypeKind.Primitive, name))) => {
                  if (Primitives.validInPath(name)) {
                    None
                  } else {
                    Some(errorTemplate.format(name))
                  }
                }
                case Some(Datatype.Union(types)) => {
                  // TODO : Support union types in paths when all of
                  // the types are valid for path parameters. Requires
                  // a bit of refactoring to be able to ask if a given
                  // Type is valid in a path.
                  Some(errorTemplate.format("union"))
                }

                case Some(Datatype.Singleton(Type(TypeKind.Enum, name))) => {
                  // Enums serialize to strings
                  None
                }

                case Some(Datatype.Singleton(Type(kind, name))) => {
                  Some(errorTemplate.format(kind))
                }

              }
            }
          }
        }
      }
    }
  }

  private def validatePathParametersAreRequired(): Seq[String] = {
    internalServiceDescription.get.resources.filter(!_.modelName.isEmpty).flatMap { resource =>
      internalServiceDescription.get.models.find(_.name == resource.modelName.get) match {
        case None => None
        case Some(model: InternalModel) => {
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
