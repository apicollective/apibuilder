package io.apibuilder.swagger.v2

import _root_.helpers.{ServiceConfigurationHelpers, TestHelpers}
import io.apibuilder.swagger.helpers.FileHelpers
import org.scalatest.{FunSpec, Matchers}

// Test examples specs from https://github.com/OAI/OpenAPI-Specification/tree/main/examples/v3.0
class OpenApi3PetStoreExampleSpec extends FunSpec
  with Matchers
  with FileHelpers
  with ServiceConfigurationHelpers
  with TestHelpers
{

  private[this] lazy val svc = expectValid {
    V2Parser(
      makeServiceConfiguration()
    ).parse(readResource("petstore-20211212.yml"))
  }

  it("example") {
    println(s"svc has models: ${svc.models.map(_.name)}")
  }
}
