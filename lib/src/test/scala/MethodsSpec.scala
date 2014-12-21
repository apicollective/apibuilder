package core

import org.scalatest.{FunSpec, Matchers}

class MethodsSpec extends FunSpec with Matchers {

  it("isJsonDocumentMethod") {
    Method.isJsonDocumentMethod("GET") should be(false)
    Method.isJsonDocumentMethod("get") should be(false)
    Method.isJsonDocumentMethod("DELETE") should be(false)
    Method.isJsonDocumentMethod("delete") should be(false)
    Method.isJsonDocumentMethod("POST") should be(true)
    Method.isJsonDocumentMethod("post") should be(true)
    Method.isJsonDocumentMethod("PUT") should be(true)
    Method.isJsonDocumentMethod("put") should be(true)
    Method.isJsonDocumentMethod("PATCH") should be(true)
    Method.isJsonDocumentMethod("patch") should be(true)
  }

}
