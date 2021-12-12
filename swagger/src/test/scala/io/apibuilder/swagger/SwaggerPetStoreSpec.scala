package io.apibuilder.swagger

import _root_.helpers.ServiceConfigurationHelpers
import io.apibuilder.swagger.helpers.FileHelpers
import org.scalatest.{FunSpec, Matchers}

class SwaggerPetStoreSpec extends FunSpec
  with Matchers
  with FileHelpers
  with ServiceConfigurationHelpers
{

  private[this] lazy val svc = Parser(
    makeServiceConfiguration()
  ).parseString(readResource("petstore-20211212.yml"))

  it("example") {
    println(s"svc has models: ${svc.models.map(_.name)}")
  }
}
