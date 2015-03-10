package core

import com.gilt.apidoc.v0.models.Organization
import lib.{Text, VersionTag}

case class ServiceConfiguration(
  orgKey: String,
  orgNamespace: String,
  version: String
) {
  assert(orgKey.trim == orgKey, s"orgKey[$orgKey] must be trimmed")
  assert(orgNamespace.trim == orgNamespace, s"orgNamespace[$orgNamespace] must be trimmed")
  assert(version.trim == version, s"version[$version] must be trimmed")

  /**
    * Example: apidocSpec => apidoc.spec.v0
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
      orgNamespace = org.namespace.trim,
      orgKey = org.key.trim,
      version = version.trim
    )
  }

}
