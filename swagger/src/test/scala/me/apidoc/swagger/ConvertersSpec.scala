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

  it("baseUrl") {
    Converters.baseUrl(Nil, "localhost", None) should be("http://localhost")
    Converters.baseUrl(Seq("http"), "localhost", None) should be("http://localhost")
    Converters.baseUrl(Seq("HTTP"), "localhost", None) should be("http://localhost")
    Converters.baseUrl(Seq("https"), "localhost", None) should be("https://localhost")
    Converters.baseUrl(Seq("https"), "localhost", Some("/api")) should be("https://localhost/api")
    Converters.baseUrl(Seq("http", "https"), "localhost", Some("/api")) should be("http://localhost/api")
    Converters.baseUrl(Seq("https", "http"), "localhost", Some("/api")) should be("https://localhost/api")
  }

}
