package lib

object Methods {

  val MethodsNotAcceptingBodies = Seq("GET", "DELETE")

  def isJsonDocumentMethod(verb: String): Boolean = {
    !MethodsNotAcceptingBodies.contains(verb.toUpperCase)
  }

}
