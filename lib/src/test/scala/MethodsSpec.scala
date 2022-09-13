package lib

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class MethodsSpec extends AnyFunSpec with Matchers {

  it("supportsBody") {
    Methods.supportsBody("GET") should be(false)
    Methods.supportsBody("get") should be(false)
    Methods.supportsBody("DELETE") should be(true)
    Methods.supportsBody("delete") should be(true)
    Methods.supportsBody("POST") should be(true)
    Methods.supportsBody("post") should be(true)
    Methods.supportsBody("PUT") should be(true)
    Methods.supportsBody("put") should be(true)
    Methods.supportsBody("PATCH") should be(true)
    Methods.supportsBody("patch") should be(true)
  }

}
