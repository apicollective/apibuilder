package me.apidoc.swagger

import lib.ServiceConfiguration
import org.scalatest.{FunSpec, Matchers}

class UtilSpec extends FunSpec with Matchers {

  it("toMap") {
    Util.toMap(null) should be(Map())
    Util.toMap(java.util.Collections.emptyMap()) should be(Map.empty)

    val map = new java.util.HashMap[String, String]()
    map.put("foo", "bar")
    Util.toMap(map) should be(Map("foo" -> "bar"))
  }

}
