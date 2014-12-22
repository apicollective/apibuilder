package core

import com.gilt.apidocspec.models.Method
import lib.{Datatype, Primitives, Type, TypeKind}
import org.scalatest.{FunSpec, Matchers}

class SvcApiDocJson extends FunSpec with Matchers {

  private val Path = "api/api.json"
  private lazy val service = TestHelper.parseFile(Path).serviceDescription.get

  it("parses models") {
    val models = service.models.keys.toSet
    models.contains("foo") should be(false)
    models.contains("user") should be(true)
    models.contains("organization") should be(true)

    val user = service.models("user")
    user.fields.find(_.name == "guid").get.`type` should be(Datatype.Singleton(Type(TypeKind.Primitive, Primitives.Uuid.toString)))
    user.fields.find(_.name == "email").get.`type` should be(Datatype.Singleton(Type(TypeKind.Primitive, Primitives.String.toString)))
  }

  it("parses resources") {
    val resources = service.resources.keys.toSet
    resources.contains("foo") should be(false)
    resources.contains("user") should be(true)
    resources.contains("organization") should be(true)
  }

  it("has defaults for all limit and offset parameters") {
    service.resources.values.flatMap(_.operations.filter(_.method == Method.Get)).foreach { op =>

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
    val validCodes = Seq("200", "201", "204", "409")
    service.resources.values.flatMap(_.operations.filter(_.method == Method.Post)).foreach { op =>
      op.responses.keys.find { code => !validCodes.contains(code)}.foreach { code =>
        fail(s"POST operation should return a 200, 204 or a 409 - invalid response for op[$op] response[$code]")
      }
    }
  }

}
