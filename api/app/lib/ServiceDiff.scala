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

  private def diffString(
    label: String,
    a: String,
    b: String
  ): Seq[String] = {
    a match {
      case `b` => Nil
      case other => Seq(s"$label changed from $a to $b")
    }
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

  private def diffVersion(): Seq[Difference] = Nil
  private def diffBaseUrl(): Seq[Difference] = Nil
  private def diffDescription(): Seq[Difference] = Nil
  private def diffHeaders(): Seq[Difference] = Nil
  private def diffImports(): Seq[Difference] = Nil
  private def diffEnums(): Seq[Difference] = Nil
  private def diffUnions(): Seq[Difference] = Nil
  private def diffModels(): Seq[Difference] = Nil
  private def diffResources(): Seq[Difference] = Nil


}
