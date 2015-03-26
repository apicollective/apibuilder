package me.apidoc.swagger

import lib.ServiceConfiguration
import org.scalatest.{FunSpec, Matchers}

class ConvertersSpec extends FunSpec with Matchers {

  it("substitutePathParameters") {
    Converters.substitutePathParameters("/pets/:id") should be("/pets/:id")
    Converters.substitutePathParameters("/pets/{id}") should be("/pets/:id")
    Converters.substitutePathParameters("/stores/{id}/pets") should be("/stores/:id/pets")
    Converters.substitutePathParameters("/stores/{guid}/pets") should be("/stores/:guid/pets")
  }

  it("baseUrls") {
    Converters.baseUrls(Nil, "localhost", None) should be(Nil)
    Converters.baseUrls(Seq("http"), "localhost", None) should be(Seq("http://localhost"))
    Converters.baseUrls(Seq("HTTP"), "localhost", None) should be(Seq("http://localhost"))
    Converters.baseUrls(Seq("https"), "localhost", None) should be(Seq("https://localhost"))
    Converters.baseUrls(Seq("https"), "localhost", Some("/api")) should be(Seq("https://localhost/api"))
    Converters.baseUrls(Seq("http", "https"), "localhost", Some("/api")) should be(Seq("http://localhost/api", "https://localhost/api"))
    Converters.baseUrls(Seq("https", "http"), "localhost", Some("/api")) should be(Seq("https://localhost/api", "http://localhost/api"))
  }

}
