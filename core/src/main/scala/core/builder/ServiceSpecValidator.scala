package builder

import core.{Importer, TypeValidator, TypesProvider}
import com.gilt.apidoc.spec.v0.models.{Method, Operation, ParameterLocation, Resource, Service}
import lib.{Datatype, DatatypeResolver, Kind, Methods, Primitives, Text, Type}

case class ServiceSpecValidator(
  service: Service
) {

  private val typeResolver = DatatypeResolver(
    enumNames = service.enums.map(_.name) ++ service.imports.flatMap { service =>
      service.enums.map { enum =>
        s"${service.namespace}.enums.${enum}"
      }
    },

    modelNames = service.models.map(_.name) ++ service.imports.flatMap { service =>
      service.models.map { model =>
        s"${service.namespace}.models.${model}"
      }
    },

    unionNames = service.unions.map(_.name) ++ service.imports.flatMap { service =>
      service.unions.map { union =>
        s"${service.namespace}.unions.${union}"
      }
    }
  )

  private val validator = TypeValidator(
    defaultNamespace = Some(service.namespace),
    enums = TypesProvider.FromService(service).enums
  )

  lazy val errors: Seq[String] = {
    validateName() ++
    validateBaseUrl() ++
    validateModels() ++
    validateEnums() ++
    validateUnions() ++
    validateHeaders() ++
    validateModelAndEnumAndUnionNamesAreDistinct() ++
    validateFields() ++
    validateFieldDefaults() ++
    validateResources() ++
    validateParameterBodies() ++
    validateParameterDefaults() ++
    validateParameters() ++
    validateResponses()
  }

  private def validateName(): Seq[String] = {
    if (Text.startsWithLetter(service.name)) {
      Seq.empty
    } else {
      Seq(s"Name[${service.name}] must start with a letter")
    }
  }

  private def validateBaseUrl(): Seq[String] = {
    service.baseUrl match {
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

  private def validateModels(): Seq[String] = {
    val nameErrors = service.models.flatMap { model =>
      Text.validateName(model.name) match {
        case Nil => None
        case errors => {
          Some(s"Model[${model.name}] name is invalid: ${errors.mkString(" ")}")
        }
      }
    }

    val fieldErrors = service.models.filter { _.fields.isEmpty }.map { model =>
      s"Model[${model.name}] must have at least one field"
    }

    val duplicates = dupsError("Model", service.models.map(_.name))

    nameErrors ++ fieldErrors ++ duplicates
  }

  private def validateEnums(): Seq[String] = {
    val nameErrors = service.enums.flatMap { enum =>
      Text.validateName(enum.name) match {
        case Nil => None
        case errors => {
          Some(s"Enum[${enum.name}] name is invalid: ${errors.mkString(" ")}")
        }
      }
    }

    val duplicates = dupsError("Enum", service.enums.map(_.name))

    val valueErrors = service.enums.filter { _.values.isEmpty }.map { enum =>
      s"Enum[${enum.name}] must have at least one value"
    }

    val valuesWithInvalidNames = service.enums.flatMap { enum =>
      enum.values.filter(v => !v.name.isEmpty && !Text.startsWithLetter(v.name)).map { value =>
        s"Enum[${enum.name}] value[${value.name}] is invalid: must start with a letter"
      }
    }

    val duplicateValues = service.enums.flatMap { enum =>
      dups(enum.values.map(_.name)).map { value =>
        s"Enum[${enum.name}] value[$value] appears more than once"
      }
    }

    nameErrors ++ duplicates ++ valueErrors ++ valuesWithInvalidNames ++ duplicateValues
  }

  private def validateUnions(): Seq[String] = {
    val nameErrors = service.unions.flatMap { union =>
      Text.validateName(union.name) match {
        case Nil => None
        case errors => {
          Some(s"Union[${union.name}] name is invalid: ${errors.mkString(" ")}")
        }
      }
    }

    val typeErrors = service.unions.filter { _.types.isEmpty }.map { union =>
      s"Union[${union.name}] must have at least one type"
    }

    val invalidTypes = service.unions.filter(!_.name.isEmpty).flatMap { union =>
      union.types.flatMap { t =>
        typeResolver.parse(t.`type`) match {
          case None => Seq(s"Union[${union.name}] type[${t.`type`}] not found")
          case Some(t: Datatype) => {
            t.`type` match {
              case Type(Kind.Primitive, "unit") => {
                Seq("Union types cannot contain unit. To make a particular field optional, use the required property.")
              }
              case _ => {
                Seq.empty
              }
            }
          }
        }
      }
    }

    val duplicates = dupsError("Union", service.unions.map(_.name))

    nameErrors ++ typeErrors ++ invalidTypes ++ duplicates
  }

  private def validateHeaders(): Seq[String] = {
    val enumNames = service.enums.map(_.name).toSet

    val headersWithInvalidTypes = service.headers.flatMap { header =>
      typeResolver.parse(header.`type`) match {
        case None => Some(s"Header[${header.name}] type[${header.`type`}] is invalid")
        case Some(dt) => {
          dt.`type` match {
            case Type(Kind.Primitive, "string") => None
            case Type(Kind.Enum, _) => None
            case Type(Kind.Model | Kind.Union | Kind.Primitive, _) => {
              Some(s"Header[${header.name}] type[${header.`type`}] is invalid: Must be a string or the name of an enum")
            }
          }
        }
      }
    }

    val duplicates = dupsError("Header", service.headers.map(_.name))

    headersWithInvalidTypes ++ duplicates
  }

  /**
    * While not strictly necessary, we do this to reduce
    * confusion. Otherwise we would require an extension to
    * always indicate if a type referenced a model, union, or enum.
    */
  private def validateModelAndEnumAndUnionNamesAreDistinct(): Seq[String] = {
    val modelNames = service.models.map(_.name.toLowerCase)
    val enumNames = service.enums.map(_.name.toLowerCase)
    val unionNames = service.unions.map(_.name.toLowerCase)

    modelNames.filter { enumNames.contains(_) }.map { name =>
      s"Name[$name] cannot be used as the name of both a model and an enum"
    } ++ modelNames.filter { unionNames.contains(_) }.map { name =>
      s"Name[$name] cannot be used as the name of both a model and a union type"
    } ++ enumNames.filter { unionNames.contains(_) }.map { name =>
      s"Name[$name] cannot be used as the name of both an enum and a union type"
    }
  }

  private def validateFields(): Seq[String] = {
    service.models.flatMap { model =>
      model.fields.flatMap { field =>
        Text.validateName(field.name) match {
          case Nil => None
          case errors => {
            Some(s"Model[${model.name}] field name[${field.name}] is invalid: ${errors.mkString(" ")}")
          }
        }
      }
    }

    service.models.flatMap { model =>
      model.fields.flatMap { field =>
        typeResolver.parse(field.`type`) match {
          case None => Some(s"${model.name}.${field.name} has invalid type[${field.`type`}]")
          case _ => None
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
    service.models.flatMap { model =>
      model.fields.flatMap { field =>
        field.default.flatMap { default =>
          validateDefault(s"${model.name}.${field.name}", field.`type`, default)
        }
      }
    }
  }

  private def validateResources(): Seq[String] = {
    val datatypeErrors = service.resources.flatMap { resource =>
      typeResolver.parse(resource.`type`) match {
        case None => {
          Some(s"Resource[${resource.`type`}] has an invalid type")
        }
        case Some(dt) => {
          dt match {
            case Datatype.List(_) | Datatype.Map(_) => {
              Some(s"Resource[${resource.`type`}] has an invalid type: must be a singleton (not a list nor map)")
            }
            case Datatype.Singleton(t) => {
              t match {
                case Type(Kind.Model | Kind.Enum | Kind.Union, name) => {
                  None
                }
                case Type(Kind.Primitive, name) => {
                  Some(s"Resource[${resource.`type`}] has an invalid type: Primitives cannot be mapped to resources")
                }
              }
            }
          }
        }
      }
    }

    val missingOperations = service.resources.filter { _.operations.isEmpty }.map { resource =>
      s"Resource[${resource.`type`}] must have at least one operation"
    }

    val duplicateModels = service.resources.flatMap { resource =>
      val numberResources = service.resources.filter { _.`type` == resource.`type` }.size
      if (numberResources <= 1) {
        None
      } else {
        Some(s"Resource[${resource.`type`}] cannot appear multiple times")
      }
    }.distinct

    datatypeErrors ++ missingOperations ++ duplicateModels
  }

  private def validateParameterBodies(): Seq[String] = {
    val typesNotFound = service.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.body match {
          case None => Seq.empty
          case Some(body) => {
            typeResolver.parse(body.`type`) match {
              case None => Some(opLabel(resource, op, s"body: Type[${body.`type`}] not found"))
              case Some(_) => None
            }
          }
        }
      }
    }

    val invalidMethods = service.resources.flatMap { resource =>
      resource.operations.filter(op => !op.body.isEmpty && !Methods.isJsonDocumentMethod(op.method.toString)).map { op =>
        opLabel(resource, op, s"Cannot specify body for HTTP method[${op.method}]")
      }
    }

    typesNotFound ++ invalidMethods
  }

  private def validateParameterDefaults(): Seq[String] = {
    service.resources.flatMap { resource =>
      resource.operations.filter(!_.parameters.isEmpty).flatMap { op =>
        op.parameters.flatMap { param =>
          typeResolver.parse(param.`type`).flatMap { pd =>
            param.default match {
              case None => None
              case Some(default) => {
                validateDefault(opLabel(resource, op, s"param[${param.name}]"), param.`type`, default)
              }
            }
          }
        }
      }
    }
  }

  private def validateParameters(): Seq[String] = {
    service.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.parameters.flatMap { p =>
          typeResolver.parse(p.`type`) match {
            case None => {
              Some(opLabel(resource, op, s"Parameter[${p.name}] has an invalid type: ${p.`type`}"))
            }
            case Some(dt) => {
              p.location match {
                case ParameterLocation.Query => {
                  // Query parameters can only be primitives or enums
                  dt match {
                    case Datatype.List(Type(Kind.Primitive | Kind.Enum, name)) => {
                      None
                    }
                    case Datatype.List(Type(Kind.Model | Kind.Union, name)) => {
                      Some(opLabel(resource, op, s"Parameter[${p.name}] has an invalid type[${p.`type`}]. Model and union types are not supported as query parameters."))
                    }

                    case Datatype.Singleton(Type(Kind.Primitive | Kind.Enum, name)) => {
                      None
                    }
                    case Datatype.Singleton(Type(Kind.Model | Kind.Union, name)) => {
                      Some(opLabel(resource, op, s"Parameter[${p.name}] has an invalid type[${p.`type`}]. Model and union types are not supported as query parameters."))
                    }

                    case Datatype.Map(_) => {
                      Some(opLabel(resource, op, s"Parameter[${p.name}] has an invalid type[${p.`type`}]. Maps are not supported as query parameters."))
                    }

                  }

                }

                case ParameterLocation.Path => {
                  // Path parameters are required
                  p.required match {
                    case true => None
                    case false => {
                      Some(opLabel(resource, op, s"path parameter[${p.name}] is specified as optional. All path parameters are required"))
                    }
                  }
                }

                case ParameterLocation.Form | ParameterLocation.UNDEFINED(_) => {
                  None
                }
              }
            }
          }
        }
      }
    }
  }

  private def validateResponses(): Seq[String] = {
    val invalidMethods = service.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.method match {
          case Method.UNDEFINED(name) => Some(opLabel(resource, op, s"Invalid HTTP method[$name]. Must be one of: " + Method.all.mkString(", ")))
          case _ => None
        }
      }
    }

    val missingOrInvalidTypes = service.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.flatMap { r =>
          typeResolver.parse(r.`type`) match {
            case None => {
              Some(opLabel(resource, op, s"response code[${r.code}] has an invalid type[${r.`type`}]."))
            }
            case Some(_) => None
          }
        }
      }
    }

    val mixed2xxResponseTypes = service.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        val types = op.responses.filter { r => r.code >= 200 && r.code < 300 }.map(_.`type`).distinct
        if (types.size <= 1) {
          None
        } else {
          Some(s"Resource[${resource.`type`}] cannot have varying response types for 2xx response codes: ${types.sorted.mkString(", ")}")
        }
      }
    }

    val statusCodesNotAllowed = Seq(404) // also >= 500
    val responsesWithDisallowedTypes = service.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.find { r => statusCodesNotAllowed.contains(r.code) || r.code >= 500 }.flatMap { r =>
          Some(opLabel(resource, op, s"response code[${r.code}] cannot be explicitly specified"))
        }
      }
    }

    val statusCodesRequiringUnit = Seq(204, 304)
    val noContentWithTypes = service.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.filter(r => statusCodesRequiringUnit.contains(r.code) && r.`type` != Primitives.Unit.toString).map { r =>
          opLabel(resource, op, s"response code[${r.code}] must return unit and not[${r.`type`}]")
        }
      }
    }

    invalidMethods ++ missingOrInvalidTypes ++ mixed2xxResponseTypes ++ responsesWithDisallowedTypes ++ noContentWithTypes
  }


  private def validateDefault(
    prefix: String,
    `type`: String,
    default: String
  ): Option[String] = {
    typeResolver.parse(`type`) match {
      case None => {
        None
      }
      case Some(pd) => {
        validator.validate(pd, default, Some(prefix))
      }
    }
  }

  private def dupsError(label: String, values: Iterable[String]): Seq[String] = {
    dups(values).map { n =>
      s"$label[$n] appears more than once"
    }.toSeq
  }

  private def dups(values: Iterable[String]): Iterable[String] = {
    values.groupBy(_.toLowerCase).filter { _._2.size > 1 }.keys
  }

  private def opLabel(
    resource: Resource,
    op: Operation,
    message: String
  ): String = {
    s"Resource[${resource.`type`}] ${op.method} ${op.path} $message"
  }


}
