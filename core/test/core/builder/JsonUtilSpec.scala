package builder

import play.api.libs.json.{Json, JsUndefined}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class JsonUtilSpec extends AnyFunSpec with Matchers {

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

    JsonUtil.asOptLong(Json.toJson("1")) should be(Some(1L))
    JsonUtil.asOptLong(Json.toJson(" 1 ")) should be(Some(1L))

    JsonUtil.asOptLong(Json.toJson("foo")) should be(None)
  }

  it("hasKey") {
    JsonUtil.hasKey(Json.obj("a" -> 1), "a") should be(true)
    JsonUtil.hasKey(Json.obj("a" -> 1), "b") should be(false)
  }

  it("parseBoolean") {
    JsonUtil.parseBoolean("true") should be(Some(true))
    JsonUtil.parseBoolean("false") should be(Some(false))

    Seq("", "foo", "[]", "{}").foreach { value =>
      JsonUtil.parseBoolean(value) should be(None)
    }
  }

  it("isNumeric") {
    Seq("1", "-1", "5").foreach { value =>
      JsonUtil.isNumeric(value) should be(true)
    }
      
    Seq("", "foo", "[]", "{}").foreach { value =>
      JsonUtil.isNumeric(value) should be(false)
    }
  }

  it("asSeqOfString") {
    JsonUtil.asSeqOfString(Json.toJson("")) should be(Nil)
    JsonUtil.asSeqOfString(Json.toJson("  ")) should be(Nil)
    JsonUtil.asSeqOfString(JsUndefined("null")) should be(Nil)

    JsonUtil.asSeqOfString(Json.toJson("foo")) should be(Seq("foo"))
    JsonUtil.asSeqOfString(Json.toJson("  foo  ")) should be(Seq("foo"))

    JsonUtil.asSeqOfString(Json.parse("""["foo"]""")) should be(Seq("foo"))
    JsonUtil.asSeqOfString(Json.parse("""["foo","bar"]""")) should be(Seq("foo","bar"))
  }
}
