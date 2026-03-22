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
    * Example: apidocSpec => apidoc.spec
    */
  def applicationNamespace(key: String): String = {
    ApplicationNamespaceNonLetterRegexp.replaceAllIn(
      (
        Seq(orgNamespace.trim) ++
        Text.splitIntoWords(Text.camelCaseToUnderscore(key.trim)).map(_.toLowerCase).map(_.trim)
      ).filter(!_.isEmpty).mkString("."),
      m => m.group(1)
    )
  }

}
