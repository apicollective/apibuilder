package lib

import com.gilt.apidoc.spec.v0.models.Service

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

  private def diffOptionalString(
    label: String,
    a: Option[String],
    b: Option[String]
  ): Seq[String] = {
    (a, b) match {
      case (None, None) => Nil
      case (Some(value), None) => Seq(s"$label removed: ${Text.truncate(value)}")
      case (None, Some(value)) => Seq(s"$label added: ${Text.truncate(value)}")
      case (Some(valueA), Some(valueB)) => {
        if (valueA == valueB) {
          Nil
        } else {
          Seq(s"$label changed from ${Text.truncate(valueA)} to ${Text.truncate(valueB)}")
        }
      }
    }
  }

  private def diffString(
    label: String,
    a: String,
    b: String
  ): Seq[String] = {
    diffOptionalString(label, Some(a), Some(b))
  }

  private def diffOptionalStringNonBreaking(
    label: String,
    a: Option[String],
    b: Option[String]
  ): Seq[Difference] = {
    diffOptionalString(label, a, b).map { Difference.NonBreaking(_) }
  }

  private def diffStringNonBreaking(
    label: String,
    a: String,
    b: String
  ): Seq[Difference] = {
    diffString(label, a, b).map { Difference.NonBreaking(_) }
  }

  private def diffStringBreaking(
    label: String,
    a: String,
    b: String
  ): Seq[Difference] = {
    diffString(label, a, b).map { Difference.Breaking(_) }
  }

  private def diffApidoc(): Seq[Difference] = {
    diffStringNonBreaking("apidoc/version", a.apidoc.version, b.apidoc.version)
  }

  private def diffName(): Seq[Difference] = {
    diffStringNonBreaking("name", a.name, b.name)
  }

  private def diffOrganization(): Seq[Difference] = {
    diffStringNonBreaking("organization/key", a.organization.key, b.organization.key)
  }

  private def diffApplication(): Seq[Difference] = {
    diffStringNonBreaking("application/key", a.application.key, b.application.key)
  }

  private def diffNamespace(): Seq[Difference] = {
    diffStringBreaking("namespace", a.namespace, b.namespace)
  }

  private def diffVersion(): Seq[Difference] = {
    diffStringNonBreaking("version", a.version, b.version)
  }

  private def diffBaseUrl(): Seq[Difference] = {
    diffOptionalStringNonBreaking("base_url", a.baseUrl, b.baseUrl)
  }

  private def diffDescription(): Seq[Difference] = {
    diffOptionalStringNonBreaking("description", a.description, b.description)
  }

  private def diffHeaders(): Seq[Difference] = Nil
  private def diffImports(): Seq[Difference] = Nil
  private def diffEnums(): Seq[Difference] = Nil
  private def diffUnions(): Seq[Difference] = Nil
  private def diffModels(): Seq[Difference] = Nil
  private def diffResources(): Seq[Difference] = Nil


}
