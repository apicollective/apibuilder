package lib

import com.gilt.apidoc.spec.v0.models._

sealed trait Difference {

  def description: String

}

object Difference {

  case class NonBreaking(description: String) extends Difference
  case class Breaking(description: String) extends Difference

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
    val addedHeaders = b.headers.map(_.name).filter(h => a.headers.find(_.name == h).isEmpty)

    a.headers.flatMap { headerA =>
      b.headers.find(_.name == headerA.name) match {
        case None => Some(Difference.NonBreaking(Helpers.removed("header", headerA.name)))
        case Some(headerB) => diffHeader(headerA, headerB)
      }
    } ++ b.headers.find( h => addedHeaders.contains(h.name) ).map { h =>
      h.required match {
        case false => Difference.NonBreaking(Helpers.added("optional header", h.name))
        case true => Difference.Breaking(Helpers.added("required header", h.name))
      }
    }
  }

  private def diffHeader(a: Header, b: Header): Seq[Difference] = {
    assert(a.name == b.name, "Header names must be the same")
    val prefix = s"header ${a.name}"

    Helpers.diffStringBreaking(s"$prefix type", a.`type`, b.`type`) ++
    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation) ++
    Helpers.diffRequired(s"$prefix required", a.required, b.required) ++
    Helpers.diffDefault(s"$prefix default", a.default, b.default)
  }

  private def diffImports(): Seq[Difference] = {
    val added = b.imports.map(_.uri).filter(uri => a.imports.find(_.uri == uri).isEmpty)

    a.imports.flatMap { importA =>
      b.imports.find(_.uri == importA.uri) match {
        case None => Some(Difference.NonBreaking(Helpers.removed("import", importA.uri)))
        case Some(importB) => diffImport(importA, importB)
      }
    } ++ added.map { uri =>
      Difference.NonBreaking(Helpers.added("import", uri))
    }
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
    val added = b.enums.map(_.name).filter(n => a.enums.find(_.name == n).isEmpty)

    a.enums.flatMap { enumA =>
      b.enums.find(_.name == enumA.name) match {
        case None => Some(Difference.NonBreaking(Helpers.removed("enum", enumA.name)))
        case Some(enumB) => diffEnum(enumA, enumB)
      }
    } ++ added.map { name =>
      Difference.NonBreaking(Helpers.added("enum", name))
    }
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
    val added = b.map(_.name).filter(n => a.find(_.name == n).isEmpty)
    val prefix = s"enum $enumName value"

    a.flatMap { valueA =>
      b.find(_.name == valueA.name) match {
        case None => Some(Difference.NonBreaking(Helpers.removed(prefix, valueA.name)))
        case Some(valueB) => diffEnumValue(enumName, valueA, valueB)
      }
    } ++ added.map { name =>
      Difference.NonBreaking(Helpers.added(prefix, name))
    }
  }

  private def diffEnumValue(enumName: String, a: EnumValue, b: EnumValue): Seq[Difference] = {
    assert(a.name == b.name, "Enum value name's must be the same")
    val prefix = s"enum $enumName value ${a.name}"

    Helpers.diffOptionalStringNonBreaking(s"$prefix description", a.description, b.description) ++
    Helpers.diffDeprecation(prefix, a.deprecation, b.deprecation)
  }

  private def diffUnions(): Seq[Difference] = Nil
  private def diffModels(): Seq[Difference] = Nil
  private def diffResources(): Seq[Difference] = Nil

  private object Helpers {

    def removed(label: String, value: String) = s"$label removed: ${Text.truncate(value)}"

    def added(label: String, value: String) = s"$label added: ${Text.truncate(value)}"

    def changed(label: String, from: String, to: String) = s"$label changed from ${Text.truncate(from)} to ${Text.truncate(to)}"

    def diffRequired(label: String, a: Boolean, b: Boolean): Seq[Difference] = {
      (a, b) match {
        case (true, true) => Nil
        case (false, false) => Nil
        case (true, false) => Nil
        case (false, true) => Seq(Difference.Breaking(s"$label is now required"))
      }
    }

    def diffDefault(label: String, a: Option[String], b: Option[String]): Seq[Difference] = {
      (a, b) match {
        case (None, None) => Nil
        case (Some(_), Some(_)) => Nil
        case (None, Some(_)) => Nil
        case (Some(_), None) => {
          Seq(Difference.Breaking(s"$label removed default"))
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
      diffString(label, a, b).map { Difference.Breaking(_) }
    }

    def diffStringNonBreaking(
      label: String,
      a: String,
      b: String
    ): Seq[Difference] = {
      diffString(label, a, b).map { Difference.NonBreaking(_) }
    }

    def diffArrayNonBreaking(
      label: String,
      a: Seq[String],
      b: Seq[String]
    ): Seq[Difference] = {
      diffString(label, "[" + a.mkString(", ") + "]", "[" + b.mkString(", ") + "]").map { Difference.NonBreaking(_) }
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
      diffOptionalString(label, a, b).map { Difference.NonBreaking(_) }
    }

    def diffDeprecation(prefix: String, a: Option[Deprecation], b: Option[Deprecation]): Seq[Difference] = {
      (a, b) match {
        case (None, None) => Nil
        case (Some(_), Some(_)) => Nil
        case (Some(_), None) => Seq(Difference.NonBreaking(Helpers.removed(prefix, "deprecation")))
        case (None, Some(d)) => {
          d.description match {
            case None => Seq(Difference.NonBreaking(s"$prefix deprecated"))
            case Some(desc) => Seq(Difference.NonBreaking(s"$prefix deprecated: $desc"))
          }
        }
      }
    }

  }

}
