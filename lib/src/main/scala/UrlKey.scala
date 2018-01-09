package lib

object UrlKey {

  private[this] val MinKeyLength = 3

  // Only want lower case letters and dashes
  private[this] val Regexp1 = """([^0-9a-z\-\_\.])""".r

  // Turn multiple dashes into single dashes
  private[this] val Regexp2 = """(\-+)""".r

  // Turn multiple underscores into single underscore
  private[this] val Regexp3 = """(\_+)""".r

  private[this] val RegexpLeadingSpaces = """^\-+""".r
  private[this] val RegexpTrailingSpaces = """\-+$""".r

  def generate(value: String): String = {
    generate(format(value), 0)
  }

  @scala.annotation.tailrec
  private[this] def generate(value: String, suffix: Int): String = {
    val key = if (suffix <= 0) { value } else { s"$value-1" }
    validate(key) match {
      case Nil => key
      case errors => generate(key, suffix + 1)
    }
  }

  def format(value: String): String = {
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

  def validate(key: String, label: String = "Key"): Seq[String] = {
    val generated = UrlKey.format(key)
    if (key.length < MinKeyLength) {
      Seq(s"$label must be at least $MinKeyLength characters")
    } else if (key != generated) {
      Seq(s"$label must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid ${label.toLowerCase} would be: $generated")
    } else {
      Nil
    }
  }

}
