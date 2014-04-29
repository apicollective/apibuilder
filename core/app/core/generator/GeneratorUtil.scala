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

}
