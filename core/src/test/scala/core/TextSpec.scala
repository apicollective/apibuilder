package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class TextSpec extends FunSpec with Matchers {

  describe("truncate") {

    it("does not truncate short text") {
      Text.truncate("foo") should be("foo")
      Text.truncate("This is") should be("This is")
    }

    it("respects limit") {
      Text.truncate("1234567890", 10) should be("1234567890")
      Text.truncate("12345678900", 10) should be("1234567...")
      Text.truncate("This is a long sentence", 50) should be("This is a long sentence")
      Text.truncate("This is a long sentence", 10) should be("This is...")
    }
  }

  describe("pluralize") {

    it("pluralizes standard words") {
      Text.pluralize("user") should be("users")
      Text.pluralize("address") should be("addresses")
      Text.pluralize("family") should be("families")

      Text.pluralize("datum") should be("data")
      Text.pluralize("data") should be("data")

      Text.pluralize("person") should be("people")
      Text.pluralize("people") should be("people")
    }

  }

}
