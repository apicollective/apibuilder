package core

object Util {

  val MethodsNotAcceptingBodies = Seq("GET", "DELETE")

  def isJsonDocumentMethod(verb: String): Boolean = {
    !MethodsNotAcceptingBodies.contains(verb.toUpperCase)
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
