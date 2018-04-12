package lib

object Methods {

  val MethodsNotAcceptingBodies: Set[String] = Set("GET")

  def supportsBody(verb: String): Boolean = {
    !MethodsNotAcceptingBodies.contains(verb.toUpperCase)
  }

}
