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
    diffDeprecation(prefix, a.deprecation, b.deprecation) ++
    Helpers.diffRequired(s"$prefix required", a.required, b.required) ++
    Helpers.diffDefault(s"$prefix default", a.default, b.default)
  }

  private def diffDeprecation(prefix: String, a: Option[Deprecation], b: Option[Deprecation]): Seq[Difference] = {
    (a, b) match {
      case (None, None) => Nil
      case (Some(_), Some(_)) => Nil
      case (Some(_), None) => Seq(Difference.NonBreaking(Helpers.removed(prefix, "deprecation")))
      case (None, Some(_)) => Seq(Difference.NonBreaking(Helpers.added(prefix, "deprecation")))
    }
  }

  private def diffImports(): Seq[Difference] = Nil
  private def diffEnums(): Seq[Difference] = Nil
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

  }

}
