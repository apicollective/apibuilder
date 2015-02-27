package lib

import org.scalatest.{FunSpec, Matchers}

class UrlKeySpec extends FunSpec with Matchers {

  describe("generate") {

    it("good urls alone") {
      UrlKey.generate("foo") should be("foo")
      UrlKey.generate("foo-bar") should be("foo-bar")
    }

    it("numbers") {
      UrlKey.generate("foo123") should be("foo123")
    }

    it("lower case") {
      UrlKey.generate("FOO-BAR") should be("foo-bar")
    }

    it("trim") {
      UrlKey.generate("  foo-bar  ") should be("foo-bar")
    }

    it("leading garbage") {
      UrlKey.generate("!foo") should be("foo")
    }

    it("trailing garbage") {
      UrlKey.generate("foo!") should be("foo")
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
