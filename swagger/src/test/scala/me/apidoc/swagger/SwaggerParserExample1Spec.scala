package me.apidoc.swagger

import helpers.ServiceConfigurationHelpers
import org.scalatest.{FunSpec, Matchers}

class SwaggerParserExample1Spec extends FunSpec
  with Matchers
  with ServiceConfigurationHelpers
{

  private[this] val example =
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
    val requests = Parser(
      makeServiceConfiguration()
    ).parseString(example)
    requests.models.map(_.name) shouldBe Seq("TestRequest")
    requests.resources.map(_.`type`) shouldBe Seq("TestRequest")
  }
}
