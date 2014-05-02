package core.generator

private[generator] object GeneratorUtil {

  private val JsonDocumentMethods = Seq("POST", "PUT", "PATCH")

  def isJsonDocumentMethod(verb: String): Boolean = {
    JsonDocumentMethods.contains(verb)
  }

  // Select out named parameters in the path. E.g. /:org/:service/foo would return [org, service]
  def namedParametersInPath(path: String): Seq[String] = {
    path.split("/").flatMap { name =>
      if (name.startsWith(":")) {
        Some(name.slice(1, name.length))
      } else {
        None
      }
    }
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
