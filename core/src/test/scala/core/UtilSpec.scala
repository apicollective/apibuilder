package core

import org.scalatest.FunSpec

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class UtilSpec extends FunSpec with Matchers {

  lazy val service = TestHelper.parseFile(s"reference-api/api.json").serviceDescription.get
  private lazy val ageGroupEnum = service.enums.find(_.name == "age_group").getOrElse {
    sys.error("No age_group enum found")
  }

  it("isJsonDocumentMethod") {
    Util.isJsonDocumentMethod("GET") should be(false)
    Util.isJsonDocumentMethod("get") should be(false)
    Util.isJsonDocumentMethod("DELETE") should be(false)
    Util.isJsonDocumentMethod("delete") should be(false)
    Util.isJsonDocumentMethod("POST") should be(true)
    Util.isJsonDocumentMethod("post") should be(true)
    Util.isJsonDocumentMethod("PUT") should be(true)
    Util.isJsonDocumentMethod("put") should be(true)
    Util.isJsonDocumentMethod("PATCH") should be(true)
    Util.isJsonDocumentMethod("patch") should be(true)
  }

  it("namedParametersInPath") {
    Util.namedParametersInPath("/users") should be(Seq.empty)
    Util.namedParametersInPath("/users/:guid") should be(Seq("guid"))
    Util.namedParametersInPath("/:org/docs/:version") should be(Seq("org", "version"))
    Util.namedParametersInPath("/:org/:service/:version") should be(Seq("org", "service", "version"))
  }

  it("isValidEnumValue") {
    Util.isValidEnumValue(ageGroupEnum, "Youth") should be(true)
    Util.isValidEnumValue(ageGroupEnum, "Adult") should be(true)
    Util.isValidEnumValue(ageGroupEnum, "foobar") should be(false)
  }

  it("assertValidEnumValue") {
    Util.assertValidEnumValue(ageGroupEnum, "Youth")
    Util.assertValidEnumValue(ageGroupEnum, "Adult")
    intercept[IllegalArgumentException] {
      Util.assertValidEnumValue(ageGroupEnum, "foobar")
    }.getMessage should be("requirement failed: Enum[age_group] does not have a value[foobar]. Valid values are: Youth, Adult")
  }

}
