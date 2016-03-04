package lib

import play.api.libs.json.Json

import com.bryzek.apidoc.spec.v0.models._
import com.bryzek.apidoc.api.v0.models.{Diff, DiffBreaking, DiffNonBreaking}

/**
  * Takes two service descriptions. Returns a list of changes from
  * service a to service b. The list of changes is intended to be
  * legible by a human.
  */
case class ServiceDiff(
  a: Service,
  b: Service
) {

  val differences: Seq[Diff] = Seq(
    diffApidoc(),
    diffInfo(),
    diffName(),
    diffOrganization(),
    diffApplication(),
    diffNamespace(),
    diffVersion(),
    diffBaseUrl(),
    diffDescription(),
    diffAttributes(),
    diffHeaders,
    diffImports(),
    diffEnums(),
    diffUnions(),
    diffModels(),
    diffResources()
  ).flatten

  private[this] def diffApidoc(): Seq[Diff] = {
    Helpers.diffStringNonBreaking("apidoc/version", a.apidoc.version, b.apidoc.version)
  }

  private[this] def diffInfo(): Seq[Diff] = {
    diffContact() ++ diffLicense()
  }

  private[this] def diffContact(): Seq[Diff] = {
    Helpers.diffOptionalStringNonBreaking("contact/name", a.info.contact.flatMap(_.name), b.info.contact.flatMap(_.name)) ++
    Helpers.diffOptionalStringNonBreaking("contact/url", a.info.contact.flatMap(_.url), b.info.contact.flatMap(_.url)) ++
    Helpers.diffOptionalStringNonBreaking("contact/email", a.info.contact.flatMap(_.email), b.info.contact.flatMap(_.email))
  }

  private[this] def diffLicense(): Seq[Diff] = {
    Helpers.diffOptionalStringNonBreaking("license/name", a.info.license.map(_.name), b.info.license.map(_.name)) ++
    Helpers.diffOptionalStringNonBreaking("license/url", a.info.license.flatMap(_.url), b.info.license.flatMap(_.url))
  }

  private[this] def diffName(): Seq[Diff] = {
    Helpers.diffStringNonBreaking("name", a.name, b.name)
  }

  private[this] def diffOrganization(): Seq[Diff] = {
    Helpers.diffStringNonBreaking("organization/key", a.organization.key, b.organization.key)
  }

  private[this] def diffApplication(): Seq[Diff] = {
    Helpers.diffStringNonBreaking("application/key", a.application.key, b.application.key)
  }

  private[this] def diffNamespace(): Seq[Diff] = {
    Helpers.diffStringBreaking("namespace", a.namespace, b.namespace)
  }

  private[this] def diffVersion(): Seq[Diff] = {
    Helpers.diffStringNonBreaking("version", a.version, b.version)
  }

  private[this] def diffBaseUrl(): Seq[Diff] = {
    Helpers.diffOptionalStringNonBreaking("base_url", a.baseUrl, b.baseUrl)
  }

  private[this] def diffDescription(): Seq[Diff] = {
    Helpers.diffOptionalStringNonBreaking("description", a.description, b.description)
  }

  private[this] def diffAttributes(): Seq[Diff] = {
    Helpers.diffAttributes("attributes", a.attributes, b.attributes)
  }

  private[this] def diffHeaders(): Seq[Diff] = {
    val added = b.headers.map(_.name).filter(h => a.headers.find(_.name == h).isEmpty)

    a.headers.flatMap { headerA =>
      b.headers.find(_.name == headerA.name) match {
        case None => Some(DiffNonBreaking(Helpers.removed("header", headerA.name)))
        case Some(headerB) => diffHeader(headerA, headerB)
      }
    } ++ b.headers.find( h => added.contains(h.name) ).map { h =>
      h.required match {
        case false => DiffNonBreaking(Helpers.added("optional header", h.name))
        case true => DiffBreaking(Helpers.added("required header", h.name))
      }
    }
  }

  private[this] def diffHeader(a: Header, b: Header): Seq[Diff] = {
    assert(a.name == b.name, "Header names must be the same")
    val prefix = s"header ${a.name}"

    Helpers.diffStringBreaking(s"$prefix type", a.`type`, b.`type`) ++
    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation) ++
    Helpers.diffRequired(prefix, a.required, b.required) ++
    Helpers.diffDefault(prefix, a.default, b.default) ++
    Helpers.diffAttributes(prefix, a.attributes, b.attributes)
  }

  private[this] def diffImports(): Seq[Diff] = {
    a.imports.flatMap { importA =>
      b.imports.find(_.uri == importA.uri) match {
        case None => Some(DiffNonBreaking(Helpers.removed("import", importA.uri)))
        case Some(importB) => diffImport(importA, importB)
      }
    } ++ Helpers.findNew("import", a.imports.map(_.uri), b.imports.map(_.uri))
  }

  private[this] def diffImport(a: Import, b: Import): Seq[Diff] = {
    assert(a.uri == b.uri, "Import uri's must be the same")
    val prefix = s"import ${a.uri}"

    Helpers.diffStringNonBreaking(s"$prefix namespace", a.namespace, b.namespace) ++
    Helpers.diffStringNonBreaking(s"$prefix organization/key", a.organization.key, b.organization.key) ++
    Helpers.diffStringNonBreaking(s"$prefix application/key", a.application.key, b.application.key) ++
    Helpers.diffStringNonBreaking(s"$prefix version", a.version, b.version) ++
    Helpers.diffArrayNonBreaking(s"$prefix enums", a.enums, b.enums) ++
    Helpers.diffArrayNonBreaking(s"$prefix unions", a.unions, b.unions) ++
    Helpers.diffArrayNonBreaking(s"$prefix models", a.models, b.models)
  }

  private[this] def diffEnums(): Seq[Diff] = {
    a.enums.flatMap { enumA =>
      b.enums.find(_.name == enumA.name) match {
        case None => Some(DiffBreaking(Helpers.removed("enum", enumA.name)))
        case Some(enumB) => diffEnum(enumA, enumB)
      }
    } ++ Helpers.findNew("enum", a.enums.map(_.name), b.enums.map(_.name))
  }

  private[this] def diffEnum(a: Enum, b: Enum): Seq[Diff] = {
    assert(a.name == b.name, "Enum name's must be the same")
    val prefix = s"enum ${a.name}"

    Helpers.diffStringNonBreaking(s"$prefix plural", a.plural, b.plural) ++
    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffAttributes(prefix, a.attributes, b.attributes) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation) ++
    diffEnumValues(a.name, a.values, b.values)
  }

  private[this] def diffEnumValues(enumName: String, a: Seq[EnumValue], b: Seq[EnumValue]): Seq[Diff] = {
    val prefix = s"enum $enumName value"

    a.flatMap { valueA =>
      b.find(_.name == valueA.name) match {
        case None => Some(DiffBreaking(Helpers.removed(prefix, valueA.name)))
        case Some(valueB) => diffEnumValue(enumName, valueA, valueB)
      }
    } ++ Helpers.findNew(prefix, a.map(_.name), b.map(_.name))
  }

  private[this] def diffEnumValue(enumName: String, a: EnumValue, b: EnumValue): Seq[Diff] = {
    assert(a.name == b.name, "Enum value name's must be the same")
    val prefix = s"enum $enumName value ${a.name}"

    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffAttributes(prefix, a.attributes, b.attributes) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation)
  }

  private[this] def diffUnions(): Seq[Diff] = {
    a.unions.flatMap { unionA =>
      b.unions.find(_.name == unionA.name) match {
        case None => Some(DiffBreaking(Helpers.removed("union", unionA.name)))
        case Some(unionB) => diffUnion(unionA, unionB)
      }
    } ++ Helpers.findNew("union", a.unions.map(_.name), b.unions.map(_.name))
  }

  private[this] def diffUnion(a: Union, b: Union): Seq[Diff] = {
    assert(a.name == b.name, "Union name's must be the same")
    val prefix = s"union ${a.name}"

    Helpers.diffOptionalStringBreaking(s"$prefix discriminator", a.discriminator, b.discriminator) ++
    Helpers.diffStringNonBreaking(s"$prefix plural", a.plural, b.plural) ++
    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffAttributes(prefix, a.attributes, b.attributes) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation) ++
    diffUnionTypes(a.name, a.types, b.types)
  }

  private[this] def diffUnionTypes(unionName: String, a: Seq[UnionType], b: Seq[UnionType]): Seq[Diff] = {
    val prefix = s"union $unionName type"

    a.flatMap { typeA =>
      b.find(_.`type` == typeA.`type`) match {
        case None => Some(DiffBreaking(Helpers.removed(prefix, typeA.`type`)))
        case Some(typeB) => diffUnionType(unionName, typeA, typeB)
      }
    } ++ Helpers.findNew(prefix, a.map(_.`type`), b.map(_.`type`))
  }

  private[this] def diffUnionType(unionName: String, a: UnionType, b: UnionType): Seq[Diff] = {
    assert(a.`type` == b.`type`, "Union type name's must be the same")
    val prefix = s"union $unionName type ${a.`type`}"

    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffAttributes(prefix, a.attributes, b.attributes) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation)
  }

  private[this] def diffModels(): Seq[Diff] = {
    a.models.flatMap { modelA =>
      b.models.find(_.name == modelA.name) match {
        case None => Some(DiffBreaking(Helpers.removed("model", modelA.name)))
        case Some(modelB) => diffModel(modelA, modelB)
      }
    } ++ Helpers.findNew("model", a.models.map(_.name), b.models.map(_.name))
  }

  private[this] def diffModel(a: Model, b: Model): Seq[Diff] = {
    assert(a.name == b.name, "Model name's must be the same")
    val prefix = s"model ${a.name}"

    Helpers.diffStringNonBreaking(s"$prefix plural", a.plural, b.plural) ++
    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffAttributes(prefix, a.attributes, b.attributes) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation) ++
    diffFields(a.name, a.fields, b.fields)
  }

  private[this] def diffFields(modelName: String, a: Seq[Field], b: Seq[Field]): Seq[Diff] = {
    val added = b.map(_.name).filter(h => a.find(_.name == h).isEmpty)
    val prefix = s"model $modelName"

    a.flatMap { fieldA =>
      b.find(_.name == fieldA.name) match {
        case None => Some(DiffBreaking(Helpers.removed(s"$prefix field", fieldA.name)))
        case Some(fieldB) => diffField(modelName, fieldA, fieldB)
      }
    } ++ b.filter( f => added.contains(f.name) ).map { f =>
      (f.required, f.default) match {
        case (false, None) => DiffNonBreaking(Helpers.added(s"$prefix optional field", f.name))
        case (false, Some(default)) => DiffNonBreaking(Helpers.added(s"$prefix optional field", s"${f.name}, defaults to ${Text.truncate(default)}"))
        case (true, None) => DiffBreaking(Helpers.added(s"$prefix required field", f.name))
        case (true, Some(default)) => DiffNonBreaking(Helpers.added(s"$prefix required field", s"${f.name}, defaults to ${Text.truncate(default)}"))
      }
    }
  }

  private[this] def diffField(modelName: String, a: Field, b: Field): Seq[Diff] = {
    assert(a.name == b.name, "Model field name's must be the same")
    val prefix = s"model $modelName field ${a.name}"

    Helpers.diffStringBreaking(s"$prefix type", a.`type`, b.`type`) ++
    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation) ++
    Helpers.diffDefault(prefix, a.default, b.default) ++
    Helpers.diffRequired(prefix, a.required, b.required) ++
    Helpers.diffAttributes(prefix, a.attributes, b.attributes) ++
    Helpers.diffMinimum(prefix, a.minimum, b.minimum) ++
    Helpers.diffMaximum(prefix, a.maximum, b.maximum) ++
    Helpers.diffOptionalStringNonBreaking(s"$prefix example", a.example, b.example)
  }

  private[this] def diffResources(): Seq[Diff] = {
    a.resources.flatMap { resourceA =>
      b.resources.find(_.`type` == resourceA.`type`) match {
        case None => Some(DiffBreaking(Helpers.removed("resource", resourceA.`type`)))
        case Some(resourceB) => diffResource(resourceA, resourceB)
      }
    } ++ Helpers.findNew("resource", a.resources.map(_.`type`), b.resources.map(_.`type`))
  }

  private[this] def diffResource(a: Resource, b: Resource): Seq[Diff] = {
    assert(a.`type` == b.`type`, "Resource types must be the same")
    val prefix = s"resource ${a.`type`}"

    Helpers.diffStringNonBreaking(s"$prefix plural", a.plural, b.plural) ++
    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffAttributes(prefix, a.attributes, b.attributes) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation) ++
    diffOperations(a.`type`, a.operations, b.operations)
  }

  private[this] def operationKey(op: Operation): String = {
    s"${op.method.toString.toUpperCase} ${op.path}".trim
  }

  private[this] def diffOperations(resourceType: String, a: Seq[Operation], b: Seq[Operation]): Seq[Diff] = {
    val added = b.filter(opB => a.find( opA => operationKey(opB) == operationKey(opA) ).isEmpty)
    val prefix = s"resource $resourceType"

    a.flatMap { opA =>
      b.find(opB => operationKey(opB) == operationKey(opA)) match {
        case None => Some(DiffBreaking(Helpers.removed(s"$prefix operation", operationKey(opA))))
        case Some(opB) => diffOperation(resourceType, opA, opB)
      }
    } ++ added.map { op =>
      DiffNonBreaking(Helpers.added(s"$prefix operation", operationKey(op)))
    }
  }

  private[this] def diffOperation(resourceType: String, a: Operation, b: Operation): Seq[Diff] = {
    assert(a.method == b.method, "Operation methods must be the same")
    assert(a.path == b.path, "Operation paths must be the same")
    val prefix = s"resource $resourceType operation " + operationKey(a)

    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffAttributes(prefix, a.attributes, b.attributes) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation) ++
    diffBody(prefix, a.body, b.body) ++
    diffParameters(prefix, a.parameters, b.parameters) ++
    diffResponses(prefix, a.responses, b.responses)
  }

  private[this] def diffBody(prefix: String, a: Option[Body], b: Option[Body]): Seq[Diff] = {
    (a, b) match {
      case (None, None) => Nil
      case (None, Some(bodyB)) => Seq(DiffBreaking(Helpers.added(prefix, "body")))
      case (Some(bodyB), None) => Seq(DiffBreaking(Helpers.removed(prefix, "body")))
      case (Some(bodyA), Some(bodyB)) => {
        Helpers.diffStringBreaking(s"$prefix body type", bodyA.`type`, bodyB.`type`) ++
        Helpers.diffOptionalStringNonBreaking(s"$prefix body description", bodyA.description, bodyB.description) ++
        Helpers.diffAttributes(prefix, bodyA.attributes, bodyB.attributes) ++
        Helpers.diffDeprecation(s"$prefix body", bodyA.deprecation, bodyB.deprecation)
      }
    }
  }

  private[this] def diffParameters(prefix: String, a: Seq[Parameter], b: Seq[Parameter]): Seq[Diff] = {
    val added = b.map(_.name).filter(h => a.find(_.name == h).isEmpty)

    a.flatMap { parameterA =>
      b.find(_.name == parameterA.name) match {
        case None => Some(DiffBreaking(Helpers.removed(s"$prefix parameter", parameterA.name)))
        case Some(parameterB) => diffParameter(prefix, parameterA, parameterB)
      }
    } ++ b.filter( p => added.contains(p.name) ).map { p =>
      (p.required, p.default) match {
        case (false, None) => DiffNonBreaking(Helpers.added(s"$prefix optional parameter", p.name))
        case (false, Some(default)) => DiffNonBreaking(Helpers.added(s"$prefix optional parameter", s"${p.name}, defaults to ${Text.truncate(default)}"))
        case (true, None) => DiffBreaking(Helpers.added(s"$prefix required parameter", p.name))
        case (true, Some(default)) => DiffNonBreaking(Helpers.added(s"$prefix required parameter", s"${p.name}, defaults to ${Text.truncate(default)}"))
      }
    }
  }

  private[this] def diffParameter(prefix: String, a: Parameter, b: Parameter): Seq[Diff] = {
    assert(a.name == b.name, "Parameter name's must be the same")
    val thisPrefix = s"$prefix parameter ${a.name}"

    Helpers.diffStringBreaking(s"$thisPrefix type", a.`type`, b.`type`) ++
    Helpers.diffStringBreaking(s"$thisPrefix location", a.location.toString, b.location.toString) ++
    Helpers.diffOptionalStringNonBreaking(s"$thisPrefix description", a.description, b.description) ++
    Helpers.diffDeprecation(thisPrefix, a.deprecation, b.deprecation) ++
    Helpers.diffDefault(thisPrefix, a.default, b.default) ++
    Helpers.diffRequired(thisPrefix, a.required, b.required) ++
    Helpers.diffMinimum(thisPrefix, a.minimum, b.minimum) ++
    Helpers.diffMaximum(thisPrefix, a.maximum, b.maximum) ++
    Helpers.diffOptionalStringNonBreaking(s"$thisPrefix example", a.example, b.example)
  }

  /**
    * Returns the response code as a string.
    */
  private[this] def responseCode(r: Response): String = {
    r.code match {
      case ResponseCodeInt(code) => code.toString
      case ResponseCodeUndefinedType(desc) => desc
      case ResponseCodeOption.Default => ResponseCodeOption.Default.toString
      case ResponseCodeOption.UNDEFINED(value) => value
    }
  }

  private[this] def diffResponses(prefix: String, a: Seq[Response], b: Seq[Response]): Seq[Diff] = {
    val added = b.map(_.code).filter(code => a.find(_.code == code).isEmpty)

    a.flatMap { responseA =>
      b.find(_.code == responseA.code) match {
        case None => Some(DiffBreaking(Helpers.removed(s"$prefix response", responseCode(responseA))))
        case Some(responseB) => diffResponse(prefix, responseA, responseB)
      }
    } ++ b.filter( r => added.contains(r.code) ).map { r =>
      DiffNonBreaking(Helpers.added(s"$prefix response", responseCode(r)))
    }
  }

  private[this] def diffResponse(prefix: String, a: Response, b: Response): Seq[Diff] = {
    assert(responseCode(a) == responseCode(b), "Response codes must be the same")
    val thisPrefix = s"$prefix response ${responseCode(a)}"
    Helpers.diffStringBreaking(s"$thisPrefix type", a.`type`, b.`type`) ++
    Helpers.diffOptionalStringNonBreaking(s"$thisPrefix description", a.description, b.description) ++
    Helpers.diffDeprecation(thisPrefix, a.deprecation, b.deprecation)
  }

  private object Helpers {

    def removed(label: String, value: String) = s"$label removed: ${Text.truncate(value)}"

    def added(label: String, value: String) = s"$label added: ${Text.truncate(value)}"

    def changed(label: String, from: String, to: String) = s"$label changed from ${Text.truncate(from)} to ${Text.truncate(to)}"

    def findNew(prefix: String, a: Seq[String], b: Seq[String]): Seq[Diff] = {
      b.filter(n => a.find(_ == n).isEmpty).map { name =>
        DiffNonBreaking(Helpers.added(prefix, name))
      }
    }

    def diffRequired(label: String, a: Boolean, b: Boolean): Seq[Diff] = {
      (a, b) match {
        case (true, true) => Nil
        case (false, false) => Nil
        case (true, false) => Seq(DiffNonBreaking(s"$label is no longer required"))
        case (false, true) => Seq(DiffBreaking(s"$label is now required"))
      }
    }

    /**
      * We consider a breaking change if a minimum is added or lowered.
      */
    def diffMinimum(label: String, a: Option[Long], b: Option[Long]): Seq[Diff] = {
      (a, b) match {
        case (None, None) => Nil
        case (Some(from), Some(to)) => {
          if (from == to) {
            Nil
          } else {
            val desc = s"$label minimum changed from $from to $to"
            if (from < to) {
              Seq(DiffBreaking(desc))
            } else {
              Seq(DiffNonBreaking(desc))
            }
          }
        }
        case (None, Some(min)) => {
          Seq(DiffBreaking(s"$label minimum added: $min"))
        }
        case (Some(min), None) => {
          Seq(DiffNonBreaking(s"$label minimum removed: $min"))
        }
      }
    }

    /**
      * We consider a breaking change if a maximum is added or increased.
      */
    def diffMaximum(label: String, a: Option[Long], b: Option[Long]): Seq[Diff] = {
      (a, b) match {
        case (None, None) => Nil
        case (Some(from), Some(to)) => {
          if (from == to) {
            Nil
          } else {
            val desc = s"$label maximum changed from $from to $to"
            if (from < to) {
              Seq(DiffNonBreaking(desc))
            } else {
              Seq(DiffBreaking(desc))
            }
          }
        }
        case (None, Some(max)) => {
          Seq(DiffBreaking(s"$label maximum added: $max"))
        }
        case (Some(max), None) => {
          Seq(DiffNonBreaking(s"$label maximum removed: $max"))
        }
      }
    }

    def diffDefault(label: String, a: Option[String], b: Option[String]): Seq[Diff] = {
      (a, b) match {
        case (None, None) => Nil
        case (Some(from), Some(to)) => {
          if (from == to) {
            Nil
          } else {
            Seq(DiffNonBreaking(s"$label default changed from ${Text.truncate(from)} to ${Text.truncate(to)}"))
          }
        }
        case (None, Some(default)) => {
          Seq(DiffNonBreaking(s"$label default added: ${Text.truncate(default)}"))
        }
        case (Some(default), None) => {
          Seq(DiffBreaking(s"$label default removed: ${Text.truncate(default)}"))
        }
      }
    }

    def diffString(
      label: String,
      a: String,
      b: String
    ): Seq[String] = {
      diffOptionalString(label, Some(a), Some(b))
    }

    def diffStringBreaking(
      label: String,
      a: String,
      b: String
    ): Seq[Diff] = {
      diffString(label, a, b).map { DiffBreaking(_) }
    }

    def diffStringNonBreaking(
      label: String,
      a: String,
      b: String
    ): Seq[Diff] = {
      diffString(label, a, b).map { DiffNonBreaking(_) }
    }

    def diffArrayNonBreaking(
      label: String,
      a: Seq[String],
      b: Seq[String]
    ): Seq[Diff] = {
      diffString(label, "[" + a.mkString(", ") + "]", "[" + b.mkString(", ") + "]").map { DiffNonBreaking(_) }
    }

    def diffOptionalString(
      label: String,
      a: Option[String],
      b: Option[String]
    ): Seq[String] = {
      (a, b) match {
        case (None, None) => Nil
        case (Some(value), None) => Seq(Helpers.removed(label, value))
        case (None, Some(value)) => Seq(Helpers.added(label, value))
        case (Some(valueA), Some(valueB)) => {
          if (valueA == valueB) {
            Nil
          } else {
            Seq(Helpers.changed(label, valueA, valueB))
          }
        }
      }
    }

    def diffOptionalStringBreaking(
      label: String,
      a: Option[String],
      b: Option[String]
    ): Seq[Diff] = {
      diffOptionalString(label, a, b).map { DiffBreaking(_) }
    }

    def diffOptionalStringNonBreaking(
      label: String,
      a: Option[String],
      b: Option[String]
    ): Seq[Diff] = {
      diffOptionalString(label, a, b).map { DiffNonBreaking(_) }
    }

    def diffDeprecation(prefix: String, a: Option[Deprecation], b: Option[Deprecation]): Seq[Diff] = {
      (a, b) match {
        case (None, None) => Nil
        case (Some(_), Some(_)) => Nil
        case (Some(_), None) => Seq(DiffNonBreaking(Helpers.removed(prefix, "deprecation")))
        case (None, Some(d)) => {
          d.description match {
            case None => Seq(DiffNonBreaking(s"$prefix deprecated"))
            case Some(desc) => Seq(DiffNonBreaking(s"$prefix deprecated: $desc"))
          }
        }
      }
    }

    def diffAttributes(prefix: String, a: Seq[Attribute], b: Seq[Attribute]): Seq[Diff] = {
      val aMap = Map(a map ( attr => attr.name -> attr ): _*)
      val bMap = Map(b map ( attr => attr.name -> attr ): _*)

      val aNames = aMap.keys.toSeq
      val bNames = bMap.keys.toSeq

      // Removed
      val removedNames = aNames diff bNames
      val removedDiffs = removedNames map (name => DiffNonBreaking(s"$prefix attribute removed: $name"))

      // Added
      val addedNames = bNames diff aNames
      val addedDiffs = addedNames map (name => DiffNonBreaking(s"$prefix attribute added: $name"))

      // Changed
      val namesInBoth = aNames intersect bNames
      val changedDiffs:Seq[Diff] = namesInBoth flatMap {name =>
        val aAttr = aMap(name)
        val bAttr = bMap(name)

        diffStringNonBreaking(s"$prefix attribute '$name' value", Json.stringify(aAttr.value), Json.stringify(bAttr.value)) ++
        diffOptionalStringNonBreaking(s"$prefix attribute '$name' description", aAttr.description, bAttr.description) ++
        diffDeprecation(s"$prefix attribute '$name'", aAttr.deprecation, bAttr.deprecation)
      }

      changedDiffs ++ removedDiffs ++ addedDiffs
    }
  }

}
