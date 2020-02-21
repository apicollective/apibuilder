package lib.query

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class QuerySpec extends PlaySpec with GuiceOneAppPerSuite {

  def validateQuery(q: String, words: Seq[String], orgKeys: Seq[String]): Unit = {
    QueryParser(q) match {
      case None => fail(s"Query[$q] failed to parse")
      case Some(query) => {
        query.words must be(words)
        query.orgKeys must be(orgKeys)
      }
    }
  }

  "QueryParser" in {
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

  "Part" must {

    "parses text" in {
      Part("foo") must be(Seq(Part.Text("foo")))
      Part("foo bar") must be(Seq(Part.Text("foo"), Part.Text("bar")))
    }

    "parses org key" in {
      Part("org:foo") must be(Seq(Part.OrgKey("foo")))
    }

    "case insensitive on org label" in {
      Part("ORG:FOO") must be(Seq(Part.OrgKey("FOO")))
    }

    "parses org key w/ nested colon" in {
      Part("org:foo:bar") must be(Seq(Part.OrgKey("foo:bar")))
    }

    "leaves unknown keys as text" in {
      Part("foo:bar") must be(Seq(Part.Text("foo:bar")))
    }

    "empty string raises error" in {
      intercept[AssertionError] {
        Part("  ")
      }.getMessage must be("assertion failed: Value must be trimmed")
    }

  }

}
