package builder

import core.{TypeValidator, TypesProvider, Util}
import io.apibuilder.spec.v0.models.{ResponseCodeInt, Header, Method, Operation, ParameterLocation, ResponseCode}
import io.apibuilder.spec.v0.models.{ResponseCodeUndefinedType, ResponseCodeOption, Resource, Service, Union}
import lib.{DatatypeResolver, Kind, Methods, Primitives, Text, VersionTag}

case class ServiceSpecValidator(
  service: Service
) {

  private val ReservedDiscriminatorValues = Seq("value", "implicit")

  private val localTypeResolver = DatatypeResolver(
    enumNames = service.enums.map(_.name),
    modelNames = service.models.map(_.name),
    unionNames = service.unions.map(_.name)
  )
  
  private val typeResolver = DatatypeResolver(
    enumNames = localTypeResolver.enumNames ++ service.imports.flatMap { service =>
      service.enums.map { enum =>
        s"${service.namespace}.enums.$enum"
      }
    },

    modelNames = localTypeResolver.modelNames ++ service.imports.flatMap { service =>
      service.models.map { model =>
        s"${service.namespace}.models.$model"
      }
    },

    unionNames = localTypeResolver.unionNames ++ service.imports.flatMap { service =>
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
    validateApidoc() ++
    validateName() ++
    validateBaseUrl() ++
    validateModels() ++
    validateEnums() ++
    validateUnions() ++
    validateHeaders(service.headers, "Header") ++
    validateModelAndEnumAndUnionNamesAreDistinct() ++
    validateFields() ++
    validateFieldDefaults() ++
    validateResources() ++
    validateParameterLocations() ++
    validateParameterBodies() ++
    validateParameterDefaults() ++
    validateParameterNames() ++
    validateParameters() ++
    validateResponses()
  }

  private def validateApidoc(): Seq[String] = {
    val specified = VersionTag(service.apidoc.version)
    val current = io.apibuilder.spec.v0.Constants.Version
    specified.major match {
      case None => {
        Seq(s"Invalid apidoc version[${service.apidoc.version}]. Latest version of apidoc specification is $current")
      }
      case Some(major) => {
        Nil
      }
    }
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
          Some(s"Model[${model.name}] name is invalid: ${errors.mkString(" and ")}")
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
          Some(s"Enum[${enum.name}] name is invalid: ${errors.mkString(" and ")}")
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
          Some(s"Union[${union.name}] name is invalid: ${errors.mkString(" and ")}")
        }
      }
    }

    val typeErrors = service.unions.filter { _.types.isEmpty }.map { union =>
      s"Union[${union.name}] must have at least one type"
    }

    val invalidTypes = service.unions.filter(!_.name.isEmpty).flatMap { union =>
      union.types.flatMap { t =>
        typeResolver.parse(t.`type`) match {
          case None => {
            Seq(s"Union[${union.name}] type[${t.`type`}] not found")
          }
          case Some(_) => {
            // Validate that the type is NOT imported as there is
            // no way we could retroactively modify the imported
            // type to extend the union type that is only being
            // defined in this service.
            localTypeResolver.parse(t.`type`) match {
              case None => {
                Seq(s"Union[${union.name}] type[${t.`type`}] is invalid. Cannot use an imported type as part of a union as there is no way to declare that the imported type expands the union type defined here.")
              }
              case Some(kind: Kind) => {
                kind match {
                  case Kind.Primitive("unit") => {
                    Seq("Union types cannot contain unit. To make a particular field optional, use the required property.")
                  }
                  case _ => {
                    Nil
                  }
                }
              }
            }
          }
        }
      }
    }
    
    val unionTypeErrors = service.unions.flatMap { union =>
      union.types.find(_.`type` == union.name) match {
        case Some(_) => {
          Seq(s"Union[${union.name}] cannot contain itself as one of its types")
        }
        case None => {
          union.discriminator match {
            case None => {
              union.types.filter { t => t.default.isDefined }.toList match {
                case Nil => None
                case types => Seq(
                  s"Union[${union.name}] types cannot specify default as the union type does not have a 'discriminator' specified: " + types.map(_.`type`).mkString(", ")
                )
              }
            }

            case Some(discriminator) => {
              if (ReservedDiscriminatorValues.contains(discriminator)) {
                Seq(s"Union[${union.name}] discriminator[$discriminator]: The keyword[$discriminator] is reserved and cannot be used as a discriminator")
              } else {
                Text.validateName(discriminator) match {
                  case Nil => {
                    unionTypesWithNamedField(union, discriminator) match {
                      case Nil => Nil
                      case types => {
                        Seq(
                          s"Union[${union.name}] discriminator[$discriminator] must be unique. Field exists on: " +
                            types.mkString(", ")
                        )
                      }
                    }
                  }
                  case errs => Seq(s"Union[${union.name}] discriminator[$discriminator]: " + errs.mkString(", "))
                }
              }
            }
          }
        }
      }
    }

    val duplicates = dupsError("Union", service.unions.map(_.name))

    nameErrors ++ typeErrors ++ invalidTypes ++ unionTypeErrors ++ duplicates ++ validateUnionTypeDiscriminatorValues()
  }

  private[this] def validateUnionTypeDiscriminatorValues(): Seq[String] = {
    service.unions.flatMap { union =>
      union.types.flatMap { unionType =>
        unionType.discriminatorValue match {
          case None => None
          case Some(value) => {
            Text.validateName(value) match {
              case Nil => None
              case errs => {
                Some(s"Union[${union.name}] type[${unionType.`type`}] discriminator_value[${value}] is invalid: ${errs.mkString(" and ")}")
              }
            }
          }
        }
      }
    }
  }

  /**
    * Given a union, returns the list of types that contain the
    * specified field.
    */
  private[this] def unionTypesWithNamedField(union: Union, fieldName: String): Seq[String] = {
    union.types.flatMap { unionType =>
      typeResolver.parse(unionType.`type`) match {
        case None => {
          Nil
        }
        case Some(kind) => {
          unionTypesWithNamedField(kind, fieldName)
        }
      }
    }
  }

  private[this] def unionTypesWithNamedField(kind: Kind, fieldName: String): Seq[String] = {  
    kind match {
      case Kind.Primitive(_) | Kind.Enum(_) => {
        Nil
      }

      case Kind.List(inner) => {
        unionTypesWithNamedField(inner, fieldName)
      }

      case Kind.Map(inner) => {
        unionTypesWithNamedField(inner, fieldName)
      }

      case Kind.Model(name) => {
        service.models.find(_.name == name) match {
          case None => Nil
          case Some(model) => {
            model.fields.find(_.name == fieldName) match {
              case None => Nil
              case Some(_) => Seq(kind.toString)
            }
          }
        }
      }

      case Kind.Union(name) => {
        service.unions.find(_.name == name) match {
          case None => Nil
          case Some(u) => {
            unionTypesWithNamedField(u, fieldName).map { t =>
              s"${u.name}.$t"
            }
          }
        }
      }
    }
  }

  private def validateHeaders(headers: Seq[Header], location: String): Seq[String] = {
    val headersWithInvalidTypes = headers.flatMap { header =>
      typeResolver.parse(header.`type`) match {
        case None => Some(s"$location[${header.name}] type[${header.`type`}] is invalid")
        case Some(kind) => {
          kind match {
            case Kind.Enum(_) | Kind.Primitive("string") => None
            case Kind.List(Kind.Enum(_) | Kind.Primitive("string")) => None
            case Kind.Model(_) | Kind.Union(_) | Kind.Primitive(_) | Kind.List(_) | Kind.Map(_) => {
              Some(s"$location[${header.name}] type[${header.`type`}] is invalid: Must be a string or the name of an enum")
            }
          }
        }
      }
    }

    val duplicates = dupsError(location, headers.map(_.name))

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
    val nameErrors = service.models.flatMap { model =>
      model.fields.flatMap { field =>
        Text.validateName(field.name) match {
          case Nil => None
          case errors => {
            Some(s"Model[${model.name}] field name[${field.name}] is invalid: ${errors.mkString(" and ")}")
          }
        }
      }
    }

    val duplicateFieldErrors = service.models.flatMap { model =>
      dupsError(s"Model[${model.name}] field", model.fields.filterNot(_.name.isEmpty).map(_.name))
    }

    val typeErrors = service.models.flatMap { model =>
      model.fields.flatMap { field =>
        typeResolver.parse(field.`type`) match {
          case None => Some(s"${model.name}.${field.name} has invalid type[${field.`type`}]")
          case _ => None
        }
      }
    }

    nameErrors ++ duplicateFieldErrors ++ typeErrors
  }

  /**
   * Validates that any defaults specified for fields are valid:
   *   Any field with a default is required
   *   Valid based on the datatype
   *   If an enum, the default is listed as a value for that enum
   */
  private def validateFieldDefaults(): Seq[String] = {
    service.models.flatMap { model =>
      model.fields.flatMap { field =>
        field.default.flatMap { default =>
          field.required match {
            case false => Some(s"${model.name}.${field.name} has a default specified. It must be marked required")
            case true => validateDefault(s"${model.name}.${field.name}", field.`type`, default)
          }
        }
      }
    } match {
      case Nil => {
        service.models.flatMap { model =>
          model.fields.
            filter { f => f.minimum.isDefined || f.maximum.isDefined }.
            filter { f => f.default.isDefined && JsonUtil.isNumeric(f.default.get) }.
            flatMap { field =>
              field.default match {
                case None => Nil
                case Some(default) => {
                  Seq(
                    field.minimum.flatMap { min =>
                      if (default.toLong < min) {
                        Some(s"${model.name}.${field.name} default[$default] must be >= specified minimum[$min]")
                      } else {
                        None
                      }
                    },
                    field.maximum.flatMap { max =>
                      if (default.toLong > max) {
                        Some(s"${model.name}.${field.name} default[$default] must be <= specified maximum[$max]")
                      } else {
                        None
                      }
                    }
                  ).flatten
                }
              }
            }
        }
      }
      case errors => {
        errors
      }
    }
  }

  private def validateResources(): Seq[String] = {
    val datatypeErrors = service.resources.flatMap { resource =>
      typeResolver.parse(resource.`type`) match {
        case None => {
          Some(s"Resource[${resource.`type`}] has an invalid type. Must resolve to a known enum, model or primitive")
        }
        case Some(kind) => {
          kind match {
            case Kind.List(_) | Kind.Map(_) => {
              Some(s"Resource[${resource.`type`}] has an invalid type: must be a singleton (not a list nor map)")
            }
            case Kind.Model(_) | Kind.Enum(_) | Kind.Union(_) => {
              None
            }
            case Kind.Primitive(name) => {
              Some(s"Resource[${resource.`type`}] has an invalid type: Primitives cannot be mapped to resources")
            }
          }
        }
      }
    }

    val missingOperations = service.resources.filter { _.operations.isEmpty }.map { resource =>
      s"Resource[${resource.`type`}] must have at least one operation"
    }

    val duplicateModels = service.resources.flatMap { resource =>
      val numberResources = service.resources.count(_.`type` == resource.`type`)
      if (numberResources <= 1) {
        None
      } else {
        Some(s"Resource[${resource.`type`}] cannot appear multiple times")
      }
    }.distinct

    val duplicatePlurals = dupsError("Resource with plural", service.resources.map(_.plural))

    datatypeErrors ++ missingOperations ++ duplicateModels ++ duplicatePlurals
  }

  private def validateParameterLocations(): Seq[String] = {
    service.resources.flatMap { resource =>
      resource.operations.filter(_.parameters.nonEmpty).flatMap { op =>
        op.parameters.flatMap { param =>
          param.location match {
            case ParameterLocation.UNDEFINED(name) => Some(opLabel(resource, op, s"location[$name] is not recognized. Must be one of: ${ParameterLocation.all.map(_.toString.toLowerCase).mkString(", ")}"))
            case _ => None
          }
        }
      }
    }
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
      resource.operations.filter(op => op.body.isDefined && !Methods.isJsonDocumentMethod(op.method.toString)).map { op =>
        opLabel(resource, op, s"Cannot specify body for HTTP method[${op.method}]")
      }
    }

    typesNotFound ++ invalidMethods
  }

  private def validateParameterDefaults(): Seq[String] = {
    service.resources.flatMap { resource =>
      resource.operations.filter(_.parameters.nonEmpty).flatMap { op =>
        op.parameters.flatMap { param =>
          typeResolver.parse(param.`type`).flatMap { pd =>
            param.default match {
              case None => None
              case Some(default) => {
                if (param.required) {
                  validateDefault(opLabel(resource, op, s"param[${param.name}]"), param.`type`, default)
                } else {
                  Some(opLabel(resource, op, s"param[${param.name}] has a default specified. It must be marked required"))
                }
              }
            }
          }
        }
      }
    }
  }

  private def validateParameterNames(): Seq[String] = {
    service.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        dupsError(
          opLabel(resource, op, "Parameter"),
          op.parameters.map(_.name)
        )
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
            case Some(kind) => {
              p.location match {
                case ParameterLocation.Query | ParameterLocation.Header => {
                  // Query and Header parameters can only be primitives or enums
                  kind match {
                    case Kind.Enum(_) => {
                      None
                    }

                    case Kind.Primitive(_) if isValidInUrl(kind) => {
                      None
                    }

                    case Kind.Primitive(_) => {
                      Some(opLabel(resource, op, s"Parameter[${p.name}] has an invalid type[${p.`type`}]. Valid types for ${p.location.toString.toLowerCase} parameters are: enum, ${Primitives.ValidInPath.mkString(", ")}."))
                    }

                    case Kind.List(nested) if isValidInUrl(nested) => {
                      None
                    }

                    case Kind.List(_) => {
                      Some(opLabel(resource, op, s"Parameter[${p.name}] has an invalid type[${p.`type`}]. Valid nested types for lists in ${p.location.toString.toLowerCase} parameters are: enum, ${Primitives.ValidInPath.mkString(", ")}."))
                    }

                    case Kind.Model(_) | Kind.Union(_) => {
                      Some(opLabel(resource, op, s"Parameter[${p.name}] has an invalid type[${p.`type`}]. Model and union types are not supported as ${p.location.toString.toLowerCase} parameters."))
                    }

                    case Kind.Map(_) => {
                      Some(opLabel(resource, op, s"Parameter[${p.name}] has an invalid type[${p.`type`}]. Maps are not supported as ${p.location.toString.toLowerCase} parameters."))
                    }

                  }

                }

                case ParameterLocation.Path => {
                  // Path parameters are required
                  if (p.required) {
                    // Verify that path parameter is actually in the path and immediately before or after a '/'
                    if (!Util.namedParametersInPath(op.path).contains(p.name)) {
                      Some(opLabel(resource, op, s"path parameter[${p.name}] is missing from the path[${op.path}]"))
                    } else if (isValidInUrl(kind)) {
                      None
                    } else {
                      val errorTemplate = opLabel(resource, op, s"path parameter[${p.name}] has an invalid type[%s]. Valid types for path parameters are: enum, ${Primitives.ValidInPath.mkString(", ")}.")
                      Some(errorTemplate.format(kind.toString))
                    }
                  } else {
                    Some(opLabel(resource, op, s"path parameter[${p.name}] is specified as optional. All path parameters are required"))
                  }
                }

                case _ => {
                  None
                }
              }
            }
          }
        }
      }
    }
  }

  private def isValidInUrl(kind: Kind): Boolean = {
    kind match {
      case Kind.Primitive(name) => {
        Primitives.validInUrl(name)
      }
      case Kind.Enum(_) => {
        // Serializes as a string
        true
      }
      case Kind.Model(_) | Kind.Union(_) | Kind.List(_) | Kind.Map(_) => {
        false
      }
    }
  }

  private def responseCodeString(responseCode: ResponseCode): String = {
    responseCode match {
      case ResponseCodeOption.Default => ResponseCodeOption.Default.toString
      case ResponseCodeOption.UNDEFINED(value) => value
      case ResponseCodeInt(value) => value.toString
      case ResponseCodeUndefinedType(value) => value.toString
    }
  }

  private def validateResponses(): Seq[String] = {
    val invalidCodes = service.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.flatMap { r =>
          r.code match {
            case ResponseCodeOption.Default => None
            case ResponseCodeOption.UNDEFINED(value) => Some(s"Response code must be an integer or the keyword 'default' and not[$value]")
            case ResponseCodeUndefinedType(value) => Some(s"Response code must be an integer or the keyword 'default' and not[$value]")
            case ResponseCodeInt(code) => {
              if (code < 100) {
                Some(s"Response code[$code] must be >= 100")
              } else {
                None
              }
            }
          }
        }
      }
    }

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
              Some(opLabel(resource, op, s"response code[${responseCodeString(r.code)}] has an invalid type[${r.`type`}]."))
            }
            case Some(_) => None
          }
        }
      }
    }

    val mixed2xxResponseTypes = service.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        val types = op.responses.flatMap { r =>
          r.code match {
            case ResponseCodeInt(value) => {
              if (value >= 200 && value < 300) {
                Some(r.`type`)
              } else {
                None
              }
            }
            case ResponseCodeOption.Default | ResponseCodeOption.UNDEFINED(_) | ResponseCodeUndefinedType(_) => None
          }
        }.distinct

        if (types.size <= 1) {
          None
        } else {
          Some(s"Resource[${resource.`type`}] cannot have varying response types for 2xx response codes: ${types.sorted.mkString(", ")}")
        }
      }
    }

    val statusCodesRequiringUnit = Seq("204", "304")
    val noContentWithTypes = service.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.filter(r => statusCodesRequiringUnit.contains(responseCodeString(r.code)) && r.`type` != Primitives.Unit.toString).map { r =>
          opLabel(resource, op, s"response code[${responseCodeString(r.code)}] must return unit and not[${r.`type`}]")
        }
      }
    }

    val invalidHeaders = for {
      resource <- service.resources
      op <- resource.operations
      r <- op.responses
    } yield {
      validateHeaders(r.headers.getOrElse(Nil), opLabel(resource, op, s"response code[${responseCodeString(r.code)}] header"))
    }

    invalidCodes ++ invalidMethods ++ missingOrInvalidTypes ++ mixed2xxResponseTypes ++ noContentWithTypes ++ invalidHeaders.flatten
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
    values.groupBy(Text.camelCaseToUnderscore(_).toLowerCase.trim).filter { _._2.size > 1 }.keys
  }

  private def opLabel(
    resource: Resource,
    op: Operation,
    message: String
  ): String = {
    Seq(
      s"Resource[${resource.`type`}]",
      op.method.toString,
      op.path,
      message
    ).map(_.trim).filter(!_.isEmpty).mkString(" ")
  }


}
