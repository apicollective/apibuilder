package core

import org.scalatest.FunSpec

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class UtilSpec extends FunSpec with Matchers {

  lazy val service = TestHelper.parseFile(s"api/api.json").serviceDescription.get
  private lazy val visibilityEnum = service.enums.get("visibility").getOrElse {
    sys.error("No visibility enum found")
  }

  it("namedParametersInPath") {
    Util.namedParametersInPath("/users") should be(Seq.empty)
    Util.namedParametersInPath("/users/:guid") should be(Seq("guid"))
    Util.namedParametersInPath("/:org/foo/:version") should be(Seq("org", "version"))
    Util.namedParametersInPath("/:org/:service/:version") should be(Seq("org", "service", "version"))
  }

  it("isValidEnumValue") {
    Util.isValidEnumValue(visibilityEnum, "user") should be(true)
    Util.isValidEnumValue(visibilityEnum, "organization") should be(true)
    Util.isValidEnumValue(visibilityEnum, "foobar") should be(false)
  }

}
