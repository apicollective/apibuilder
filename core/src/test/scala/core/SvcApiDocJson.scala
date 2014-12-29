package core

import com.gilt.apidocspec.models.Method
import org.scalatest.{FunSpec, Matchers}

class SvcApiDocJson extends FunSpec with Matchers {

  private val Path = "api/api.json"
  private lazy val service = TestHelper.parseFile(Path).service.get

  it("parses models") {
    val models = service.models.map(_.name)
    models.contains("foo") should be(false)
    models.contains("user") should be(true)
    models.contains("organization") should be(true)

    val user = service.models.find(_.name == "user").get
    user.fields.find(_.name == "guid").get.`type` should be("uuid")
    user.fields.find(_.name == "email").get.`type` should be("string")
  }

  it("parses resources") {
    val resources = service.resources.map(_.model.name)
    resources.contains("foo") should be(false)
    resources.contains("user") should be(true)
    resources.contains("organization") should be(true)
  }

  it("has defaults for all limit and offset parameters") {
    service.resources.flatMap(_.operations.filter(_.method == Method.Get)).foreach { op =>

      op.parameters.find { _.name == "limit" }.map { p =>
        p.default match {
          case None => fail("no default specified for limit param")
          case Some(v) => v.toInt should be >= 25
        }
      }

      op.parameters.find { _.name == "offset" }.map { p =>
        p.default should be(Some("0"))
      }

    }
  }

  it("all POST operations return either a 200, 201, 204 or a 409") {
    val validCodes = Seq(200, 201, 204, 409)
    service.resources.flatMap(_.operations.filter(_.method == Method.Post)).foreach { op =>
      op.responses.find { r => !validCodes.contains(r.code)}.foreach { code =>
        fail(s"POST operation should return a ${validCodes.mkString(", ")} - invalid response for op[$op] response[$code]")
      }
    }
  }

}
