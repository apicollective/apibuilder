package util

import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.apache.commons.codec.binary.Base64

class BasicAuthorizationSpec extends FunSpec with Matchers {

  def encode(v: String):String = {
    new String(Base64.encodeBase64(v.getBytes))
  }

  it("parses a valid token") {
    BasicAuthorization.get("Basic " + encode("abc")) should be(Some(BasicAuthorization.Token("abc")))
  }

  it("parses a valid optional token") {
    BasicAuthorization.get(Some("Basic " + encode("abc"))) should be(Some(BasicAuthorization.Token("abc")))
  }

  it("parses a valid token with a colon") {
    BasicAuthorization.get("Basic " + encode("abc:")) should be(Some(BasicAuthorization.Token("abc")))
  }

  it("parses username and password") {
    BasicAuthorization.get("Basic " + encode("user:pass")) should be(Some(BasicAuthorization.User("user", "pass")))
  }

  it("Ignores non basic auth") {
    BasicAuthorization.get("Other " + encode("user:pass")) should be(None)
  }

  it("Ignores empty string") {
    BasicAuthorization.get("Basic " + encode("")) should be(None)
  }

}
