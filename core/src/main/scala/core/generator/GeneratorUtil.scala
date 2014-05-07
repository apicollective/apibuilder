package core.generator

import core.Util

private[generator] object GeneratorUtil {

  private val JsonDocumentMethods = Seq("POST", "PUT", "PATCH")

  def isJsonDocumentMethod(verb: String): Boolean = {
    JsonDocumentMethods.contains(verb)
  }

  // TODO: Remove wrapper
  def namedParametersInPath(path: String) = Util.namedParametersInPath(path)

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
