package lib

object UrlKey {

  // Only want lower case letters and dashes
  private val Regexp1 = """([^0-9a-z\-\.])""".r

  // Now turn multiple dashes into single dashes
  private val Regexp2 = """(\-+)""".r

  def generate(value: String): String = {
    val a = Regexp1.replaceAllIn(value.toLowerCase.trim, m => "-")
    Regexp2.replaceAllIn(a, m => "-")
  }

}
