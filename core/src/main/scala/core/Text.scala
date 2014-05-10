package core

import scala.collection.immutable.StringOps

object Text {

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

  private val Plurals = Map("datum" -> "data",
                            "person" -> "people")
  private val KnownPlurals = Plurals.values.toSet

  /**
   * Handle only base cases for pluralization. User can specify own plural
   * form via api.json if needed.
   */
  def pluralize(value: String): String = {
    if (KnownPlurals.contains(value.toLowerCase)) {
      value

    } else if (Plurals.contains(value)) {
      Plurals(value)

    } else if (value.endsWith("ss")) {
      value + "es"

    } else if (value.endsWith("y")) {
      val letters = value.split("")
      letters.slice(0, letters.size - 1).mkString("") + "ies"

    } else {
      value + "s"
    }
  }

  private val RemoveUnsafeCharacters = """([^0-9a-zA-Z])""".r
  def safeName(name: String): String = {
    RemoveUnsafeCharacters.replaceAllIn(name, m => "").trim
  }

  private val MakeSingular = """s$""".r
  def singular(name: String) = {
    MakeSingular.replaceAllIn(name, m => "").trim
  }

  def underscoreToInitCap(value: String): String = {
    initCap(value.split("_"))
  }

  def dashToInitCap(value: String): String = {
    initCap(value.split("-"))
  }

  def snakeToCamelCase(value: String) = {
    value.split("_").toList match {
      case Nil => ""
      case part :: rest => part + initCap(rest)
    }
  }

  def initCap(word: String): String = {
    new StringOps(Text.safeName(word).toLowerCase).capitalize
  }

  def initCap(parts: Seq[String]): String = {
    parts.map(s => initCap(s)).mkString("")
  }

  private val Capitals = """([A-Z])""".r
  def camelCaseToUnderscore(phrase: String): String = {
    val word = Capitals.replaceAllIn(phrase, m => s"_${m}").trim
    if (word.startsWith("_")) {
      word.slice(1, word.length)
    } else {
      word
    }
  }

  implicit class Indentable(s: String) {
    def indent: String = indent(2)
    def indent(width: Int): String = {
      s.split("\n").map((" " * width) + _).mkString("\n")
    }
  }

}
