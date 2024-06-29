package core

import cats.data.ValidatedNec
import helpers.ApiJsonHelpers
import io.apibuilder.api.json.v0.models.Field
import io.apibuilder.spec.v0.models.Service
import org.scalatest.Assertion
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsString, JsValue, Json}

class ServiceMapSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  private def setup[T](field: Field)(f: ValidatedNec[String, Service] => T): T= {
    f(
      TestHelper.serviceValidator(
        makeApiJson(
          models = Map("user" -> makeModel(fields = Seq(field)))
        )
      )
    )
  }

  private def expectError(typ: String, default: JsValue): Assertion = {
    setup(makeField(name = "tags", `type` = typ, default = Some(
      default
    ))) { v =>
      v.isInvalid shouldBe true
    }
  }

  private def expectSuccess(typ: String, default: JsValue): Assertion = {
    setup(makeField(name = "tags", `type` = typ, default = Some(
      default
    ))) { v =>
      val js = expectValid(v).models.head.fields.head.default.getOrElse {
        sys.error("Missing default")
      }
      Json.parse(js) should equal(default)
    }
  }

  it("accepts type: map, defaulting to element type of string for backwards compatibility") {
    setup(makeField(name = "tags", `type` = "map")) { v =>
      expectValid(v).models.head.fields.head.`type` should be("map[string]")
    }
  }

  it("accept defaults for maps") {
    setup(makeField(name = "tags", `type` = "map", default = Some(JsString("{ }")))) { v =>
      expectValid(v).models.head.fields.head.default shouldBe Some("{ }")
    }
  }

  it("validates invalid json") {
    def expectError(default: String, msg: String) = {
      setup(makeField(name = "tags", `type` = "map", default = Some(JsString(default)))) { v =>
        expectInvalid(v) should be(Seq(msg))
      }
    }
    expectError("bar", "Model[user] Field[tags] default[bar] is not valid json")
    // TODO: Track down why error message is different
    expectError("1", "Model[user] Field[tags] default[1] is not a valid JSON Object")
  }

  it("accepts valid defaults for map[string]") {
    val default = Json.obj("foo" -> "bar")
    setup(makeField(name = "tags", `type` = "map", default = Some(default))) { v =>
      expectValid(v).models.head.fields.head.default shouldBe Some(default.toString)
    }
  }

  it("accepts valid defaults for map[integer]") {
    val default = Json.obj("foo" -> 1)
    setup(makeField(name = "tags", `type` = "map", default = Some(default))) { v =>
      expectValid(v).models.head.fields.head.default shouldBe Some(default.toString)
    }
  }

  it("rejects invalid defaults for typed maps") {
    expectError("map[integer]", Json.obj("foo" -> "bar"))
    expectSuccess("map[integer]", Json.obj("foo" -> 1))

    expectError("map[boolean]", Json.obj("foo" -> "bar"))
    expectSuccess("map[boolean]", Json.obj("foo" -> true))
    expectSuccess("map[boolean]", Json.obj("foo" -> "true"))
  }

  it("accepts valid defaults for list") {
    expectError("[integer]", Json.arr("foo", "bar"))
    expectError("[integer]", Json.toJson("1"))
    expectSuccess("[integer]", Json.arr("1", "2"))
    expectSuccess("[string]", Json.arr("foo", "bar"))
  }

}
