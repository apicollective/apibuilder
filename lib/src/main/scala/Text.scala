package lib

import scala.collection.immutable.StringOps

object Text {

  /**
   * We require names to be alpha numeric and to start with a letter
   */
  def isValidName(name: String): Boolean = {
    validateName(name).isEmpty
  }

  def validateName(name: String): Seq[String] = {
    val alphaNumericError = if (isAlphaNumeric(name)) {
                              Seq.empty
                            } else {
                              Seq("Name can only contain a-z, A-Z, 0-9, - and _ characters")
                            }

    val startsWithLetterError = if (startsWithLetter(name)) {
                                  Seq.empty
                                } else if (name.isEmpty) {
                                  Seq("Name cannot be blank")
                                } else {
                                  Seq("Name must start with a letter")
                                }

    alphaNumericError ++ startsWithLetterError
  }

  private[this] val AlphaNumericRx = "^[a-zA-Z0-9-_.\\.]*$".r

  def isAlphaNumeric(value: String): Boolean = {
    value match {
      case AlphaNumericRx() => true
      case _ => false
    }
  }

  private[this] val StartsWithLetterRx = "^[a-zA-Z].*".r

  def startsWithLetter(value: String): Boolean = {
    val result = value match {
      case StartsWithLetterRx() => true
      case _ => false
    }
    result
  }

  private[this] val Ellipsis = "..."

  /**
    * if value is longer than maxLength characters, it wil be truncated
    * to <= (maxLength-Ellipsis.length) characters and an ellipsis
    * added. We try to truncate on a space to avoid breaking a word in
    * pieces.
    *
    * @param value The string value to truncate
    * @param maxLength The max length of the returned string, including the final ellipsis if added. Must be >= 10
    * @param ellipsis If the string is truncated, this value will be appended to the string.
   */
  def truncate(
    value: String,
    maxLength: Int = 80,
    ellipsis: Option[String] = Some(Ellipsis)
  ): String = {
    val suffix = ellipsis.getOrElse("")
    require(maxLength >= suffix.length, "maxLength must be greater than the length of the suffix[${suffix.length}]")

    if (value.length <= maxLength) {
      value
    } else {
      val pieces = value.split(" ")
      var i = pieces.length
      while (i > 0) {
        val sentence = pieces.slice(0, i).mkString(" ").trim
        val target = sentence + suffix
        if (target.length <= maxLength) {
          return target
        }
        i -= 1
      }

      value.split("").slice(0, maxLength - suffix.length).mkString("") + suffix
    }
  }

  private[this] val Plurals = Map(
    "metadatum" -> "metadata",
    "datum" -> "data",
    "person" -> "people",
    "species" -> "species",
    "epoch" -> "epochs"
  )
  private[lib] val KnownPlurals = (Plurals.values ++ Seq(
    "bison",
    "buffalo",
    "deer",
    "duck",
    "fish",
    "moose",
    "pike",
    "plankton",
    "salmon",
    "sheep",
    "squid",
    "swine",
    "trout"
  )).toSet

  /**
   * Handle only base cases for pluralization. User can specify own plural
   * form via api.json if needed.
   */
  def pluralize(value: String): String = {
    if (KnownPlurals.contains(value.toLowerCase)) {
      value

    } else if (Plurals.contains(value)) {
      Plurals(value)

    } else if (value.endsWith("es") || value.endsWith("ts") || value.endsWith("data")) {
      value

    } else {
      org.atteo.evo.inflector.English.plural(value)
    }
  }

  private[this] val RemoveUnsafeCharacters = """([^0-9a-zA-Z\-\_])""".r
  def safeName(name: String): String = {
    RemoveUnsafeCharacters.replaceAllIn(name, _ => "").replaceAll("\\.", "_").replaceAll("\\_+", "_").trim
  }

  def underscoreToInitCap(value: String): String = {
    initCap(splitIntoWords(value))
  }

  def underscoreAndDashToInitCap(value: String): String = {
    initCap(splitIntoWords(value).flatMap(_.split("-")))
  }

  private[this] val WordDelimiterRx = "_|\\-|\\.|:|/".r

  def splitIntoWords(value: String): Seq[String] = {
    WordDelimiterRx.split(lib.Text.camelCaseToUnderscore(value)).map(_.trim).filter(!_.isEmpty)
  }

  def snakeToCamelCase(value: String): String = {
    splitIntoWords(value).toList match {
      case Nil => ""
      case part :: rest => part + initCap(rest)
    }
  }

  def initCap(word: String): String = {
    word.capitalize
  }

  def initCap(parts: Seq[String]): String = {
    parts.map(s => initCap(s)).mkString("")
  }

  /**
    * Returns the word with first character in lower case
    */
  private[this] val InitLowerCaseRx = """^([A-Z])""".r
  def initLowerCase(word: String): String = {
    InitLowerCaseRx.replaceAllIn(word, m => s"${m.toString.toLowerCase}")
  }

  private[this] val Capitals = """([A-Z])""".r
  def camelCaseToUnderscore(phrase: String): String = {
    if (phrase == phrase.toUpperCase) {
      phrase.toLowerCase
    } else {
      val word = Capitals.replaceAllIn(phrase, m => s"_${m}").trim
      if (word.startsWith("_")) {
        word.slice(1, word.length)
      } else {
        word
      }
    }
  }

  implicit class Indentable(s: String) {
    def indent: String = indent(2)
    def indent(width: Int): String = {
      s.split("\n").map { value =>
        if (value.trim == "") {
          ""
        } else {
          (" " * width) + value
        }
      }.mkString("\n")
    }
  }

}
