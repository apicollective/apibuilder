package core.generator

import core.{ Text, Util }

private[generator] object GeneratorUtil {

  /**
   * Turns a URL path to a camelcased method name.
   */
  def urlToMethodName(resourcePath: String, httpMethod: String, url: String): String = {
    val pieces = (if (resourcePath.startsWith("/:")) {
      url
    } else {
      url.replaceAll("^" + resourcePath, "")
    }).split("/").filter { !_.isEmpty }

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

  def bodyAllowed(verb: String): Boolean = {
    verb != "GET" && verb != "DELETE"
  }

  /**
   * Splits a string into lines with a given max length
   * leading indentation.
   */
  def splitIntoLines(comment: String, maxLength: Int = 80): Seq[String] = {
    val sb = new scala.collection.mutable.ListBuffer[String]()
    var currentWord = new StringBuilder()
    comment.split(" ").map(_.trim).foreach { word =>
      if (word.length + currentWord.length >= maxLength) {
        if (!currentWord.isEmpty) {
          sb.append(currentWord.toString)
        }
        currentWord = new StringBuilder()
      } else if (!currentWord.isEmpty) {
        currentWord.append(" ")
      }
      currentWord.append(word)
    }
    if (!currentWord.isEmpty) {
      sb.append(currentWord.toString)
    }
    sb.toList
  }

  /**
   * Format into a multi-line comment w/ a set number of spaces for
   * leading indentation
   */
  def formatComment(comment: String, numberSpaces: Int = 0): String = {
    val spacer = " " * numberSpaces
    splitIntoLines(comment, 80 - 2 - numberSpaces).map { line =>
      s"$spacer# $line"
    }.mkString("\n")
  }

}
