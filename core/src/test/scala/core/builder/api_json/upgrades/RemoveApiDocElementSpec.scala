package builder.api_json.upgrades

import core.TestHelper
import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.annotation.nowarn


@nowarn("msg=value apidoc in class Service is deprecated")
class RemoveApiDocElementSpec extends AnyFunSpec with Matchers with ApiJsonHelpers{

  it("accepts json with apidoc node") {
    setupValidApiJson(
      """
        |{
        |  "name": "API Builder",
        |  "apidoc": {
        |    "version": "1.0"
        |  }
        |}
        |""".stripMargin
    ).apidoc.get.version should be("1.0")
  }

}