package lib

import org.scalatest.{FunSpec, ShouldMatchers}

class ServiceUriSpec extends FunSpec with ShouldMatchers {

  describe("parse") {

    it("invalid URIs") {
      ServiceUri.parse("") should be(None)
      ServiceUri.parse("foo") should be(None)
      ServiceUri.parse("http://foo") should be(None)
      ServiceUri.parse("http://foo:9000/123") should be(None)
      ServiceUri.parse("http://localhost:9000/gilt/apidoc/latest/service") should be(None)
    }

    it("valid URIs") {
      ServiceUri.parse("http://localhost:9000/gilt/apidoc/latest/service.json") should be(Some(ServiceUri(
        host = "localhost:9000",
        org = "gilt",
        app = "apidoc",
        version = "latest"
      )))

      ServiceUri.parse("http://localhost:9000/gilt/apidoc/0.9.1-dev/service.json") should be(Some(ServiceUri(
        host = "localhost:9000",
        org = "gilt",
        app = "apidoc",
        version = "0.9.1-dev"
      )))

      ServiceUri.parse(" HTTPS://localhost:9000/gilt/apidoc/0.9.1-dev/service.json") should be(Some(ServiceUri(
        host = "localhost:9000",
        org = "gilt",
        app = "apidoc",
        version = "0.9.1-dev"
      )))
    }

  }

}
