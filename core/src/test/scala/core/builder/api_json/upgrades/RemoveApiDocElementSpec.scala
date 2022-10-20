package builder.api_json.upgrades

import core.TestHelper
import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class RemoveApiDocElementSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  it("accepts json with apidoc node") {
    TestHelper.serviceValidatorFromApiJson(
      """
        |{
        |  "name": "API Builder",
        |  "apidoc": {
        |    "version": "1.0"
        |  }
        |}
        |""".stripMargin
    ).errors().mkString("") should be("")
  }

}