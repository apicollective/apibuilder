package core

import lib.VersionTag
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ApiBuilderVersionSpec extends AnyFunSpec with Matchers {

  describe("json documenent with no apidoc node") {

    val json = """
      { "name": "API Builder" }
    """

    it("defaults to latest version") {
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors().mkString("") should be("")
      validator.service().apidoc.version should be(io.apibuilder.spec.v0.Constants.Version)
    }

  }
}
