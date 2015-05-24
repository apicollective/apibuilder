package lib.query

import org.scalatest.{FunSpec, ShouldMatchers}

class QuerySpec extends FunSpec with ShouldMatchers {

  def validateQuery(q: String, words: Seq[String], orgKeys: Seq[String]) {
    QueryParser(q) match {
      case None => fail(s"Query[$q] failed to parse")
      case Some(query) => {
        query.words should be(words)
        query.orgKeys should be(orgKeys)
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
    QueryParser("") should be(None)
    QueryParser("   ") should be(None)
  }

  describe("Part") {

    it("parses text") {
      Part("foo") should be(Seq(Part.Text("foo")))
      Part("foo bar") should be(Seq(Part.Text("foo"), Part.Text("bar")))
    }

    it("parses org key") {
      Part("org:foo") should be(Seq(Part.OrgKey("foo")))
    }

    it("case insensitive on org label") {
      Part("ORG:FOO") should be(Seq(Part.OrgKey("FOO")))
    }

    it("parses org key w/ nested colon") {
      Part("org:foo:bar") should be(Seq(Part.OrgKey("foo:bar")))
    }

    it("leaves unknown keys as text") {
      Part("foo:bar") should be(Seq(Part.Text("foo:bar")))
    }

    it("empty string raises error") {
      intercept[AssertionError] {
        Part("  ")
      }.getMessage should be("assertion failed: Value must be trimmed")
    }

  }

}
