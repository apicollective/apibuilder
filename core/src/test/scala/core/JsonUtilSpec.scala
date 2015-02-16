package core

import play.api.libs.json.{Json, JsUndefined}
import org.scalatest.{FunSpec, Matchers}

class JsonUtilSpec extends FunSpec with Matchers {

  it("asOptString") {
    JsonUtil.asOptString(Json.toJson("")) should be(None)
    JsonUtil.asOptString(Json.toJson("  ")) should be(None)
    JsonUtil.asOptString(JsUndefined("null")) should be(None)

    JsonUtil.asOptString(Json.toJson("foo")) should be(Some("foo"))
    JsonUtil.asOptString(Json.toJson("  foo  ")) should be(Some("foo"))
  }

  it("asOptBoolean") {
    JsonUtil.asOptBoolean(Json.toJson("")) should be(None)
    JsonUtil.asOptBoolean(JsUndefined("null")) should be(None)

    JsonUtil.asOptBoolean(Json.toJson("true")) should be(Some(true))
    JsonUtil.asOptBoolean(Json.toJson("false")) should be(Some(false))
    JsonUtil.asOptBoolean(Json.toJson("  false  ")) should be(Some(false))

    JsonUtil.asOptBoolean(Json.toJson("foo")) should be(None)
  }

  it("asOptLong") {
    JsonUtil.asOptLong(Json.toJson("")) should be(None)
    JsonUtil.asOptLong(JsUndefined("null")) should be(None)

    JsonUtil.asOptLong(Json.toJson("1")) should be(Some(1l))
    JsonUtil.asOptLong(Json.toJson(" 1 ")) should be(Some(1l))

    JsonUtil.asOptLong(Json.toJson("foo")) should be(None)
  }

  it("hasKey") {
    JsonUtil.hasKey(Json.obj("a" -> 1), "a") should be(true)
    JsonUtil.hasKey(Json.obj("a" -> 1), "b") should be(false)
  }

}
