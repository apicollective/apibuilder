package util

import org.apache.commons.codec.binary.Base64
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

class BasicAuthorizationSpec extends PlaySpec with OneAppPerSuite {

  def encode(v: String):String = {
    new String(Base64.encodeBase64(v.getBytes))
  }

  it("parses a valid token") {
    BasicAuthorization.get("Basic " + encode("abc")) must be(Some(BasicAuthorization.Token("abc")))
  }

  it("parses a valid optional token") {
    BasicAuthorization.get(Some("Basic " + encode("abc"))) must be(Some(BasicAuthorization.Token("abc")))
  }

  it("parses a valid token with a colon") {
    BasicAuthorization.get("Basic " + encode("abc:")) must be(Some(BasicAuthorization.Token("abc")))
  }

  it("parses username and password") {
    BasicAuthorization.get("Basic " + encode("user:pass")) must be(Some(BasicAuthorization.User("user", "pass")))
  }

  it("Ignores non basic auth") {
    BasicAuthorization.get("Other " + encode("user:pass")) must be(None)
  }

  it("Ignores invalid auth") {
    BasicAuthorization.get("Basic " + encode("user:pass:other")) must be(None)
  }

  it("Ignores empty string") {
    BasicAuthorization.get("Basic " + encode("")) must be(None)
  }

}
