package lib

case class ServiceConfiguration(
  orgKey: String,
  orgNamespace: String,
  version: String
) {
  assert(orgKey.trim == orgKey, s"orgKey[$orgKey] must be trimmed")
  assert(orgNamespace.trim == orgNamespace, s"orgNamespace[$orgNamespace] must be trimmed")
  assert(version.trim == version, s"version[$version] must be trimmed")

  private val ApplicationNamespaceNonLetterRegexp = """\.([^a-zA-Z])""".r

  /**
    * Example: apidocSpec => apidoc.spec.v0
    */
  def applicationNamespace(key: String): String = {
    ApplicationNamespaceNonLetterRegexp.replaceAllIn(
      (
        Seq(orgNamespace.trim) ++
        Text.splitIntoWords(Text.camelCaseToUnderscore(key.trim)).map(_.toLowerCase).map(_.trim) ++
        Seq(VersionTag(version).major.map(num => s"v$num").getOrElse(""))
      ).filter(!_.isEmpty).mkString("."),
      m => m.group(1)
    )
  }

}
