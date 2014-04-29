package core.generator.scala

import core.{ Datatype, Field, Operation, ServiceDescription, Resource, Text, UrlKey }

object ClientGenerator {
  def apply(service: ServiceDescription) = new ClientGenerator(service).generate
}

class ClientGenerator(service: ServiceDescription) {
  def generate: Map[String, String] = {
    service.resources.map { resource =>
      val modelClassName = Text.singular(Text.underscoreToInitCap(resource.name))
      val clientClassName = modelClassName + "Client"
      clientClassName -> generateOperations(clientClassName, resource)
    }.toMap
  }

  def generateOperations(clientClassName: String, resource: Resource): String = {
s"""
class $clientClassName extends Client {
  override val resource = "/${UrlKey.generate(resource.name)}"
}
"""
  }
}
