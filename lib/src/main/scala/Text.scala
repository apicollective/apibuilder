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
                              Seq("Name can only contain a-z, A-Z, 0-9 and _ characters")
                            }

    val startsWithLetterError = if (startsWithLetter(name)) {
                                  Seq.empty
                                } else if (name.size == 0) {
                                  Seq("Name cannot be blank")
                                } else {
                                  Seq("Name must start with a letter")
                                }

    alphaNumericError ++ startsWithLetterError
  }

  private val AlphaNumericRx = "^[a-zA-Z0-9_.\\.]*$".r

  def isAlphaNumeric(value: String): Boolean = {
    value match {
      case AlphaNumericRx() => true
      case _ => false
    }
  }

  private val StartsWithLetterRx = "^[a-zA-Z].*".r

  def startsWithLetter(value: String): Boolean = {
    val result = value match {
      case StartsWithLetterRx() => true
      case _ => false
    }
    result
  }

  private val Ellipsis = "..."

  /**
   * if value is longer than maxLength characters, it wil be truncated to <= 97
   * characters and an ellipsis added. We try to truncate on a space to avoid
   * breaking a word in pieces.
   */
  def truncate(value: String, maxLength: Int = 100): String = {
    require(maxLength >= 10, "maxLength must be >= 10")

    if (value.length <= maxLength) {
      value
    } else {
      val pieces = value.split(" ")
      var i = pieces.length
      while (i > 0) {
        val sentence = pieces.slice(0, i).mkString(" ")
        if (sentence.length <= (maxLength-Ellipsis.length)) {
          return sentence + Ellipsis
        }
        i -= 1
      }

      val letters = value.split("")
      letters.slice(0, letters.length-4).mkString("") + Ellipsis
    }
  }

  private val Plurals = Map(
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

  private val RemoveUnsafeCharacters = """([^0-9a-zA-Z])""".r
  def safeName(name: String): String = {
    RemoveUnsafeCharacters.replaceAllIn(name, m => "").trim
  }

  def underscoreToInitCap(value: String): String = {
    initCap(splitIntoWords(value))
  }

  def underscoreAndDashToInitCap(value: String): String = {
    initCap(splitIntoWords(value).flatMap(_.split("-")))
  }

  private val WordDelimeterRx = "_|\\-|\\.|:".r

  def splitIntoWords(value: String): Seq[String] = {
    WordDelimeterRx.split(value).map(_.trim).filter(!_.isEmpty)
  }

  def snakeToCamelCase(value: String) = {
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
  private val InitLowerCaseRx = """^([A-Z])""".r
  def initLowerCase(word: String) = {
    InitLowerCaseRx.replaceAllIn(word, m => s"${m.toString.toLowerCase}")
  }

  private val Capitals = """([A-Z])""".r
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
