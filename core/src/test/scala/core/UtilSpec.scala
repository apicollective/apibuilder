package core

import org.scalatest.FunSpec

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class UtilSpec extends FunSpec with Matchers {

  it("isJsonDocumentMethod") {
    Util.isJsonDocumentMethod("GET") should be(false)
    Util.isJsonDocumentMethod("POST") should be(true)
    Util.isJsonDocumentMethod("PUT") should be(true)
    Util.isJsonDocumentMethod("PATCH") should be(true)
  }

  it("namedParametersInPath") {
    Util.namedParametersInPath("/users") should be(Seq.empty)
    Util.namedParametersInPath("/users/:guid") should be(Seq("guid"))
    Util.namedParametersInPath("/:org/docs/:version") should be(Seq("org", "version"))
    Util.namedParametersInPath("/:org/:service/:version") should be(Seq("org", "service", "version"))
  }

}
