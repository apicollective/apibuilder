package generator

import org.scalatest.{ ShouldMatchers, FunSpec }

class ScalaUtilSpec extends FunSpec with ShouldMatchers {

  it("toClassName") {
    ScalaUtil.toClassName("UnableToFulfill") should be("UnableToFulfill")
    ScalaUtil.toClassName("UNABLE_TO_FULFILL") should be("UnableToFulfill")

    ScalaUtil.toClassName("error") should be("Error")
    ScalaUtil.toClassName("error", true) should be("Errors")

    ScalaUtil.toClassName("error_message") should be("ErrorMessage")
    ScalaUtil.toClassName("error_message", true) should be("ErrorMessages")

    ScalaUtil.toClassName("incidents_create") should be("IncidentsCreate")
    ScalaUtil.toClassName("incidents-create") should be("IncidentsCreate")
    ScalaUtil.toClassName("incidents.create") should be("IncidentsCreate")
    ScalaUtil.toClassName("incident.create") should be("IncidentCreate")
    ScalaUtil.toClassName("incidents:create") should be("IncidentsCreate")
    ScalaUtil.toClassName("incident:create") should be("IncidentCreate")
  }

  it("toVariable") {
    ScalaUtil.toVariable("Foo") should be("foo")
    ScalaUtil.toVariable("FooBar") should be("fooBar")
    ScalaUtil.toVariable("Foo_Bar") should be("fooBar")
    ScalaUtil.toVariable("foo_bar") should be("fooBar")

    ScalaUtil.toVariable("error") should be("error")
    ScalaUtil.toVariable("error", true) should be("errors")
    ScalaUtil.toVariable("error_message") should be("errorMessage")
    ScalaUtil.toVariable("error_messages", true) should be("errorMessages")
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
