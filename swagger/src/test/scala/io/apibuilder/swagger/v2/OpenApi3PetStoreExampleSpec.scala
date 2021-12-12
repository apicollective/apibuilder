package io.apibuilder.swagger.v2

import _root_.helpers.{ServiceConfigurationHelpers, TestHelpers}
import io.apibuilder.spec.v0.models.{Field, Model}
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

  it("baseUrl") {
    svc.baseUrl.get shouldBe "http://petstore.swagger.io/api"
  }

  it("description") {
    svc.description.get shouldBe "A sample API that uses a petstore as an example to demonstrate features in the OpenAPI 3.0 specification"
  }

  it("models") {
    def model[T](name: String)(f: Model => T): T = {
      f(svc.models.find(_.name == name).get)
    }
    def field[T](model: Model, name: String)(f: Field => T): T = {
      f(model.fields.find(_.name == name).get)
    }

    svc.models.map(_.name).sorted shouldBe Seq("Error", "NewPet", "Pet").sorted

    model("Error") { m =>
      m.plural shouldBe "Errors"
      m.description shouldBe None
      m.deprecation shouldBe None
      m.fields.map(_.name) shouldBe Seq("code", "message")

      field(m, "code") { f =>
        f.required shouldBe true
        f.`type` shouldBe "integer"
        f.description shouldBe None
      }

      field(m, "message") { f =>
        f.required shouldBe true
        f.`type` shouldBe "string"
        f.description shouldBe None
      }
    }


    model("NewPet") { m =>
      m.plural shouldBe "NewPets"
      m.description shouldBe None
      m.deprecation shouldBe None
      m.fields.map(_.name) shouldBe Seq("name", "tag")

      field(m, "name") { f =>
        f.required shouldBe true
        f.`type` shouldBe "string"
        f.description shouldBe None
      }

      field(m, "tag") { f =>
        f.required shouldBe false
        f.`type` shouldBe "string"
        f.description shouldBe None
      }
    }

  }
}
