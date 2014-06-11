package core.generator

import org.scalatest.{ ShouldMatchers, FunSpec }

class ScalaUtilSpec extends FunSpec with ShouldMatchers {

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
