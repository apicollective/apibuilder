package builder

import core.{TypeValidator, TypesProvider, Util}
import io.apibuilder.spec.v0.models._
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

  // TODO: Support import of interfaces
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
    validateInterfaces() ++
    validateModels() ++
    validateEnums() ++
    validateUnions() ++
    validateHeaders(service.headers, "Header") ++
    validateTypeNamesAreUnique() ++
    validateResources() ++
    validateParameterLocations() ++
    validateParameterBodies() ++
    validateParameterDefaults() ++
    validateParameterNames() ++
    validateParameters() ++
    validateResponses() ++
    validateGlobalAnnotations()
  }

  private def validateApidoc(): Seq[String] = {
    val specified = VersionTag(service.apidoc.version)
    specified.major match {
      case None => {
        val current = io.apibuilder.spec.v0.Constants.Version
        Seq(s"Invalid apidoc version[${service.apidoc.version}]. Latest version of apidoc specification is $current")
      }
      case Some(_) => {
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

  def validateName(prefix: String, name: String): Seq[String] = {
    name.trim match {
      case "" => Seq(s"$prefix name cannot be empty")
      case n => Text.validateName(n) match {
        case Nil => Nil
        case errors => Seq(s"$prefix name is invalid: ${errors.mkString(" and ")}")
      }
    }
  }

  private def validateInterfaces(): Seq[String] = {
    service.interfaces.flatMap { interface =>
      val p = s"Interface[${interface.name}]"
      validateName(p, interface.name) ++ validateFields(p, interface.fields)
    } ++
      dupsError("Interface", service.interfaces.map(_.name))
  }

  private def validateModels(): Seq[String] = {
    val modelErrors = service.models.flatMap { model =>
      val p = s"Model[${model.name}]"

      validateName(p, model.name) ++
        validateFields(p, model.fields) ++
        model.interfaces.flatMap { n => validateInterfaceFields(p, n, model.fields) }
    }

    val atLeastOneFieldErrors = service.models.filter { _.fields.isEmpty }.map { model =>
      s"Model[${model.name}] must have at least one field"
    }

    val duplicates = dupsError("Model", service.models.map(_.name))

    modelErrors ++ atLeastOneFieldErrors ++ duplicates
  }

  def validateAnnotation(prefix: String, anno: String): Seq[String] = {
    if (service.annotations.map(_.name).contains(anno)){
      Nil
    } else {
      Seq(s"$prefix annotation[$anno] is invalid. Annotations must be defined.")
    }
  }

  def validateFields(prefix: String, fields: Seq[Field]) = {
    dupsError(s"$prefix field", fields.map(_.name)) ++
      fields.flatMap { f => validateField(s"$prefix Field[${f.name}]", f) }
  }

  def validateField(prefix: String, field: Field): Seq[String] = {
    validateName(prefix, field.name) ++
      validateType(prefix, field.`type`) ++
      validateRange(prefix, field.minimum, field.maximum) ++
      validateInRange(prefix, field.minimum, field.maximum, field.default) ++
      field.default.toSeq.flatMap { d => validateDefault(prefix, field.`type`, d) } ++
      validateAnnotations(prefix, field.annotations)
  }

  private def validateInterfaceFields(prefix: String, interfaceName: String, fields: Seq[Field]): Seq[String] = {
    service.interfaces.find(_.name == interfaceName) match {
      case None => Seq(s"$prefix Interface[$interfaceName] not found")
      case Some(i) => validateInterfaceFields(prefix, i, fields)
    }
  }

  private def validateInterfaceFields(prefix: String, interface: Interface, fields: Seq[Field]): Seq[String] = {
    interface.fields.flatMap { interfaceField =>
      fields.find(_.name == interfaceField.name) match {
        case None => {
          Seq(s"$prefix missing field '${interfaceField.name}' as defined in the interface '${interface.name}'")
        }
        case Some(modelField) => {
          if (interfaceField.`type` == modelField.`type`) {
            Nil
          } else {
            Seq(s"$prefix field '${modelField.name}' type '${modelField.`type`}' is invalid. Must match the '${interface.name}' interface which defines this field as type '${interfaceField.`type`}'")
          }
        }
      }
    }
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
    }.toList match {
      case Nil => {
        // Check uniqueness of the serialization values
        service.enums.flatMap { enum =>
          dups(enum.values.map { ev => ev.value.getOrElse(ev.name) }).map { value =>
            s"Enum[${enum.name}] value[$value] appears more than once"
          }
        }
      }
      case errs => errs
    }

    nameErrors ++ duplicates ++ valueErrors ++ validateEnumValues ++ valuesWithInvalidNames ++ duplicateValues
  }

  private def validateAnnotations(prefix: String, annotations: Seq[String]): Seq[String] = {
    annotations.flatMap { a => validateAnnotation(prefix, a) } ++
      dupsError(s"$prefix Annotation", annotations)

  }

  private def validateGlobalAnnotations(): Seq[String] = {
    val nameErrors = service.annotations.flatMap { anno =>
      Text.validateName(anno.name) match {
        case Nil => None
        case errors => {
          Some(s"Annotation[${anno.name}] name is invalid: ${errors.mkString(" and ")}")
        }
      }
    }

    val duplicates = dupsError("Annotation", service.annotations.map(_.name))

    nameErrors ++ duplicates
  }

  private[this] def validateEnumValues(): Seq[String] = {
    service.enums.flatMap { enum =>
      enum.values.flatMap { enumValue =>
        enumValue.value match {
          case None => Nil
          case Some(v) => Text.validateName(v) match {
            case Nil => Nil
            case errors => Seq(s"Enum[${enum.name}] value[$v] is invalid: ${errors.mkString(" and ")}")
          }
        }
      }
    }
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
      validateUnionTypes(s"Union[${union.name}]", union.types)
    }

    val discriminatorErrors = service.unions.flatMap { union =>
      union.discriminator match {
        case None => {
          union.types.filter { t => t.default.getOrElse(false) }.toList match {
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

    val duplicates = dupsError("Union", service.unions.map(_.name))

    nameErrors ++ typeErrors ++ invalidTypes ++ validateUnionCyclicReferences() ++ discriminatorErrors ++ duplicates ++ validateUnionTypeDiscriminatorValues()
  }

  // Validate that the type is NOT imported as there is
  // no way we could retroactively modify the imported
  // type to extend the union type that is only being
  // defined in this service.
  private def validateTypeLocal(prefix: String, typ: String): Seq[String] = {
    validateType(prefix, typ) match {
      case Nil => {
        localTypeResolver.parse(typ) match {
          case None => {
            Seq(s"$prefix is invalid. Cannot use an imported type as part of a union as there is no way to declare that the imported type expands the union type defined here.")
          }
          case Some(_) => Nil
        }
      }
      case _ => Nil // another type error already found
    }
  }

  private def validateTypeNotUnit(prefix: String, typ: String): Seq[String] = {
    localTypeResolver.parse(typ) match {
      case Some(kind: Kind.Primitive) if kind.name == "unit" => {
        Seq(s"$prefix Union types cannot contain unit. To make a particular field optional, use the required property.")
      }
      case _ => Nil
    }
  }

  private def validateUnionTypes(prefix: String, types: Seq[UnionType]): Seq[String] = {
    types.flatMap { t =>
      validateType(s"$prefix", t.`type`) ++
        validateTypeNotUnit(prefix, t.`type`) ++
        validateTypeLocal(s"$prefix Type[${t.`type`}]", t.`type`) ++
        dupsError(s"$prefix Type", types.map(_.`type`))
    } ++ validateUnionTypeDefaults(prefix, types)
  }

  private def validateUnionTypeDefaults(prefix: String, types: Seq[UnionType]): Seq[String] = {
    val defaultTypes = types.filter(_.default.getOrElse(false)).map(_.`type`)
    if (defaultTypes.size > 1) {
      Seq(s"$prefix Only 1 type can be specified as default. Currently the following types are marked as default: ${defaultTypes.toList.sorted.mkString(", ")}")
    } else {
      Nil
    }
  }

  private[this] def validateUnionTypeDiscriminatorValues(): Seq[String] = {
    validateUnionTypeDiscriminatorValuesValidNames() ++
      validateUnionTypeDiscriminatorValuesAreDistinct() ++
      validateUnionTypeDiscriminatorKeyValuesAreUniquePerModel()
  }

  private[this] def validateUnionTypeDiscriminatorValuesValidNames(): Seq[String] = {
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

  private[this] def validateUnionCyclicReferences(): Seq[String] = {
    val unionNames = service.unions.map(_.name).toSet

    val unionDeps: Map[String, Set[String]] = service.unions.map { union =>
      union.name -> union.types.collect {
        case t if unionNames.contains(t.`type`) => t.`type`
      }.toSet
    }.toMap

    def findCycle(union: String, acc: List[String]): Option[List[String]] = {
      if (acc.contains(union)) {
        Some(acc)
      } else {
        val newAcc = union :: acc
        unionDeps(union).foldLeft(None: Option[List[String]]) { case (res, child) =>
          res.orElse(findCycle(child, newAcc))
        }
      }
    }

    val allCycles = service.unions.flatMap { union => findCycle(union.name, Nil) }

    val deduped = allCycles.groupBy(_.sorted).values.toSeq.map(_.head)

    deduped.map { cycle =>
      if (cycle.size == 1) {
        s"Union[${cycle.head}] cannot contain itself as one of its types or sub-types"
      } else {
        // need to reverse the order and duplicate the start of the cycle, so that c->b->a becomes a->b->c->a
        val reversed = (cycle.last :: cycle).reverse
        s"Union[${reversed.head}] cannot contain itself as one of its types or sub-types: ${reversed.mkString("->")}"
      }
    }
  }

  private[this] def validateUnionTypeDiscriminatorValuesAreDistinct(): Seq[String] = {
    service.unions.flatMap { union =>
      dupsError(
        s"Union[${union.name}] discriminator values",
        union.types.map(getDiscriminatorValue)
      )
    }
  }

  private[this] def getDiscriminatorValue(unionType: UnionType): String =
    unionType.discriminatorValue.getOrElse(unionType.`type`)

  private[this] def validateUnionTypeDiscriminatorKeyValuesAreUniquePerModel(): Seq[String] =
    service
      .unions
      .flatMap(u => u.types.map(t => (u, t)))
      // model -> unions
      .groupBy { case (_, unionType) => unionType.`type` }
      .flatMap { case (modelName, unions) => validateUniqueDiscriminatorKeyValues(modelName, unions) }
      .toSeq


  private def validateUniqueDiscriminatorKeyValues(modelName: String, unions: Seq[(Union, UnionType)]): Seq[String] = {
    lazy val unionNames = unions.map { case (union, _) => union.name }.mkString(", ")

    def getMessageError(elementKey: String) =
      s"Model[$modelName] used in unions[$unionNames] cannot use more than one discriminator $elementKey."

    def getMessage(elementKey: String, elements: String) =
      getMessageError(elementKey) + s" Found distinct discriminator ${elementKey}s[$elements]."

    // keys
    val distinctKeys = unions.map { case (union, _) => union.discriminator }.distinct
    val nameError =
      if (distinctKeys.size > 1)
        if (distinctKeys.contains(None))
          Some(getMessageError(elementKey = "name") +
            s" All unions should define the same discriminator name, or not define one at all.")
        else
          Some(getMessage(elementKey = "name", distinctKeys.flatten.mkString(", ")))
      else
        None

    // values
    val distinctValues = unions.map { case (_, unionType) => getDiscriminatorValue(unionType) }.distinct
    val valueError =
      if (distinctValues.size > 1)
        Some(getMessage(elementKey = "value", distinctValues.mkString(", ")))
      else
        None

    nameError.toSeq ++ valueError
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

  private[this] case class TypesUniqueValidator(label: String, names: Seq[String]) {
    val all: Set[String] = names.map(_.toLowerCase()).toSet
    def contains(name: String): Boolean = all.contains(name.toLowerCase())
  }

  /**
    * While not strictly necessary, we do this to reduce
    * confusion. Otherwise we would require an extension to
    * always indicate if a type referenced a model, union, or enum.
    */
  private def validateTypeNamesAreUnique(): Seq[String] = {
    val validators: Seq[TypesUniqueValidator] = Seq(
      TypesUniqueValidator("an interface", service.interfaces.map(_.name)),
      TypesUniqueValidator("a model", service.models.map(_.name)),
      TypesUniqueValidator("an enum", service.enums.map(_.name)),
      TypesUniqueValidator("a union", service.unions.map(_.name)),
    )

    validators.flatMap(_.all.toSeq)
      .groupBy { identity }
      .filter { case (_, v ) => v.size > 1 }
      .keys.toList.sorted
      .map { name =>
      val english = validators.filter(_.contains(name)).map(_.label).sorted.toList match {
        case one :: two :: Nil => s"both ${one} and ${two}"
        case all => all.mkString(", ")
      }
      s"Name[$name] cannot be used as the name of $english type"
    }
  }

  private def validateRange(prefix: String, minimum: Option[Long], maximum: Option[Long]): Seq[String] = {
    (minimum, maximum) match {
      case (Some(min), Some(max)) => {
        if (min <= max) {
          Nil
        } else {
          Seq(s"$prefix minimum[$min] must be <= specified maximum[$max]")
        }
      }
      case (_, _) => Nil
    }
  }

  private def validateInRange(prefix: String, minimum: Option[Long], maximum: Option[Long], value: Option[String]): Seq[String] = {
    value.flatMap(JsonUtil.parseBigDecimal) match {
      case None => Nil
      case Some(bd) => {
        val minErrors = minimum match {
          case Some(min) if bd < min => Seq(s"$prefix default[$bd] must be >= specified minimum[$min]")
          case _ => Nil
        }
        val maxErrors = maximum match {
          case Some(max) if bd > max => Seq(s"$prefix default[$bd] must be <= specified maximum[$max]")
          case _ => Nil
        }
        minErrors ++ maxErrors
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
      resource.operations.filter(op => op.body.isDefined && !Methods.supportsBody(op.method.toString)).map { op =>
        opLabel(resource, op, s"Cannot specify body for HTTP method[${op.method}]")
      }
    }

    typesNotFound ++ invalidMethods
  }

  private def validateParameterDefaults(): Seq[String] = {
    service.resources.flatMap { resource =>
      resource.operations.filter(_.parameters.nonEmpty).flatMap { op =>
        op.parameters.flatMap { param =>
          param.default match {
            case None => Nil
            case Some(default) => {
              if (param.required) {
                validateDefault(opLabel(resource, op, s"param[${param.name}]"), param.`type`, default)
              } else {
                Seq(opLabel(resource, op, s"param[${param.name}] has a default specified. It must be marked required"))
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

  private def validateType(prefix: String, `type`: String): Seq[String] = {
    typeResolver.parse(`type`) match {
      case None => Seq(s"$prefix type[${`type`}] not found")
      case Some(_) => Nil
    }
  }

  private def validateDefault(
    prefix: String,
    `type`: String,
    default: String
  ): Seq[String] = {
    typeResolver.parse(`type`) match {
      case None => Nil
      case Some(kind) => validator.validate(kind, default, Some(prefix)).toSeq
    }
  }

  private def dupsError(label: String, values: Iterable[String]): Seq[String] = {
    dups(values).map { n =>
      s"$label[$n] appears more than once"
    }
  }

  private def dups(values: Iterable[String]): List[String] = {
    values.groupBy(Text.camelCaseToUnderscore(_).toLowerCase.trim)
      .filter { _._2.size > 1 }
      .keys.toList.sorted
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
