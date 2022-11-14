package builder

import core.{TypeValidator, TypesProvider, Util}
import io.apibuilder.spec.v0.models._
import lib._

import scala.annotation.tailrec

case class ServiceSpecValidator(
  service: Service,
) {

  private val ReservedDiscriminatorValues = Seq("value", "implicit")

  private val localTypeResolver = TypesProvider.FromService(service).resolver

  private val typeResolver = DatatypeResolver(
    enumNames = localTypeResolver.enumNames ++ service.imports.flatMap { service =>
      service.enums.map { enum =>
        s"${service.namespace}.enums.$enum"
      }
    },

    interfaceNames = localTypeResolver.interfaceNames ++ service.imports.flatMap { service =>
      service.interfaces.map { interface =>
        s"${service.namespace}.interfaces.$interface"
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
    validateName() ++
    validateBaseUrl() ++
    validateInterfaces() ++
    validateModels() ++
    validateEnums() ++
    validateUnions() ++
    validateHeaders(service.headers, "Header") ++
    validateTypeNamesAreUnique() ++
    validateResources() ++
    validateResourceLocations() ++
    validateResourceBodies() ++
    validateResourceDefaults() ++
    validateResourceNames() ++
    validateParameters() ++
    validateResponses() ++
    validateGlobalAnnotations()
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
      DuplicateErrorMessage.message("Interface", service.interfaces.map(_.name))
  }

  private def validateModels(): Seq[String] = {
    service.models.flatMap { model =>
      val p = s"Model[${model.name}]"

      validateName(p, model.name) ++
        validateFields(p, model.fields) ++
        model.interfaces.flatMap { n => validateInterfaceFields(p, n, model.fields) }
    } ++ DuplicateErrorMessage.message("Model", service.models.map(_.name))
  }

  def validateAnnotation(prefix: String, anno: String): Seq[String] = {
    if (service.annotations.map(_.name).contains(anno)){
      Nil
    } else {
      Seq(s"$prefix annotation[$anno] is invalid. Annotations must be defined.")
    }
  }

  def validateFields(prefix: String, fields: Seq[Field]): Seq[String] = {
    DuplicateErrorMessage.message(s"$prefix field", fields.map(_.name)) ++
      fields.flatMap { f => validateField(s"$prefix Field[${f.name}]", f) }
  }

  def validateField(prefix: String, field: Field): Seq[String] = {
    validateName(prefix, field.name) ++
      validateType(prefix, field.`type`) {
        case _: Kind.Interface => Some(s"$prefix type[${field.`type`}] is an interface and cannot be used as a field type. Specify the specific model you need or use a union type")
        case _: Kind.Primitive => None
        case _: Kind.Enum => None
        case _: Kind.Model => None
        case _: Kind.Union => None
        case _: Kind.List => None
        case _: Kind.Map => None
      } ++
      validateRange(prefix, field.minimum, field.maximum) ++
      validateInRange(prefix, field.minimum, field.maximum, field.default) ++
      field.default.toSeq.flatMap { d => validateDefault(prefix, field.`type`, d) } ++
      validateAnnotations(prefix, field.annotations)
  }

  private def validateInterfaceFields(prefix: String, interfaceName: String, fields: Seq[Field]): Seq[String] = {
    typeResolver.parse(interfaceName) {
      case _: Kind.Interface => true
      case _ => false
    } match {
      case Some(_: Kind.Interface) => {
        service.interfaces.find(_.name == interfaceName) match {
          case Some(i) => validateInterfaceFields(prefix, i, fields)
          case None => {
            // TODO: should we validate for imported interfaces?
            Nil
          }
        }
      }
      case _ => Seq(s"$prefix Interface[$interfaceName] not found")
    }
  }

  private def validateInterfaceFields(prefix: String, interface: Interface, fields: Seq[Field]): Seq[String] = {
    interface.fields.flatMap { interfaceField =>
      fields.find(_.name == interfaceField.name) match {
        case None => {
          Seq(s"$prefix missing field '${interfaceField.name}' as defined in the interface '${interface.name}'")
        }
        case Some(modelField) => {
          val fieldPrefix = s"$prefix field '${modelField.name}'"
          validateInterfaceFieldsType(fieldPrefix, interface, interfaceField, modelField) ++
            validateInterfaceFieldsRequired(fieldPrefix, interface, interfaceField, modelField)
        }
      }
    }
  }

  private def validateInterfaceFieldsType(prefix: String, interface: Interface, interfaceField: Field, modelField: Field): Seq[String] = {
    if (interfaceField.`type` == modelField.`type`) {
      Nil
    } else {
      Seq(s"$prefix type '${modelField.`type`}' is invalid. Must match the '${interface.name}' interface which defines this field as type '${interfaceField.`type`}'")
    }
  }

  private def validateInterfaceFieldsRequired(prefix: String, interface: Interface, interfaceField: Field, modelField: Field): Seq[String] = {
    if (interfaceField.required == modelField.required) {
      Nil
    } else if (interfaceField.required) {
      Seq(s"$prefix cannot be optional. Must match the '${interface.name}' interface which defines this field as required")
    } else {
      Seq(s"$prefix cannot be required. Must match the '${interface.name}' interface which defines this field as optional")
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

    val duplicates = DuplicateErrorMessage.message("Enum", service.enums.map(_.name))

    val valueErrors = service.enums.filter { _.values.isEmpty }.map { enum =>
      s"Enum[${enum.name}] must have at least one value"
    }

    val valuesWithInvalidNames = if (nameErrors.isEmpty) {
      service.enums.flatMap { enum =>
        enum.values.flatMap { value =>
          Text.validateName(value.name).toList match {
            case Nil => None
            case errors => Some(s"Enum[${enum.name}] name[${value.name}] is invalid: ${errors.mkString(", ")}")
          }
        }
      }
    } else {
      Nil
    }

    val duplicateValues = service.enums.flatMap { enum =>
      DuplicateErrorMessage.findDuplicates(enum.values.map(_.name)).map { value =>
        s"Enum[${enum.name}] value[$value] appears more than once"
      }
    }.toList match {
      case Nil => {
        // Check uniqueness of the serialization values
        service.enums.flatMap { enum =>
          DuplicateErrorMessage.findDuplicates(enum.values.map { ev => ev.value.getOrElse(ev.name) }).map { value =>
            s"Enum[${enum.name}] value[$value] appears more than once"
          }
        }
      }
      case errs => errs
    }

    nameErrors ++ duplicates ++ valueErrors ++ valuesWithInvalidNames ++ duplicateValues
  }

  private def validateAnnotations(prefix: String, annotations: Seq[String]): Seq[String] = {
    annotations.flatMap { a => validateAnnotation(prefix, a) } ++
      DuplicateErrorMessage.message(s"$prefix Annotation", annotations)

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

    val duplicates = DuplicateErrorMessage.message("Annotation", service.annotations.map(_.name))

    nameErrors ++ duplicates
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

    val invalidTypes = service.unions.filter(_.name.nonEmpty).flatMap { union =>
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

    val duplicates = DuplicateErrorMessage.message("Union", service.unions.map(_.name))

    // Only do additional validation if we have a valid discriminator
    val additionalDiscriminatorErrors = if (discriminatorErrors.isEmpty) {
      validateUnionTypeDiscriminatorValues() ++ validateUnionTypeDiscriminatorNames()
    } else {
      Nil
    }

    nameErrors ++ typeErrors ++ invalidTypes ++ validateUnionCyclicReferences() ++ discriminatorErrors ++ duplicates ++ additionalDiscriminatorErrors
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
        DuplicateErrorMessage.message(s"$prefix Type", types.map(_.`type`))
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

  private[this] def validateUnionTypeDiscriminatorNames(): Seq[String] = {
    service.unions.flatMap { union =>
      val all = getAllDiscriminatorNames(union).distinct
      if (all.length > 1) {
        union.discriminator match {
          case None => Some(s"Union[${union.name}] does not specify a discriminator yet one of the types does. Either add the same discriminator to this union or remove from the member types")
          case Some(d) => Some(s"Union[${union.name}] specifies a discriminator named '$d'. All member types must also specify this same discriminator")
        }
      } else {
        None
      }
    }
  }

  private[this] def getAllDiscriminatorNames(union: Union, resolved: Set[String] = Set.empty): Seq[Option[String]] = {
    if (resolved.contains(union.name))
      Nil
    else {
      val subUnions = union.types.flatMap(t => service.unions.find(_.name == t.`type`))
      Seq(union.discriminator) ++ subUnions.map(_.discriminator)
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
      DuplicateErrorMessage.message(
        s"Union[${union.name}] discriminator value",
        getAllDiscriminatorValues(union),
      )
    }
  }

  private[this] def getAllDiscriminatorValues(union: Union, resolved: Set[String] = Set.empty): Seq[String] = {
    if (resolved.contains(union.name))
      Nil
    else {
      val subUnions = union.types.flatMap(t => service.unions.find(_.name == t.`type`))
      union.types.map(getDiscriminatorValue) ++ subUnions.flatMap(su => getAllDiscriminatorValues(su, resolved + union.name))
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

  @tailrec
  private[this] def unionTypesWithNamedField(kind: Kind, fieldName: String): Seq[String] = {
    def findField(fields: Seq[Field]) = {
      fields.find(_.name == fieldName) match {
        case None => Nil
        case Some(_) => Seq(kind.toString)
      }
    }

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

      case Kind.Interface(name) => {
        service.interfaces.find(_.name == name) match {
          case None => Nil
          case Some(model) => findField(model.fields)
        }
      }

      case Kind.Model(name) => {
        service.models.find(_.name == name) match {
          case None => Nil
          case Some(model) => findField(model.fields)
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
            case Kind.Interface(_) | Kind.Model(_) | Kind.Union(_) | Kind.Primitive(_) | Kind.List(_) | Kind.Map(_) => {
              Some(s"$location[${header.name}] type[${header.`type`}] is invalid: Must be a string or the name of an enum")
            }
          }
        }
      }
    }

    val duplicates = DuplicateErrorMessage.message(location, headers.map(_.name))

    headersWithInvalidTypes ++ duplicates
  }

  private[this] case class TypesUniqueValidator(label: String, names: Seq[String]) {
    val all: Set[String] = names.map(_.toLowerCase()).toSet
    def contains(name: String): Boolean = all.contains(name.toLowerCase())
  }

  /**
    * While not strictly necessary, we do this to reduce
    * confusion. Otherwise we would require an extension to
    * always indicate if a type referenced a model, interface,
    * union, or enum.
    *
    * Note that a union name can overlap with an interface name
    * if the union lists that interface.
    */
  private def validateTypeNamesAreUnique(): Seq[String] = {
    val interfaceNames = service.interfaces.map(_.name)
    val validators: Seq[TypesUniqueValidator] = Seq(
      TypesUniqueValidator("an interface", interfaceNames),
      TypesUniqueValidator("a model", service.models.map(_.name)),
      TypesUniqueValidator("an enum", service.enums.map(_.name)),
      TypesUniqueValidator("a union", service.unions.filterNot { u =>
        u.interfaces.contains(u.name)
      }.map(_.name).filterNot(interfaceNames.contains)),
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
    } ++ validateUnionAndInterfaceNames()
  }

  private def validateUnionAndInterfaceNames(): Seq[String] = {
    val interfaceNames = service.interfaces.map(_.name)
    service.unions.filter { u =>
      interfaceNames.contains(u.name) && !u.interfaces.contains(u.name)
    }.map(_.name).toList match {
      case Nil => Nil
      case names => {
        names.map { n =>
          s"'$n' is defined as both a union and an interface. You must either make the names unique, or document in the union interfaces field that the type extends the '$n' interface."
        }
      }
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
            case Kind.Interface(_) | Kind.Model(_) | Kind.Enum(_) | Kind.Union(_) => {
              None
            }
            case Kind.Primitive(_) => {
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

    val duplicatePlurals = DuplicateErrorMessage.message("Resource with plural", service.resources.map(_.plural))

    datatypeErrors ++ missingOperations ++ duplicateModels ++ duplicatePlurals
  }

  private def validateResourceLocations(): Seq[String] = {
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

  private def validateResourceBodies(): Seq[String] = {
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

  private def validateResourceDefaults(): Seq[String] = {
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

  private def validateResourceNames(): Seq[String] = {
    service.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        DuplicateErrorMessage.message(
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

                    case Kind.Interface(_) | Kind.Model(_) | Kind.Union(_) => {
                      Some(opLabel(resource, op, s"Parameter[${p.name}] has an invalid type[${p.`type`}]. Interface, model and union types are not supported as ${p.location.toString.toLowerCase} parameters."))
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
      case Kind.Interface(_) | Kind.Model(_) | Kind.Union(_) | Kind.List(_) | Kind.Map(_) => {
        false
      }
    }
  }

  private def responseCodeString(responseCode: ResponseCode): String = {
    responseCode match {
      case ResponseCodeOption.Default => ResponseCodeOption.Default.toString
      case ResponseCodeOption.UNDEFINED(value) => value
      case ResponseCodeInt(value) => value.toString
      case ResponseCodeUndefinedType(value) => value
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

  private def validateType(prefix: String, `type`: String)(
    implicit validateType: Kind => Option[String] = {_ => None}
  ): Seq[String] = {
    typeResolver.parse(`type`) match {
      case None => Seq(s"$prefix type[${`type`}] not found")
      case Some(t) => {
        validateType(t) match {
          case None => Nil
          case Some(error) => Seq(error)
        }
      }
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
    ).map(_.trim).filter(_.nonEmpty).mkString(" ")
  }


}
