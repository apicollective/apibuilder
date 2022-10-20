package lib

import play.api.libs.json.Json

import io.apibuilder.spec.v0.models._
import io.apibuilder.api.v0.models.{Diff, DiffBreaking, DiffNonBreaking}

object DiffFactories {
  object Material {
    def breaking(description: String): Diff = {
      DiffBreaking(
        description = description,
        isMaterial = true,
      )
    }
    def nonBreaking(description: String): Diff = {
      DiffNonBreaking(
        description = description,
        isMaterial = true,
      )
    }
  }

  object NotMaterial {
    def nonBreaking(description: String): Diff = {
      DiffNonBreaking(
        description = description,
        isMaterial = false,
      )
    }
  }
}

/**
  * Takes two service descriptions. Returns a list of changes from
  * service a to service b. The list of changes is intended to be
  * legible by a human.
  */
case class ServiceDiff(
  a: Service,
  b: Service
) {

  import DiffFactories._

  val differences: Seq[Diff] = Seq(
    diffInfo(),
    diffName(),
    diffOrganization(),
    diffApplication(),
    diffNamespace(),
    diffVersion(),
    diffBaseUrl(),
    diffDescription(),
    diffAttributes(),
    diffHeaders(),
    diffImports(),
    diffEnums(),
    diffInterfaces(),
    diffUnions(),
    diffModels(),
    diffResources(),
    diffAnnotations()
  ).flatten

  private[this] def diffInfo(): Seq[Diff] = {
    diffContact() ++ diffLicense()
  }

  private[this] def diffContact(): Seq[Diff] = {
    Helpers.diffOptionalStringNonBreakingNotMaterial("contact/name", a.info.contact.flatMap(_.name), b.info.contact.flatMap(_.name)) ++
    Helpers.diffOptionalStringNonBreakingNotMaterial("contact/url", a.info.contact.flatMap(_.url), b.info.contact.flatMap(_.url)) ++
    Helpers.diffOptionalStringNonBreakingNotMaterial("contact/email", a.info.contact.flatMap(_.email), b.info.contact.flatMap(_.email))
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
    Helpers.diffStringNonBreakingNotMaterial("version", a.version, b.version)
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
    val added = b.headers.map(_.name).filter(h => !a.headers.exists(_.name == h))

    a.headers.flatMap { headerA =>
      b.headers.find(_.name == headerA.name) match {
        case None => Some(Material.nonBreaking(Helpers.removed("header", headerA.name)))
        case Some(headerB) => diffHeader(headerA, headerB)
      }
    } ++ b.headers.find( h => added.contains(h.name) ).map { h =>
      if (h.required) {
        Material.breaking(Helpers.added("required header", h.name))
      } else {
        Material.nonBreaking(Helpers.added("optional header", h.name))
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
        case None => Some(NotMaterial.nonBreaking(Helpers.removed("import", importA.uri)))
        case Some(importB) => diffImport(importA, importB)
      }
    } ++ Helpers.findNewNotMaterial("import", a.imports.map(_.uri), b.imports.map(_.uri))
  }

  private[this] def diffImport(a: Import, b: Import): Seq[Diff] = {
    assert(a.uri == b.uri, "Import uri's must be the same")
    val prefix = s"import ${a.uri}"

    Helpers.diffStringNonBreakingNotMaterial(s"$prefix namespace", a.namespace, b.namespace) ++
    Helpers.diffStringNonBreakingNotMaterial(s"$prefix organization/key", a.organization.key, b.organization.key) ++
    Helpers.diffStringNonBreakingNotMaterial(s"$prefix application/key", a.application.key, b.application.key) ++
    Helpers.diffStringNonBreakingNotMaterial(s"$prefix version", a.version, b.version) ++
    Helpers.diffArrayNonBreakingNotMaterial(s"$prefix enums", a.enums, b.enums) ++
    Helpers.diffArrayNonBreakingNotMaterial(s"$prefix unions", a.unions, b.unions) ++
    Helpers.diffArrayNonBreakingNotMaterial(s"$prefix models", a.models, b.models)
  }

  private[this] def diffEnums(): Seq[Diff] = {
    a.enums.flatMap { enumA =>
      b.enums.find(_.name == enumA.name) match {
        case None => Some(Material.breaking(Helpers.removed("enum", enumA.name)))
        case Some(enumB) => diffEnum(enumA, enumB)
      }
    } ++ Helpers.findNew("enum", a.enums.map(_.name), b.enums.map(_.name))
  }

  private[this] def diffEnum(a: Enum, b: Enum): Seq[Diff] = {
    assert(a.name == b.name, "Enum names must be the same")
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
        case None => Some(Material.breaking(Helpers.removed(prefix, valueA.name)))
        case Some(valueB) => diffEnumValue(enumName, valueA, valueB)
      }
    } ++ Helpers.findNew(prefix, a.map(_.name), b.map(_.name))
  }

  private[this] def diffEnumValue(enumName: String, a: EnumValue, b: EnumValue): Seq[Diff] = {
    assert(a.name == b.name, "Enum value names must be the same")
    val prefix = s"enum $enumName value ${a.name}"

    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffAttributes(prefix, a.attributes, b.attributes) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation)
  }

  private[this] def diffUnions(): Seq[Diff] = {
    a.unions.flatMap { unionA =>
      b.unions.find(_.name == unionA.name) match {
        case None => Some(Material.breaking(Helpers.removed("union", unionA.name)))
        case Some(unionB) => diffUnion(unionA, unionB)
      }
    } ++ Helpers.findNew("union", a.unions.map(_.name), b.unions.map(_.name))
  }

  private[this] def diffUnion(a: Union, b: Union): Seq[Diff] = {
    assert(a.name == b.name, "Union names must be the same")
    val prefix = s"union ${a.name}"

    Helpers.diffOptionalStringBreaking(s"$prefix discriminator", a.discriminator, b.discriminator) ++
    Helpers.diffInterfaces(prefix, a.interfaces, b.interfaces) ++
    Helpers.diffStringNonBreaking(s"$prefix plural", a.plural, b.plural) ++
    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffAttributes(prefix, a.attributes, b.attributes) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation) ++
    diffUnionTypeDefault(a, b) ++
    diffUnionTypes(a.name, a.types, b.types)
  }

  private[this] def diffUnionTypeDefault(a: Union, b: Union): Seq[Diff] = {
    val prefix = s"union ${a.name} default type"

    val defaultTypeA: Option[String] = a.types.find(_.default.getOrElse(false)).map(_.`type`)
    val defaultTypeB: Option[String] = b.types.find(_.default.getOrElse(false)).map(_.`type`)

    (defaultTypeA, defaultTypeB) match {
      case (None, None) => Nil
      case (None, Some(typeNameB)) => Seq(Material.nonBreaking(Helpers.added(prefix, typeNameB)))
      case (Some(typeNameA), Some(typeNameB)) if typeNameA == typeNameB => Nil
      case (Some(typeNameA), Some(typeNameB)) => Seq(Material.breaking(Helpers.changed(prefix, typeNameA, typeNameB)))
      case (Some(typeNameA), None) => Seq(Material.breaking(Helpers.removed(prefix, typeNameA)))
    }
  }

  private[this] def diffUnionTypes(unionName: String, a: Seq[UnionType], b: Seq[UnionType]): Seq[Diff] = {
    val prefix = s"union $unionName type"

    a.flatMap { typeA =>
      b.find(_.`type` == typeA.`type`) match {
        case None => Some(Material.breaking(Helpers.removed(prefix, typeA.`type`)))
        case Some(typeB) => diffUnionType(unionName, typeA, typeB)
      }
    } ++ Helpers.findNew(prefix, a.map(_.`type`), b.map(_.`type`))
  }

  private[this] def diffUnionType(unionName: String, a: UnionType, b: UnionType): Seq[Diff] = {
    assert(a.`type` == b.`type`, "Union type names must be the same")
    val prefix = s"union $unionName type ${a.`type`}"

    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffAttributes(prefix, a.attributes, b.attributes) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation)
  }

  private[this] def diffInterfaces(): Seq[Diff] = {
    a.interfaces.flatMap { interfaceA =>
      b.interfaces.find(_.name == interfaceA.name) match {
        case None => Some(Material.breaking(Helpers.removed("interface", interfaceA.name)))
        case Some(interfaceB) => diffInterface(interfaceA, interfaceB)
      }
    } ++ Helpers.findNew("interface", a.interfaces.map(_.name), b.interfaces.map(_.name))
  }

  private[this] def diffInterface(a: Interface, b: Interface): Seq[Diff] = {
    assert(a.name == b.name, "Interface names must be the same")
    val prefix = s"interface ${a.name}"

    Helpers.diffStringNonBreaking(s"$prefix plural", a.plural, b.plural) ++
      Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
      Helpers.diffAttributes(prefix, a.attributes, b.attributes) ++
      Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation) ++
      diffFields(prefix, a.fields, b.fields)
  }

  private[this] def diffModels(): Seq[Diff] = {
    a.models.flatMap { modelA =>
      b.models.find(_.name == modelA.name) match {
        case None => Some(Material.breaking(Helpers.removed("model", modelA.name)))
        case Some(modelB) => diffModel(modelA, modelB)
      }
    } ++ Helpers.findNew("model", a.models.map(_.name), b.models.map(_.name))
  }

  private[this] def diffModel(a: Model, b: Model): Seq[Diff] = {
    assert(a.name == b.name, "Model names must be the same")
    val prefix = s"model ${a.name}"

    Helpers.diffStringNonBreaking(s"$prefix plural", a.plural, b.plural) ++
    Helpers.diffInterfaces(prefix, a.interfaces, b.interfaces) ++
    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffAttributes(prefix, a.attributes, b.attributes) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation) ++
    diffFields(prefix, a.fields, b.fields)
  }

  private[this] def diffFields(prefix: String, a: Seq[Field], b: Seq[Field]): Seq[Diff] = {
    val added = b.map(_.name).filterNot(h => a.exists(_.name == h))

    a.flatMap { fieldA =>
      b.find(_.name == fieldA.name) match {
        case None => Some(Material.breaking(Helpers.removed(s"$prefix field", fieldA.name)))
        case Some(fieldB) => diffField(s"$prefix field ${fieldA.name}", fieldA, fieldB)
      }
    } ++ b.filter( f => added.contains(f.name) ).map { f =>
      (f.required, f.default) match {
        case (false, None) => Material.nonBreaking(Helpers.added(s"$prefix optional field", f.name))
        case (false, Some(default)) => Material.nonBreaking(Helpers.added(s"$prefix optional field", s"${f.name}, defaults to ${Text.truncate(default)}"))
        case (true, None) => Material.breaking(Helpers.added(s"$prefix required field", f.name))
        case (true, Some(default)) => Material.nonBreaking(Helpers.added(s"$prefix required field", s"${f.name}, defaults to ${Text.truncate(default)}"))
      }
    }
  }

  private[this] def diffField(prefix: String, a: Field, b: Field): Seq[Diff] = {
    assert(a.name == b.name, "field names must be the same")

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
        case None => Some(Material.breaking(Helpers.removed("resource", resourceA.`type`)))
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

  private[this] def diffAnnotations(): Seq[Diff] = {
    a.annotations.flatMap { annotA =>
      b.annotations.find(_.name == annotA.name) match {
        case None => Some(Material.nonBreaking(Helpers.removed("annotation", annotA.name)))
        case Some(annotB) => diffAnnotation(annotA, annotB)
      }
    } ++ Helpers.findNew("annotation", a.annotations.map(_.name), b.annotations.map(_.name))
  }

  private[this] def diffAnnotation(a: Annotation, b: Annotation): Seq[Diff] = {
    assert(a.name == b.name, "Annotation names must be the same")
    val prefix = s"annotation ${a.name}"

    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
      Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation)
  }

  private[this] def operationKey(op: Operation): String = {
    s"${op.method.toString.toUpperCase} ${op.path}".trim
  }

  private[this] def diffOperations(resourceType: String, a: Seq[Operation], b: Seq[Operation]): Seq[Diff] = {
    val added = b.filter(opB => !a.exists(opA => operationKey(opB) == operationKey(opA)))
    val prefix = s"resource $resourceType"

    a.flatMap { opA =>
      b.find(opB => operationKey(opB) == operationKey(opA)) match {
        case None => Some(Material.breaking(Helpers.removed(s"$prefix operation", operationKey(opA))))
        case Some(opB) => diffOperation(resourceType, opA, opB)
      }
    } ++ added.map { op =>
      Material.nonBreaking(Helpers.added(s"$prefix operation", operationKey(op)))
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
      case (None, Some(_)) => Seq(Material.breaking(Helpers.added(prefix, "body")))
      case (Some(_), None) => Seq(Material.breaking(Helpers.removed(prefix, "body")))
      case (Some(bodyA), Some(bodyB)) => {
        Helpers.diffStringBreaking(s"$prefix body type", bodyA.`type`, bodyB.`type`) ++
        Helpers.diffOptionalStringNonBreaking(s"$prefix body description", bodyA.description, bodyB.description) ++
        Helpers.diffAttributes(prefix, bodyA.attributes, bodyB.attributes) ++
        Helpers.diffDeprecation(s"$prefix body", bodyA.deprecation, bodyB.deprecation)
      }
    }
  }

  private[this] def diffParameters(prefix: String, a: Seq[Parameter], b: Seq[Parameter]): Seq[Diff] = {
    val added = b.map(_.name).filterNot(h => a.exists(_.name == h))

    a.flatMap { parameterA =>
      b.find(_.name == parameterA.name) match {
        case None => Some(Material.breaking(Helpers.removed(s"$prefix parameter", parameterA.name)))
        case Some(parameterB) => diffParameter(prefix, parameterA, parameterB)
      }
    } ++ b.filter( p => added.contains(p.name) ).map { p =>
      (p.required, p.default) match {
        case (false, None) => Material.nonBreaking(Helpers.added(s"$prefix optional parameter", p.name))
        case (false, Some(default)) => Material.nonBreaking(Helpers.added(s"$prefix optional parameter", s"${p.name}, defaults to ${Text.truncate(default)}"))
        case (true, None) => Material.breaking(Helpers.added(s"$prefix required parameter", p.name))
        case (true, Some(default)) => Material.nonBreaking(Helpers.added(s"$prefix required parameter", s"${p.name}, defaults to ${Text.truncate(default)}"))
      }
    }
  }

  private[this] def diffParameter(prefix: String, a: Parameter, b: Parameter): Seq[Diff] = {
    assert(a.name == b.name, "Parameter names must be the same")
    val thisPrefix = s"$prefix parameter ${a.name}"

    Helpers.diffStringBreaking(s"$thisPrefix type", a.`type`, b.`type`) ++
    Helpers.diffStringBreaking(s"$thisPrefix location", a.location.toString, b.location.toString) ++
    Helpers.diffOptionalStringNonBreaking(s"$thisPrefix description", a.description, b.description) ++
    Helpers.diffDeprecation(thisPrefix, a.deprecation, b.deprecation) ++
    Helpers.diffOptionAttributes(prefix, a.attributes, b.attributes) ++
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
    val added = b.map(_.code).filterNot(code => a.exists(_.code == code))

    a.flatMap { responseA =>
      b.find(_.code == responseA.code) match {
        case None => Some(Material.breaking(Helpers.removed(s"$prefix response", responseCode(responseA))))
        case Some(responseB) => diffResponse(prefix, responseA, responseB)
      }
    } ++ b.filter( r => added.contains(r.code) ).map { r =>
      Material.nonBreaking(Helpers.added(s"$prefix response", responseCode(r)))
    }
  }

  private[this] def diffResponse(prefix: String, a: Response, b: Response): Seq[Diff] = {
    assert(responseCode(a) == responseCode(b), "Response codes must be the same")
    val thisPrefix = s"$prefix response ${responseCode(a)}"
    Helpers.diffStringBreaking(s"$thisPrefix type", a.`type`, b.`type`) ++
    Helpers.diffOptionalStringNonBreaking(s"$thisPrefix description", a.description, b.description) ++
    Helpers.diffDeprecation(thisPrefix, a.deprecation, b.deprecation) ++
    Helpers.diffOptionAttributes(prefix, a.attributes, b.attributes)
  }

  private object Helpers {

    def removed(label: String, value: String) = s"$label removed: ${Text.truncate(value)}"

    def added(label: String, value: String) = s"$label added: ${Text.truncate(value)}"

    def changed(label: String, from: String, to: String) = s"$label changed from ${Text.truncate(from)} to ${Text.truncate(to)}"

    def findNew(prefix: String, a: Seq[String], b: Seq[String]): Seq[Diff] = {
      b.filterNot(a.contains).map { name =>
        Material.nonBreaking(Helpers.added(prefix, name))
      }
    }

    def findNewNotMaterial(prefix: String, a: Seq[String], b: Seq[String]): Seq[Diff] = {
      b.filterNot(a.contains).map { name =>
        NotMaterial.nonBreaking(Helpers.added(prefix, name))
      }
    }

    def diffRequired(label: String, a: Boolean, b: Boolean): Seq[Diff] = {
      (a, b) match {
        case (true, true) => Nil
        case (false, false) => Nil
        case (true, false) => Seq(Material.nonBreaking(s"$label is no longer required"))
        case (false, true) => Seq(Material.breaking(s"$label is now required"))
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
              Seq(Material.breaking(desc))
            } else {
              Seq(Material.nonBreaking(desc))
            }
          }
        }
        case (None, Some(min)) => {
          Seq(Material.breaking(s"$label minimum added: $min"))
        }
        case (Some(min), None) => {
          Seq(Material.nonBreaking(s"$label minimum removed: $min"))
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
              Seq(Material.nonBreaking(desc))
            } else {
              Seq(Material.breaking(desc))
            }
          }
        }
        case (None, Some(max)) => {
          Seq(Material.breaking(s"$label maximum added: $max"))
        }
        case (Some(max), None) => {
          Seq(Material.nonBreaking(s"$label maximum removed: $max"))
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
            Seq(Material.nonBreaking(s"$label default changed from ${Text.truncate(from)} to ${Text.truncate(to)}"))
          }
        }
        case (None, Some(default)) => {
          Seq(Material.nonBreaking(s"$label default added: ${Text.truncate(default)}"))
        }
        case (Some(default), None) => {
          Seq(Material.breaking(s"$label default removed: ${Text.truncate(default)}"))
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
      diffString(label, a, b).map(Material.breaking)
    }

    def diffStringNonBreaking(
      label: String,
      a: String,
      b: String,
    ): Seq[Diff] = {
      diffString(label, a, b).map(Material.nonBreaking)
    }

    def diffStringNonBreakingNotMaterial(
      label: String,
      a: String,
      b: String,
    ): Seq[Diff] = {
      diffString(label, a, b).map(NotMaterial.nonBreaking)
    }

    def diffArrayNonBreaking(
      label: String,
      a: Seq[String],
      b: Seq[String]
    ): Seq[Diff] = {
      diffString(label, "[" + a.mkString(", ") + "]", "[" + b.mkString(", ") + "]").map(Material.nonBreaking)
    }

    def diffArrayNonBreakingNotMaterial(
      label: String,
      a: Seq[String],
      b: Seq[String]
    ): Seq[Diff] = {
      diffString(label, "[" + a.mkString(", ") + "]", "[" + b.mkString(", ") + "]").map(NotMaterial.nonBreaking)
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
      diffOptionalString(label, a, b).map(Material.breaking)
    }

    def diffOptionalStringNonBreaking(
      label: String,
      a: Option[String],
      b: Option[String]
    ): Seq[Diff] = {
      diffOptionalString(label, a, b).map(Material.nonBreaking)
    }

    def diffOptionalStringNonBreakingNotMaterial(
      label: String,
      a: Option[String],
      b: Option[String]
    ): Seq[Diff] = {
      diffOptionalString(label, a, b).map(NotMaterial.nonBreaking)
    }

    def diffDeprecation(prefix: String, a: Option[Deprecation], b: Option[Deprecation]): Seq[Diff] = {
      (a, b) match {
        case (None, None) => Nil
        case (Some(_), Some(_)) => Nil
        case (Some(_), None) => Seq(Material.nonBreaking(Helpers.removed(prefix, "deprecation")))
        case (None, Some(d)) => {
          d.description match {
            case None => Seq(Material.nonBreaking(s"$prefix deprecated"))
            case Some(desc) => Seq(Material.nonBreaking(s"$prefix deprecated: $desc"))
          }
        }
      }
    }

    def diffOptionAttributes(prefix: String, a: Option[Seq[Attribute]], b: Option[Seq[Attribute]]): Seq[Diff] = {
      diffAttributes(prefix, a.getOrElse(Nil), b.getOrElse(Nil))
    }

    def diffInterfaces(prefix: String, a: Seq[String], b: Seq[String]): Seq[Diff] = {
      def toDiff(message: String, elements: Seq[String]) = {
        if (elements.isEmpty) {
          Nil
        } else {
          Seq(Material.breaking(s"$message: ${elements.mkString(", ")}"))
        }
      }

      toDiff(s"$prefix interface added", b.filterNot(a.contains)) ++
        toDiff(s"$prefix interface removed", a.filterNot(b.contains))
    }

    def diffAttributes(prefix: String, a: Seq[Attribute], b: Seq[Attribute]): Seq[Diff] = {
      val aMap = Map(a map ( attr => attr.name -> attr ): _*)
      val bMap = Map(b map ( attr => attr.name -> attr ): _*)

      val aNames = aMap.keys.toSeq
      val bNames = bMap.keys.toSeq

      // Removed
      val removedNames = aNames diff bNames
      val removedDiffs = removedNames map (name => Material.nonBreaking(s"$prefix attribute removed: $name"))

      // Added
      val addedNames = bNames diff aNames
      val addedDiffs = addedNames map (name => Material.nonBreaking(s"$prefix attribute added: $name"))

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
