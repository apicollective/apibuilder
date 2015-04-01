package me.apidoc.swagger.translators

import lib.ServiceConfiguration
import org.scalatest.{FunSpec, Matchers}

class BaseUrlSpec extends FunSpec with Matchers {

  it("apply") {
    BaseUrl(Nil, "localhost", None) should be(Nil)
    BaseUrl(Seq("http"), "localhost", None) should be(Seq("http://localhost"))
    BaseUrl(Seq("HTTP"), "localhost", None) should be(Seq("http://localhost"))
    BaseUrl(Seq("https"), "localhost", None) should be(Seq("https://localhost"))
    BaseUrl(Seq("https"), "localhost", Some("/api")) should be(Seq("https://localhost/api"))
    BaseUrl(Seq("http", "https"), "localhost", Some("/api")) should be(Seq("http://localhost/api", "https://localhost/api"))
    BaseUrl(Seq("https", "http"), "localhost", Some("/api")) should be(Seq("https://localhost/api", "http://localhost/api"))
  }

}
