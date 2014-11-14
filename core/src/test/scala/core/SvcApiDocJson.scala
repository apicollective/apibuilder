package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class SvcApiDocJson extends FunSpec with Matchers {

  private val Path = "api/api.json"
  private lazy val service = TestHelper.parseFile(Path).serviceDescription.get

  it("alphabetizes models") {
    service.models.map(_.name) should be(service.models.map(_.name).sorted)
  }

  it("alphabetizes resources") {
    service.resources.map(_.model.plural) should be(service.resources.map(_.model.plural).sorted)
  }

  it("parses models") {
    val models = service.models.map(_.name)
    models.contains("foo") should be(false)
    models.contains("user") should be(true)
    models.contains("organization") should be(true)

    val user = service.models.find(_.name == "user").get
    user.fields.find(_.name == "guid").get.`type` should be(TypeInstance(Container.Singleton, Type.Primitive(Primitives.Uuid)))
    user.fields.find(_.name == "email").get.`type` should be(TypeInstance(Container.Singleton, Type.Primitive(Primitives.String)))
  }

  it("parses resources") {
    val resources = service.resources.map(_.model.name)
    resources.contains("foo") should be(false)
    resources.contains("user") should be(true)
    resources.contains("organization") should be(true)
  }

  it("has defaults for all limit and offset parameters") {
    service.resources.flatMap(_.operations.filter(_.method == "GET")).foreach { op =>

      op.parameters.find { _.name == "limit" }.map { p =>
        p.default should be(Some("25"))
      }

      op.parameters.find { _.name == "offset" }.map { p =>
        p.default should be(Some("0"))
      }

    }
  }

  it("all POST operations return either a 200, 204 or a 409") {
    val validCodes = Seq(200, 204, 409)
    service.resources.flatMap(_.operations.filter(_.method == "POST")).foreach { op =>
      val invalid = op.responses.find { response => !validCodes.contains(response.code)}
      invalid.foreach { response =>
        fail(s"POST operation should return a 200, 204 or a 409 - invalid response for op[$op] response[$response")
      }
    }
  }

}
