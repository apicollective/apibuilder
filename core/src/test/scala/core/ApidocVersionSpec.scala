package core

import lib.VersionTag
import org.scalatest.{FunSpec, Matchers}

class ApidocVersionSpec extends FunSpec with Matchers {

  describe("json documenent with no apidoc node") {

    val json = """
      { "name": "Api Doc" }
    """

    it("apidoc version is migrated automatically to 0.9.4") {
      val validator = TestHelper.serviceValidatorFromApiJson(json, migration = VersionMigration(internal = true))
      validator.errors.mkString("") should be("")
      validator.service.apidoc.version should be("0.9.4")
    }

    it("validates that apidoc node is required") {
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be(s"Missing apidoc/version. Latest version of apidoc specification is ${com.gilt.apidoc.spec.v0.Constants.Version}")
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
      val json = baseJson.format(com.gilt.apidoc.spec.v0.Constants.Version)
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be("")
    }

    it("validates version <= latest version") {
      val current = com.gilt.apidoc.spec.v0.Constants.Version
      val next = VersionTag(current).nextMicro.get
      val json = baseJson.format(next)
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be(s"Apidoc version[$next] cannot be greater than the current latest version of the apidoc specification[$current]")
    }

    it("rejects invalid version") {
      val json = baseJson.format("!")
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be(s"Invalid apidoc version[!]. Latest version of apidoc specification is ${com.gilt.apidoc.spec.v0.Constants.Version}")
    }

    it("rejects empty version") {
      val json = baseJson.format("")
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be(s"Missing apidoc/version. Latest version of apidoc specification is ${com.gilt.apidoc.spec.v0.Constants.Version}")
    }

  }
}
