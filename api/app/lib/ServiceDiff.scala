package lib

import com.gilt.apidoc.spec.v0.models._
import com.gilt.apidoc.internal.v0.models.{Difference, DifferenceBreaking, DifferenceNonBreaking}

/**
  * Takes two service descriptions. Returns a list of changes from
  * service a to service b. The list of changes is intended to be
  * legible by a human.
  */
case class ServiceDiff(
  a: Service,
  b: Service
) {

  val differences: Seq[Difference] = Seq(
    diffApidoc(),
    diffName(),
    diffOrganization(),
    diffApplication(),
    diffNamespace(),
    diffVersion(),
    diffBaseUrl(),
    diffDescription(),
    diffHeaders,
    diffImports(),
    diffEnums(),
    diffUnions(),
    diffModels(),
    diffResources()
  ).flatten

  private def diffApidoc(): Seq[Difference] = {
    Helpers.diffStringNonBreaking("apidoc/version", a.apidoc.version, b.apidoc.version)
  }

  private def diffName(): Seq[Difference] = {
    Helpers.diffStringNonBreaking("name", a.name, b.name)
  }

  private def diffOrganization(): Seq[Difference] = {
    Helpers.diffStringNonBreaking("organization/key", a.organization.key, b.organization.key)
  }

  private def diffApplication(): Seq[Difference] = {
    Helpers.diffStringNonBreaking("application/key", a.application.key, b.application.key)
  }

  private def diffNamespace(): Seq[Difference] = {
    Helpers.diffStringBreaking("namespace", a.namespace, b.namespace)
  }

  private def diffVersion(): Seq[Difference] = {
    Helpers.diffStringNonBreaking("version", a.version, b.version)
  }

  private def diffBaseUrl(): Seq[Difference] = {
    Helpers.diffOptionalStringNonBreaking("base_url", a.baseUrl, b.baseUrl)
  }

  private def diffDescription(): Seq[Difference] = {
    Helpers.diffOptionalStringNonBreaking("description", a.description, b.description)
  }

  private def diffHeaders(): Seq[Difference] = {
    val added = b.headers.map(_.name).filter(h => a.headers.find(_.name == h).isEmpty)

    a.headers.flatMap { headerA =>
      b.headers.find(_.name == headerA.name) match {
        case None => Some(DifferenceNonBreaking(Helpers.removed("header", headerA.name)))
        case Some(headerB) => diffHeader(headerA, headerB)
      }
    } ++ b.headers.find( h => added.contains(h.name) ).map { h =>
      h.required match {
        case false => DifferenceNonBreaking(Helpers.added("optional header", h.name))
        case true => DifferenceBreaking(Helpers.added("required header", h.name))
      }
    }
  }

  private def diffHeader(a: Header, b: Header): Seq[Difference] = {
    assert(a.name == b.name, "Header names must be the same")
    val prefix = s"header ${a.name}"

    Helpers.diffStringBreaking(s"$prefix type", a.`type`, b.`type`) ++
    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation) ++
    Helpers.diffRequired(prefix, a.required, b.required) ++
    Helpers.diffDefault(prefix, a.default, b.default)
  }

  private def diffImports(): Seq[Difference] = {
    a.imports.flatMap { importA =>
      b.imports.find(_.uri == importA.uri) match {
        case None => Some(DifferenceNonBreaking(Helpers.removed("import", importA.uri)))
        case Some(importB) => diffImport(importA, importB)
      }
    } ++ Helpers.findNew("import", a.imports.map(_.uri), b.imports.map(_.uri))
  }

  private def diffImport(a: Import, b: Import): Seq[Difference] = {
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

  private def diffEnums(): Seq[Difference] = {
    a.enums.flatMap { enumA =>
      b.enums.find(_.name == enumA.name) match {
        case None => Some(DifferenceBreaking(Helpers.removed("enum", enumA.name)))
        case Some(enumB) => diffEnum(enumA, enumB)
      }
    } ++ Helpers.findNew("enum", a.enums.map(_.name), b.enums.map(_.name))
  }

  private def diffEnum(a: Enum, b: Enum): Seq[Difference] = {
    assert(a.name == b.name, "Enum name's must be the same")
    val prefix = s"enum ${a.name}"

    Helpers.diffStringNonBreaking(s"$prefix plural", a.plural, b.plural) ++
    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation) ++
    diffEnumValues(a.name, a.values, b.values)
  }

  private def diffEnumValues(enumName: String, a: Seq[EnumValue], b: Seq[EnumValue]): Seq[Difference] = {
    val prefix = s"enum $enumName value"

    a.flatMap { valueA =>
      b.find(_.name == valueA.name) match {
        case None => Some(DifferenceBreaking(Helpers.removed(prefix, valueA.name)))
        case Some(valueB) => diffEnumValue(enumName, valueA, valueB)
      }
    } ++ Helpers.findNew(prefix, a.map(_.name), b.map(_.name))
  }

  private def diffEnumValue(enumName: String, a: EnumValue, b: EnumValue): Seq[Difference] = {
    assert(a.name == b.name, "Enum value name's must be the same")
    val prefix = s"enum $enumName value ${a.name}"

    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation)
  }

  private def diffUnions(): Seq[Difference] = {
    a.unions.flatMap { unionA =>
      b.unions.find(_.name == unionA.name) match {
        case None => Some(DifferenceBreaking(Helpers.removed("union", unionA.name)))
        case Some(unionB) => diffUnion(unionA, unionB)
      }
    } ++ Helpers.findNew("union", a.unions.map(_.name), b.unions.map(_.name))
  }

  private def diffUnion(a: Union, b: Union): Seq[Difference] = {
    assert(a.name == b.name, "Union name's must be the same")
    val prefix = s"union ${a.name}"

    Helpers.diffStringNonBreaking(s"$prefix plural", a.plural, b.plural) ++
    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation) ++
    diffUnionTypes(a.name, a.types, b.types)
  }

  private def diffUnionTypes(unionName: String, a: Seq[UnionType], b: Seq[UnionType]): Seq[Difference] = {
    val prefix = s"union $unionName type"

    a.flatMap { typeA =>
      b.find(_.`type` == typeA.`type`) match {
        case None => Some(DifferenceBreaking(Helpers.removed(prefix, typeA.`type`)))
        case Some(typeB) => diffUnionType(unionName, typeA, typeB)
      }
    } ++ Helpers.findNew(prefix, a.map(_.`type`), b.map(_.`type`))
  }

  private def diffUnionType(unionName: String, a: UnionType, b: UnionType): Seq[Difference] = {
    assert(a.`type` == b.`type`, "Union type name's must be the same")
    val prefix = s"union $unionName type ${a.`type`}"

    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation)
  }

  private def diffModels(): Seq[Difference] = {
    a.models.flatMap { modelA =>
      b.models.find(_.name == modelA.name) match {
        case None => Some(DifferenceBreaking(Helpers.removed("model", modelA.name)))
        case Some(modelB) => diffModel(modelA, modelB)
      }
    } ++ Helpers.findNew("model", a.models.map(_.name), b.models.map(_.name))
  }

  private def diffModel(a: Model, b: Model): Seq[Difference] = {
    assert(a.name == b.name, "Model name's must be the same")
    val prefix = s"model ${a.name}"

    Helpers.diffStringNonBreaking(s"$prefix plural", a.plural, b.plural) ++
    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation) ++
    diffFields(a.name, a.fields, b.fields)
  }

  private def diffFields(modelName: String, a: Seq[Field], b: Seq[Field]): Seq[Difference] = {
    val added = b.map(_.name).filter(h => a.find(_.name == h).isEmpty)
    val prefix = s"model $modelName"

    a.flatMap { fieldA =>
      b.find(_.name == fieldA.name) match {
        case None => Some(DifferenceBreaking(Helpers.removed(s"$prefix field", fieldA.name)))
        case Some(fieldB) => diffField(modelName, fieldA, fieldB)
      }
    } ++ b.filter( f => added.contains(f.name) ).map { f =>
      (f.required, f.default) match {
        case (false, None) => DifferenceNonBreaking(Helpers.added(s"$prefix optional field", f.name))
        case (false, Some(default)) => DifferenceNonBreaking(Helpers.added(s"$prefix optional field", s"${f.name}, defaults to ${Text.truncate(default)}"))
        case (true, None) => DifferenceBreaking(Helpers.added(s"$prefix required field", f.name))
        case (true, Some(default)) => DifferenceNonBreaking(Helpers.added(s"$prefix required field", s"${f.name}, defaults to ${Text.truncate(default)}"))
      }
    }
  }

  private def diffField(modelName: String, a: Field, b: Field): Seq[Difference] = {
    assert(a.name == b.name, "Model field name's must be the same")
    val prefix = s"model $modelName field ${a.name}"

    Helpers.diffStringBreaking(s"$prefix type", a.`type`, b.`type`) ++
    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation) ++
    Helpers.diffDefault(prefix, a.default, b.default) ++
    Helpers.diffRequired(prefix, a.required, b.required) ++
    Helpers.diffMinimum(prefix, a.minimum, b.minimum) ++
    Helpers.diffMaximum(prefix, a.maximum, b.maximum) ++
    Helpers.diffOptionalStringNonBreaking(s"$prefix example", a.example, b.example)
  }

  private def diffResources(): Seq[Difference] = {
    a.resources.flatMap { resourceA =>
      b.resources.find(_.`type` == resourceA.`type`) match {
        case None => Some(DifferenceBreaking(Helpers.removed("resource", resourceA.`type`)))
        case Some(resourceB) => diffResource(resourceA, resourceB)
      }
    } ++ Helpers.findNew("resource", a.resources.map(_.`type`), b.resources.map(_.`type`))
  }

  private def diffResource(a: Resource, b: Resource): Seq[Difference] = {
    assert(a.`type` == b.`type`, "Resource types must be the same")
    val prefix = s"resource ${a.`type`}"

    Helpers.diffStringNonBreaking(s"$prefix plural", a.plural, b.plural) ++
    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation) ++
    diffOperations(a.`type`, a.operations, b.operations)
  }

  private def operationKey(op: Operation): String = {
    s"${op.method.toString.toUpperCase} ${op.path}".trim
  }

  private def diffOperations(resourceType: String, a: Seq[Operation], b: Seq[Operation]): Seq[Difference] = {
    val added = b.filter(opB => a.find( opA => operationKey(opB) == operationKey(opA) ).isEmpty)
    val prefix = s"resource $resourceType"

    a.flatMap { opA =>
      b.find(opB => operationKey(opB) == operationKey(opA)) match {
        case None => Some(DifferenceBreaking(Helpers.removed(s"$prefix operation", operationKey(opA))))
        case Some(opB) => diffOperation(resourceType, opA, opB)
      }
    } ++ added.map { op =>
      DifferenceNonBreaking(Helpers.added(s"$prefix operation", operationKey(op)))
    }
  }

  private def diffOperation(resourceType: String, a: Operation, b: Operation): Seq[Difference] = {
    assert(a.method == b.method, "Operation methods must be the same")
    assert(a.path == b.path, "Operation paths must be the same")
    val prefix = s"resource $resourceType operation " + operationKey(a)

    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation) ++
    diffBody(prefix, a.body, b.body) ++
    diffParameters(prefix, a.parameters, b.parameters) ++
    diffResponses(prefix, a.responses, b.responses)
  }

  private def diffBody(prefix: String, a: Option[Body], b: Option[Body]): Seq[Difference] = {
    (a, b) match {
      case (None, None) => Nil
      case (None, Some(bodyB)) => Seq(DifferenceBreaking(Helpers.added(prefix, "body")))
      case (Some(bodyB), None) => Seq(DifferenceBreaking(Helpers.removed(prefix, "body")))
      case (Some(bodyA), Some(bodyB)) => {
        Helpers.diffStringBreaking(s"$prefix body type", bodyA.`type`, bodyB.`type`) ++
        Helpers.diffOptionalStringNonBreaking(s"$prefix body description", bodyA.description, bodyB.description) ++
        Helpers.diffDeprecation(s"$prefix body", bodyA.deprecation, bodyB.deprecation)
      }
    }
  }

  private def diffParameters(prefix: String, a: Seq[Parameter], b: Seq[Parameter]): Seq[Difference] = {
    val added = b.map(_.name).filter(h => a.find(_.name == h).isEmpty)

    a.flatMap { parameterA =>
      b.find(_.name == parameterA.name) match {
        case None => Some(DifferenceBreaking(Helpers.removed(s"$prefix parameter", parameterA.name)))
        case Some(parameterB) => diffParameter(prefix, parameterA, parameterB)
      }
    } ++ b.filter( p => added.contains(p.name) ).map { p =>
      (p.required, p.default) match {
        case (false, None) => DifferenceNonBreaking(Helpers.added(s"$prefix optional parameter", p.name))
        case (false, Some(default)) => DifferenceNonBreaking(Helpers.added(s"$prefix optional parameter", s"${p.name}, defaults to ${Text.truncate(default)}"))
        case (true, None) => DifferenceBreaking(Helpers.added(s"$prefix required parameter", p.name))
        case (true, Some(default)) => DifferenceNonBreaking(Helpers.added(s"$prefix required parameter", s"${p.name}, defaults to ${Text.truncate(default)}"))
      }
    }
  }

  private def diffParameter(prefix: String, a: Parameter, b: Parameter): Seq[Difference] = {
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
  private def responseCode(r: Response): String = {
    r.code match {
      case ResponseCodeInt(code) => code.toString
      case ResponseCodeUndefinedType(desc) => desc
      case ResponseCodeOption.Default => ResponseCodeOption.Default.toString
      case ResponseCodeOption.UNDEFINED(value) => value
    }
  }

  private def diffResponses(prefix: String, a: Seq[Response], b: Seq[Response]): Seq[Difference] = {
    val added = b.map(_.code).filter(code => a.find(_.code == code).isEmpty)

    a.flatMap { responseA =>
      b.find(_.code == responseA.code) match {
        case None => Some(DifferenceBreaking(Helpers.removed(s"$prefix response", responseCode(responseA))))
        case Some(responseB) => diffResponse(prefix, responseA, responseB)
      }
    } ++ b.filter( r => added.contains(r.code) ).map { r =>
      DifferenceNonBreaking(Helpers.added(s"$prefix response", responseCode(r)))
    }
  }

  private def diffResponse(prefix: String, a: Response, b: Response): Seq[Difference] = {
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

    def findNew(prefix: String, a: Seq[String], b: Seq[String]): Seq[Difference] = {
      b.filter(n => a.find(_ == n).isEmpty).map { name =>
        DifferenceNonBreaking(Helpers.added(prefix, name))
      }
    }

    def diffRequired(label: String, a: Boolean, b: Boolean): Seq[Difference] = {
      (a, b) match {
        case (true, true) => Nil
        case (false, false) => Nil
        case (true, false) => Seq(DifferenceNonBreaking(s"$label is no longer required"))
        case (false, true) => Seq(DifferenceBreaking(s"$label is now required"))
      }
    }

    /**
      * We consider a breaking change if a minimum is added or lowered.
      */
    def diffMinimum(label: String, a: Option[Long], b: Option[Long]): Seq[Difference] = {
      (a, b) match {
        case (None, None) => Nil
        case (Some(from), Some(to)) => {
          if (from == to) {
            Nil
          } else {
            val desc = s"$label minimum changed from $from to $to"
            if (from < to) {
              Seq(DifferenceBreaking(desc))
            } else {
              Seq(DifferenceNonBreaking(desc))
            }
          }
        }
        case (None, Some(min)) => {
          Seq(DifferenceBreaking(s"$label minimum added: $min"))
        }
        case (Some(min), None) => {
          Seq(DifferenceNonBreaking(s"$label minimum removed: $min"))
        }
      }
    }

    /**
      * We consider a breaking change if a maximum is added or increased.
      */
    def diffMaximum(label: String, a: Option[Long], b: Option[Long]): Seq[Difference] = {
      (a, b) match {
        case (None, None) => Nil
        case (Some(from), Some(to)) => {
          if (from == to) {
            Nil
          } else {
            val desc = s"$label maximum changed from $from to $to"
            if (from < to) {
              Seq(DifferenceNonBreaking(desc))
            } else {
              Seq(DifferenceBreaking(desc))
            }
          }
        }
        case (None, Some(max)) => {
          Seq(DifferenceBreaking(s"$label maximum added: $max"))
        }
        case (Some(max), None) => {
          Seq(DifferenceNonBreaking(s"$label maximum removed: $max"))
        }
      }
    }

    def diffDefault(label: String, a: Option[String], b: Option[String]): Seq[Difference] = {
      (a, b) match {
        case (None, None) => Nil
        case (Some(from), Some(to)) => {
          if (from == to) {
            Nil
          } else {
            Seq(DifferenceNonBreaking(s"$label default changed from ${Text.truncate(from)} to ${Text.truncate(to)}"))
          }
        }
        case (None, Some(default)) => {
          Seq(DifferenceNonBreaking(s"$label default added: ${Text.truncate(default)}"))
        }
        case (Some(default), None) => {
          Seq(DifferenceBreaking(s"$label default removed: ${Text.truncate(default)}"))
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
    ): Seq[Difference] = {
      diffString(label, a, b).map { DifferenceBreaking(_) }
    }

    def diffStringNonBreaking(
      label: String,
      a: String,
      b: String
    ): Seq[Difference] = {
      diffString(label, a, b).map { DifferenceNonBreaking(_) }
    }

    def diffArrayNonBreaking(
      label: String,
      a: Seq[String],
      b: Seq[String]
    ): Seq[Difference] = {
      diffString(label, "[" + a.mkString(", ") + "]", "[" + b.mkString(", ") + "]").map { DifferenceNonBreaking(_) }
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

    def diffOptionalStringNonBreaking(
      label: String,
      a: Option[String],
      b: Option[String]
    ): Seq[Difference] = {
      diffOptionalString(label, a, b).map { DifferenceNonBreaking(_) }
    }

    def diffDeprecation(prefix: String, a: Option[Deprecation], b: Option[Deprecation]): Seq[Difference] = {
      (a, b) match {
        case (None, None) => Nil
        case (Some(_), Some(_)) => Nil
        case (Some(_), None) => Seq(DifferenceNonBreaking(Helpers.removed(prefix, "deprecation")))
        case (None, Some(d)) => {
          d.description match {
            case None => Seq(DifferenceNonBreaking(s"$prefix deprecated"))
            case Some(desc) => Seq(DifferenceNonBreaking(s"$prefix deprecated: $desc"))
          }
        }
      }
    }

  }

}
