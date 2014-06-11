package core.generator

import core.{ Text, Util }

private[generator] object GeneratorUtil {

  /**
   * Turns a URL path to a camelcased method name.
   */
  def urlToMethodName(pluralModelName: String, httpMethod: String, url: String): String = {
    val modelUrlPath = Text.camelCaseToUnderscore(pluralModelName).toLowerCase
    val pieces = url.split("/").filter { !_.isEmpty }.filter { _.toLowerCase != modelUrlPath }

    val named = pieces.filter { _.startsWith(":") }.map { name => Text.initCap(Text.safeName(name.slice(1, name.length))) }
    val notNamed = pieces.filter { !_.startsWith(":") }.map( name => Text.initCap(Text.safeName(name)) )

    if (named.isEmpty && notNamed.isEmpty) {
      httpMethod.toLowerCase

    } else if (named.isEmpty) {
      httpMethod.toLowerCase + notNamed.mkString("And")

    } else if (notNamed.isEmpty) {
      httpMethod.toLowerCase + "By" + named.mkString("And")

    } else {
      httpMethod.toLowerCase + notNamed.mkString("And") + "By" + named.mkString("And")

    }
  }

  def isJsonDocumentMethod(verb: String): Boolean = {
    verb != "GET" && verb != "DELETE"
  }

  /**
   * Format into a multi-line comment w/ a set number of spaces for
   * leading indentation
   */
  def formatComment(comment: String, numberSpaces: Int = 0): String = {
    val maxLineLength = 80 - 2 - numberSpaces
    val sb = new StringBuilder()
    var currentWord = new StringBuilder()
    comment.split(" ").foreach { word =>
      if (word.length + currentWord.length >= maxLineLength) {
        if (!currentWord.isEmpty) {
          if (!sb.isEmpty) {
            sb.append("\n")
          }
          sb.append((" " * numberSpaces)).append("#").append(currentWord.toString)
        }
        currentWord = new StringBuilder()
      }
      currentWord.append(" ").append(word)
    }
    if (!currentWord.isEmpty) {
      if (!sb.isEmpty) {
        sb.append("\n")
      }
      sb.append((" " * numberSpaces)).append("#").append(currentWord.toString)
    }
    sb.toString
  }

}
