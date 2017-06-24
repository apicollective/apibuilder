package core

import lib.VersionTag
import org.scalatest.{FunSpec, Matchers}

class ApidocVersionSpec extends FunSpec with Matchers {

  describe("json documenent with no apidoc node") {

    val json = """
      { "name": "Api Doc" }
    """

    it("defaults to latest version") {
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be("")
      validator.service.apidoc.version should be(io.apibuilder.apidoc.spec.v0.Constants.Version)
    }

  }

  describe("json documenent with an apidoc node") {

    val baseJson = """
    {
      "name": "Api Doc",
      "apidoc": {
        "version": "%s"
      }
    }
  """

    it("current version is ok") {
      val json = baseJson.format(io.apibuilder.apidoc.spec.v0.Constants.Version)
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be("")
    }

    it("rejects invalid version") {
      val json = baseJson.format("!")
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be(s"Invalid apidoc version[!]. Latest version of apidoc specification is ${io.apibuilder.apidoc.spec.v0.Constants.Version}")
    }

  }
}
