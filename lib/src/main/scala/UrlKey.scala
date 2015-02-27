package lib

object UrlKey {

  private val MinKeyLength = 4

  // Only want lower case letters and dashes
  private val Regexp1 = """([^0-9a-z\-\_\.])""".r

  // Turn multiple dashes into single dashes
  private val Regexp2 = """(\-+)""".r

  // Turn multiple underscores into single underscore
  private val Regexp3 = """(\_+)""".r

  private val RegexpLeadingSpaces = """^\-+""".r
  private val RegexpTrailingSpaces = """\-+$""".r

  def generate(value: String): String = {
    RegexpTrailingSpaces.replaceAllIn(
      RegexpLeadingSpaces.replaceAllIn(
        Regexp3.replaceAllIn(
          Regexp2.replaceAllIn(
            Regexp1.replaceAllIn(value.toLowerCase.trim, m => "-"),
            m => "-"
          ), m => "_"
        ), m => ""),
      m => ""
    )
  }

  def validate(key: String): Seq[String] = {
    val generated = UrlKey.generate(key)
    if (key.length < MinKeyLength) {
      Seq(s"Key must be at least $MinKeyLength characters")
    } else if (key != generated) {
      Seq(s"Key must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid key would be: $generated")
    } else {
      ReservedKeys.find(prefix => generated.startsWith(prefix)) match {
        case Some(prefix) => Seq(s"Prefix $key is a reserved word and cannot be used for the key")
        case None => Seq.empty
      }
    }
  }

  val ReservedKeys = Seq(
    "_internal_", "api.json", "account", "admin", "api", "api.json", "accept", "asset", "bucket",
    "code", "confirm", "config", "doc", "documentation", "domain", "email", "generator",
    "internal", "login", "logout", "member", "members", "metadatum", "metadata",
    "org", "password", "private", "reject", "service.json", "session", "setting", "scms",
    "source", "subaccount", "subscription", "team", "types", "user", "util", "version", "watch"
  ).map(UrlKey.generate(_))

}
