package io.apibuilder.swagger

import io.apibuilder.swagger.helpers.FileHelpers
import org.scalatest.{FunSpec, Matchers}

class SwaggerPetStoreSpec extends FunSpec
  with Matchers
  with FileHelpers
{

  private[this] val example = readResource("petstore-20211212.yml")

  it("example") {
    val svc = Parser(
      makeServiceConfiguration()
    ).parseString(example)
    svc.models.map(_.name).sorted shouldBe Seq("TestRequest", "placeholder")
    svc.resources.map(_.`type`) shouldBe Seq("placeholder")
  }
}
