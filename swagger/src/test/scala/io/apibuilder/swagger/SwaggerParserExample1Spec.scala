package io.apibuilder.swagger

import helpers.ServiceConfigurationHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json
import io.apibuilder.spec.v0.models.json._

class SwaggerParserExample1Spec extends AnyFunSpec
  with Matchers
  with ServiceConfigurationHelpers
{

  private val example =
    """
      |swagger: '2.0'
      |info:
      |  title: JENNYTEST
      |  version: '1.0'
      |paths:
      |  /test:
      |    put:
      |      description: a test endpoint
      |      consumes:
      |        - application/json
      |      parameters:
      |        - in: body
      |          schema:
      |            $ref: '#/definitions/TestRequest'
      |          required: true
      |          description:  test
      |      responses:
      |        '204':
      |          description: Success Response
      |        default:
      |          description: Error
      |definitions:
      |  TestRequest:
      |    type: object
      |    properties:
      |      message:
      |        type: string
      |
      |""".stripMargin

  it("example") {
    val svc = Parser(
      makeServiceConfiguration()
    ).parseString(example)
    svc.models.map(_.name).sorted shouldBe Seq("TestRequest", "placeholder")
    svc.resources.map(_.`type`) shouldBe Seq("placeholder")
  }
}
