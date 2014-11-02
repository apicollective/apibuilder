package core

import org.scalatest.FunSpec

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class UtilSpec extends FunSpec with Matchers {

  lazy val service = TestHelper.parseFile(s"reference-api/api.json").serviceDescription.get

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
    lazy val ageGroupEnum = ssd.enums.find(_.name == "AgeGroup").getOrElse {
      sys.error("No age group enum found")
    }
    ageGroupEnum.isValidEnumValue("Youth") should be(true)
    ageGroupEnum.isValidEnumValue("Adult") should be(true)
    ageGroupEnum.isValidEnumValue("foobar") should be(false)
  }

  it("assertValidEnumValue") {
    lazy val ageGroupEnum = ssd.enums.find(_.name == "AgeGroup").getOrElse {
      sys.error("No age group enum found")
    }
    ageGroupEnum.assertValidEnumValue("Youth") should be(true)
    ageGroupEnum.assertValidEnumValue("Adult") should be(true)
    intercept[RuntimeError] {
      ageGroupEnum.assertValidEnumValue("foobar")
    }.toString should be("TODO")
  }

}
