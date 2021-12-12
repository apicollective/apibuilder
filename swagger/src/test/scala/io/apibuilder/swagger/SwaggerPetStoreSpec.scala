package io.apibuilder.swagger

import helpers.ServiceConfigurationHelpers
import org.scalatest.{FunSpec, Matchers}

class SwaggerPetStoreSpec extends FunSpec
  with Matchers
  with ServiceConfigurationHelpers
{

  private[this] val example =
    """

      |""".stripMargin

  it("example") {
    val svc = Parser(
      makeServiceConfiguration()
    ).parseString(example)
    svc.models.map(_.name).sorted shouldBe Seq("TestRequest", "placeholder")
    svc.resources.map(_.`type`) shouldBe Seq("placeholder")
  }
}
