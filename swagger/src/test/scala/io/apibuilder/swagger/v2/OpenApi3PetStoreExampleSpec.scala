package io.apibuilder.swagger.v2

import _root_.helpers.{ServiceConfigurationHelpers, TestHelpers}
import io.apibuilder.swagger.helpers.FileHelpers
import lib.ServiceConfiguration
import org.scalatest.{FunSpec, Matchers}

// Test examples specs from https://github.com/OAI/OpenAPI-Specification/tree/main/examples/v3.0
class OpenApi3PetStoreExampleSpec extends FunSpec with Matchers
  with FileHelpers
  with ServiceConfigurationHelpers
  with TestHelpers
{

  private[this] val config: ServiceConfiguration = makeServiceConfiguration()
  private[this] lazy val svc = expectValid {
    V2Parser(config).parse(readResource("petstore-20211212.yml"))
  }

  it("apidoc") {
    svc.apidoc shouldBe V2ParserConstants.ApiDocConstant
  }

  it("name") {
    svc.name shouldBe "Swagger Petstore"
  }

  it("organization") {
    svc.organization.key shouldBe config.orgKey
  }

  it("application") {
    svc.application.key shouldBe "swagger-petstore"
  }

  it("namespace") {
    svc.namespace shouldBe config.orgNamespace
  }

  it("version") {
    svc.version shouldBe "1.0.0"
  }

  it("info.contact") {
    val c = svc.info.contact.get
    c.name.get shouldBe "Swagger API Team"
    c.email.get shouldBe "apiteam@swagger.io"
    c.url.get shouldBe "http://swagger.io"
  }

  it("info.license") {
    val c = svc.info.license.get
    c.name shouldBe "Apache 2.0"
    c.url.get shouldBe "https://www.apache.org/licenses/LICENSE-2.0.html"
  }

}
