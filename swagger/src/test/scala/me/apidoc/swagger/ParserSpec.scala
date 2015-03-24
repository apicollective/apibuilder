package me.apidoc.swagger

import lib.ServiceConfiguration
import org.scalatest.{FunSpec, Matchers}

class ParserSpec extends FunSpec with Matchers {

  it("substitutePathParameters") {
    Parser.substitutePathParameters("/pets/:id") should be("/pets/:id")
    Parser.substitutePathParameters("/pets/{id}") should be("/pets/:id")
    Parser.substitutePathParameters("/stores/{id}/pets") should be("/stores/:id/pets")
    Parser.substitutePathParameters("/stores/{guid}/pets") should be("/stores/:guid/pets")
  }

}
