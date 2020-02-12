package util

import org.apache.commons.codec.binary.Base64
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class BasicAuthorizationSpec extends PlaySpec with GuiceOneAppPerSuite {

  def encode(v: String):String = {
    new String(Base64.encodeBase64(v.getBytes))
  }

  "parses a valid token" in {
    BasicAuthorization.get("Basic " + encode("abc")) must be(Some(BasicAuthorization.Token("abc")))
  }

  "parses a valid optional token" in {
    BasicAuthorization.get(Some("Basic " + encode("abc"))) must be(Some(BasicAuthorization.Token("abc")))
  }

  "parses a valid token with a colon" in {
    BasicAuthorization.get("Basic " + encode("abc:")) must be(Some(BasicAuthorization.Token("abc")))
  }

  "parses username and password" in {
    BasicAuthorization.get("Basic " + encode("user:pass")) must be(Some(BasicAuthorization.User("user", "pass")))
  }

  "Ignores non basic auth" in {
    BasicAuthorization.get("Other " + encode("user:pass")) must be(None)
  }

  "Ignores invalid auth" in {
    BasicAuthorization.get("Bøasic " + encode("user:pass:other")) must be(None)
  }

  "Ignores empty string" in {
    BasicAuthorization.get("Basic " + encode("")) must be(None)
  }

}
