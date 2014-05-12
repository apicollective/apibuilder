package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class TextSpec extends FunSpec with Matchers {

  it("isValidName") {
    Text.isValidName("") should be(false)
    Text.isValidName("_vendor") should be(false)
    Text.isValidName("1vendor") should be(false)
    Text.isValidName("1") should be(false)
    Text.isValidName("_") should be(false)
    Text.isValidName("vendor") should be(true)
    Text.isValidName("Vendor") should be(true)
  }

  it("validateName") {
    Text.validateName("") should be(Seq("Name cannot be blank"))
    Text.validateName("_vendor") should be(Seq("Name must start with a letter"))
    Text.validateName("1vendor") should be(Seq("Name must start with a letter"))
    Text.validateName("1") should be(Seq("Name must start with a letter"))
    Text.validateName("_") should be(Seq("Name must start with a letter"))
    Text.validateName("vendor") should be(Seq.empty)
    Text.validateName("Vendor") should be(Seq.empty)
  }

  describe("isAlphaNumeric") {

    it("for alpha numeric strings") {
      Text.isAlphaNumeric("") should be(true)
      Text.isAlphaNumeric("this_dog") should be(true)
      Text.isAlphaNumeric("this_DOG") should be(true)
      Text.isAlphaNumeric("this_dog_1") should be(true)
    }

    it("for non alpha numeric strings") {
      Text.isAlphaNumeric("-") should be(false)
      Text.isAlphaNumeric("this!") should be(false)
      Text.isAlphaNumeric(" this") should be(false)
    }

  }

  describe("startsWithLetter") {

    it("for valid strings") {
      Text.startsWithLetter("this_dog") should be(true)
      Text.startsWithLetter("This dog") should be(true)
      Text.startsWithLetter("this!") should be(true)
    }

    it("for invalid strings") {
      Text.startsWithLetter("-") should be(false)
      Text.startsWithLetter("!this") should be(false)
      Text.startsWithLetter("_ this") should be(false)
      Text.startsWithLetter("1this") should be(false)
    }

  }

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

      Text.pluralize("species") should be("species")
    }

  }

}
