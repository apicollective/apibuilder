package lib

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class ServiceUriSpec extends PlaySpec with GuiceOneAppPerSuite {

  "parse" must {

    "invalid URIs" in {
      ServiceUri.parse("") must be(None)
      ServiceUri.parse("foo") must be(None)
      ServiceUri.parse("http://foo") must be(None)
      ServiceUri.parse("http://foo:9000/123") must be(None)
      ServiceUri.parse("http://localhost:9000/bryzek/apibuilder/latest/service") must be(None)
    }

    "valid URIs" in {
      ServiceUri.parse("http://localhost:9000/bryzek/apibuilder/latest/service.json") must be(Some(ServiceUri(
        host = "localhost:9000",
        org = "bryzek",
        app = "apibuilder",
        version = "latest"
      )))

      ServiceUri.parse("http://localhost:9000/bryzek/apibuilder/0.9.1-dev/service.json") must be(Some(ServiceUri(
        host = "localhost:9000",
        org = "bryzek",
        app = "apibuilder",
        version = "0.9.1-dev"
      )))

      ServiceUri.parse(" HTTPS://localhost:9000/bryzek/apibuilder/0.9.1-dev/service.json") must be(Some(ServiceUri(
        host = "localhost:9000",
        org = "bryzek",
        app = "apibuilder",
        version = "0.9.1-dev"
      )))
    }

  }

}
