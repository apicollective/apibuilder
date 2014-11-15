package core

import org.scalatest.FunSpec

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class UtilSpec extends FunSpec with Matchers {

  lazy val service = TestHelper.parseFile(s"api/api.json").serviceDescription.get
  private lazy val visibilityEnum = service.enums.find(_.name == "visibility").getOrElse {
    sys.error("No visibility enum found")
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
    Util.isValidEnumValue(visibilityEnum, "user") should be(true)
    Util.isValidEnumValue(visibilityEnum, "organization") should be(true)
    Util.isValidEnumValue(visibilityEnum, "foobar") should be(false)
  }

  it("assertValidEnumValue") {
    Util.assertValidEnumValue(visibilityEnum, "user")
    Util.assertValidEnumValue(visibilityEnum, "organization")
    intercept[IllegalArgumentException] {
      Util.assertValidEnumValue(visibilityEnum, "foobar")
    }.getMessage should be("requirement failed: Enum[visibility] does not have a value[foobar]. Valid values are: User, Organization")
  }

}
