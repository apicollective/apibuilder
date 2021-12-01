package io.apibuilder.swagger

import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json._

class UtilSpec extends FunSpec with Matchers {

  it("formatName") {
    Util.formatName("pet") should be("pet")
    Util.formatName("  pet  ") should be("pet")
  }

  it("substitutePathParameters") {
    Util.substitutePathParameters("/pets/:id") should be("/pets/:id")
    Util.substitutePathParameters("/pets/{id}") should be("/pets/:id")
    Util.substitutePathParameters("/stores/{id}/pets") should be("/stores/:id/pets")
    Util.substitutePathParameters("/stores/{guid}/pets") should be("/stores/:guid/pets")
  }

  it("combine") {
    Util.combine(Nil) should be(None)
    Util.combine(Seq(None)) should be(None)
    Util.combine(Seq(Some(""))) should be(None)
    Util.combine(Seq(Some("foo"))) should be(Some("foo"))
    Util.combine(Seq(Some("foo"), Some("bar"))) should be(Some("foo\n\nbar"))
    Util.combine(Seq(Some("foo"), None, Some("bar"))) should be(Some("foo\n\nbar"))
    Util.combine(Seq(Some("foo"), None, Some("bar")), connector = ", ") should be(Some("foo, bar"))
  }

  it("normalizeUrl") {
    Util.normalizeUrl("/foo") should be("/foo")
    Util.normalizeUrl("/foo_bar") should be("/foo-bar")
    Util.normalizeUrl("/FOO_BAR") should be("/foo-bar")
    Util.normalizeUrl("  /FOO_BAR  ") should be("/foo-bar")
  }

  it("choose") {
    Util.choose(None, None) should be(None)
    Util.choose(Some("a"), None) should be(Some("a"))
    Util.choose(Some("a"), Some("b")) should be(Some("a"))
    Util.choose(None, Some("b")) should be(Some("b"))
  }

  it("swaggerAnyToJsValue") {
    import collection.JavaConverters._
    case class Unsupported(foo: String)
    Util.swaggerAnyToJsValue("foo") should be(JsString("foo"))
    Util.swaggerAnyToJsValue(false) should be(JsFalse)
    Util.swaggerAnyToJsValue(true) should be(JsTrue)
    Util.swaggerAnyToJsValue(1.3) should be(JsNumber(BigDecimal(1.3)))
    Util.swaggerAnyToJsValue(1) should be(JsNumber(BigDecimal(1)))
    Util.swaggerAnyToJsValue(1.3f) should be(JsNumber(BigDecimal(1.3f)))
    Util.swaggerAnyToJsValue(Seq("foo", 1, Seq("bar").asJava).asJava) should be(JsArray(Seq(JsString("foo"), JsNumber(BigDecimal(1)), JsArray(Seq(JsString("bar"))))))
    Util.swaggerAnyToJsValue(Map("foo" -> 1, "bar" -> Seq("baz", 1.3).asJava).asJava) should be(JsObject(Seq("foo" -> JsNumber(BigDecimal(1)), "bar" -> JsArray(Seq(JsString("baz"), JsNumber(BigDecimal(1.3)))))))
    Util.swaggerAnyToJsValue(Unsupported("bar")) should be(JsNull)
  }

  it("toOption") {
    Util.toOption(null) should be(None)
    Util.toOption("") should be(None)
    Util.toOption("    ") should be(None)
    Util.toOption("foo") should be(Some("foo"))
    Util.toOption("  foo  ") should be(Some("foo"))
  }

  it("toMap") {
    Util.toMap(null) should be(Map())
    Util.toMap(java.util.Collections.emptyMap()) should be(Map.empty)

    val map = new java.util.HashMap[String, String]()
    map.put("foo", "bar")
    Util.toMap(map) should be(Map("foo" -> "bar"))
  }

  it("toArray") {
    Util.toArray(null) should be(Nil)
    Util.toArray(java.util.Collections.emptyList()) should be(Nil)

    val list = new java.util.ArrayList[String]()
    list.add("foo")
    Util.toArray(list) should be(Seq("foo"))
  }

  it("writeToTempFile") {
    val path = Util.writeToTempFile("testing")
    scala.io.Source.fromFile(path).getLines.mkString("\n") should be("testing")
  }

}
