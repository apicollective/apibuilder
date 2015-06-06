package lib

import org.scalatest.{FunSpec, Matchers}

class UrlKeySpec extends FunSpec with Matchers {

  describe("generate") {

    it("does not generate reserved keys") {
      val key = UrlKey.generate("members")
      UrlKey.validate(key) should be(Nil)
    }

    it("allows apidoc keys") {
      Seq("apidoc-spec", "apidoc-generator", "apidoc-api").foreach { key =>
        UrlKey.generate(key) should be(key)
        UrlKey.validate(key) should be(Nil)
      }
    }

    it("good urls alone") {
      UrlKey.generate("foos") should be("foos")
      UrlKey.generate("foos-bar") should be("foos-bar")
    }

    it("numbers") {
      UrlKey.generate("foos123") should be("foos123")
    }

    it("lower case") {
      UrlKey.generate("FOOS-BAR") should be("foos-bar")
    }

    it("trim") {
      UrlKey.generate("  foos-bar  ") should be("foos-bar")
    }

    it("leading garbage") {
      UrlKey.generate("!foos") should be("foos")
    }

    it("trailing garbage") {
      UrlKey.generate("foos!") should be("foos")
    }

    it("allows underscores") {
      UrlKey.generate("ning_1_8_client") should be("ning_1_8_client")
    }

  }

  describe("validate") {

    it("short") {
      UrlKey.validate("bad") should be(Seq("Key must be at least 4 characters"))
    }

    it("doesn't match generated") {
      UrlKey.validate("VALID") should be(Seq("Key must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid key would be: valid"))
      UrlKey.validate("bad nickname") should be(Seq("Key must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid key would be: bad-nickname"))
    }

    it("reserved") {
      UrlKey.validate("api.json") should be(Seq("Prefix api.json is a reserved word and cannot be used for the key"))
    }

  }

}
