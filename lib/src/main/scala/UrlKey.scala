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
    val key = format(value)
    validate(key) match {
      case Nil => key
      case errors => {
        // The provided value was insufficent. Iterate. Note that we
        // key the prefix key- here to ensure that it itself is not
        // formatted away. It is also long enough to satisy min length
        // requirements for the key.
        val key2 = format("key-" + value)
        validate(key2) match {
          case Nil => key2
          case errors => sys.error(s"Could not generate a valid key from value[$value]")
        }
      }
    }
  }

  private def format(value: String): String = {
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
    val generated = UrlKey.format(key)
    if (key.length < MinKeyLength) {
      Seq(s"Key must be at least $MinKeyLength characters")
    } else if (key != generated) {
      Seq(s"Key must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid key would be: $generated")
    } else {
      ReservedKeys.find(_ == generated) match {
        case Some(value) => Seq(s"$key is a reserved word and cannot be used for the key")
        case None => Nil
      }
    }
  }

  val ReservedKeys = Seq(
    "_internal_", "api", "api.json", "account", "admin", "accept", "asset", "bucket",
    "change", "code", "confirm", "config", "doc", "docs", "documentation", "domains",
    "generators", "history", "internal", "login", "logout", "members", "metadatum", "metadata",
    "org", "orgs", "organizations", "password", "private", "reject", "service.json", "sessions",
    "settings", "scms", "search", "source", "subaccounts", "subscriptions", "teams", "types", "users",
    "util", "version", "version", "watch", "watches"
  ).map(UrlKey.format(_))

}
