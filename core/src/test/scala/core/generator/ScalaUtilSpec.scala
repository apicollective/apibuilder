package core.generator

import org.scalatest.{ ShouldMatchers, FunSpec }

class ScalaUtilSpec extends FunSpec with ShouldMatchers {

  it("toClassName") {
    ScalaUtil.toClassName("UnableToFulfill") should be("UnableToFulfill")
    ScalaUtil.toClassName("UNABLE_TO_FULFILL") should be("UnableToFulfill")
  }

  it("toVariable") {
    ScalaUtil.toVariable("Foo") should be("foo")
    ScalaUtil.toVariable("FooBar") should be("fooBar")
    ScalaUtil.toVariable("Foo_Bar") should be("fooBar")
    ScalaUtil.toVariable("foo_bar") should be("fooBar")
  }

  it("quoteNameIfKeyword") {
    ScalaUtil.quoteNameIfKeyword("foo") should be("foo")
    ScalaUtil.quoteNameIfKeyword("val") should be("`val`")
  }

  describe("textToComment") {
    it("For single line comment") {
      ScalaUtil.textToComment("users is a") should be("""/**
 * users is a
 */
""".trim)
    }

    it("is a no op for whitespace") {
      ScalaUtil.textToComment("") should be("")
      ScalaUtil.textToComment("  ") should be("")
    }

    it("Breaks up a long comment") {
      val source = "Search all users. Results are always paginated. You must specify at least 1 parameter"
      val target = """/**
 * Search all users. Results are always paginated. You must specify at least 1
 * parameter
 */
""".trim

      ScalaUtil.textToComment(source) should be(target)
    }

  }

}
