package lib.query

import org.scalatest.{FunSpec, ShouldMatchers}

class QuerySpec extends FunSpec with ShouldMatchers {

  describe("Part") {

    it("parses text") {
      Part("foo") should be(Part.Text("foo"))
      Part("foo bar") should be(Part.Text("foo bar"))
    }

    it("parses org key") {
      Part("org:foo") should be(Part.OrgKey("foo"))
    }

    it("parses org key w/ nested colon") {
      Part("org:foo:bar") should be(Part.OrgKey("foo:bar"))
    }

    it("empty string raises error") {
      intercept[AssertionError] {
        Part("  ")
      }.getMessage should be("assertion failed: Value must be trimmed")
    }

  }

}
