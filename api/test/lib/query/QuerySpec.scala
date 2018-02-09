package lib.query

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

class QuerySpec extends PlaySpec with OneAppPerSuite {

  def validateQuery(q: String, words: Seq[String], orgKeys: Seq[String]) {
    QueryParser(q) match {
      case None => fail(s"Query[$q] failed to parse")
      case Some(query) => {
        query.words must be(words)
        query.orgKeys must be(orgKeys)
      }
    }
  }

  it("QueryParser") {
    validateQuery("foo", Seq("foo"), Nil)
    validateQuery("FOO", Seq("FOO"), Nil)
    validateQuery("  foo   ", Seq("foo"), Nil)
    validateQuery("foo bar", Seq("foo", "bar"), Nil)
    validateQuery("org:gilt", Nil, Seq("gilt"))
    validateQuery("org:gilt foo bar", Seq("foo", "bar"), Seq("gilt"))
    validateQuery("baz org:gilt foo bar", Seq("baz", "foo", "bar"), Seq("gilt"))
    validateQuery("baz org:gilt org:bryzek foo bar", Seq("baz", "foo", "bar"), Seq("gilt", "bryzek"))
    QueryParser("") must be(None)
    QueryParser("   ") must be(None)
  }

  describe("Part") {

    it("parses text") {
      Part("foo") must be(Seq(Part.Text("foo")))
      Part("foo bar") must be(Seq(Part.Text("foo"), Part.Text("bar")))
    }

    it("parses org key") {
      Part("org:foo") must be(Seq(Part.OrgKey("foo")))
    }

    it("case insensitive on org label") {
      Part("ORG:FOO") must be(Seq(Part.OrgKey("FOO")))
    }

    it("parses org key w/ nested colon") {
      Part("org:foo:bar") must be(Seq(Part.OrgKey("foo:bar")))
    }

    it("leaves unknown keys as text") {
      Part("foo:bar") must be(Seq(Part.Text("foo:bar")))
    }

    it("empty string raises error") {
      intercept[AssertionError] {
        Part("  ")
      }.getMessage must be("assertion failed: Value must be trimmed")
    }

  }

}
