package me.apidoc.swagger

import lib.ServiceConfiguration
import org.scalatest.{FunSpec, Matchers}

class UtilSpec extends FunSpec with Matchers {

  it("formatName") {
    Util.formatName("pet") should be("pet")
    Util.formatName("  pet  ") should be("pet")
  }

  it("choose") {
    Util.choose(None, None) should be(None)
    Util.choose(Some("a"), None) should be(Some("a"))
    Util.choose(Some("a"), Some("b")) should be(Some("a"))
    Util.choose(None, Some("b")) should be(Some("b"))
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

}
