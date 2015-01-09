package core

import com.gilt.apidoc.models.Organization
import lib.Text

case class ServiceConfiguration(
  orgNamespace: String,
  version: String
) {

  /**
    * Example: apidocSpec => apidoc.spec
    */
  def applicationNamespace(key: String): String = {
    (
      Seq(orgNamespace.trim) ++
      Text.splitIntoWords(Text.camelCaseToUnderscore(key.trim)).map(_.toLowerCase).map(_.trim)
    ).filter(!_.isEmpty).mkString(".")
  }

}


object ServiceConfiguration {

  def apply(
    org: Organization,
    version: String
  ): ServiceConfiguration = {
    ServiceConfiguration(
      orgNamespace = org.namespace,
      version = version
    )
  }

}
