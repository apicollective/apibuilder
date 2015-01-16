package core

import com.gilt.apidoc.v0.models.Organization
import lib.{Text, VersionTag}

case class ServiceConfiguration(
  orgKey: String,
  orgNamespace: String,
  version: String
) {

  /**
    * Example: apidocSpec => apidoc.spec
    */
  def applicationNamespace(key: String): String = {
    (
      Seq(orgNamespace.trim) ++
      Text.splitIntoWords(Text.camelCaseToUnderscore(key.trim)).map(_.toLowerCase).map(_.trim) ++
      Seq(VersionTag(version).major.map(num => s"v$num").getOrElse(""))
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
      orgKey = org.key,
      version = version
    )
  }

}
