package lib

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class TextSpec extends FunSpec with Matchers {

  it("splitIntoWords") {
    Text.splitIntoWords("foo") should be(Seq("foo"))
    Text.splitIntoWords("foo_bar") should be(Seq("foo", "bar"))
    Text.splitIntoWords("foo-bar") should be(Seq("foo", "bar"))
    Text.splitIntoWords("foo.bar") should be(Seq("foo", "bar"))
    Text.splitIntoWords("foo:bar") should be(Seq("foo", "bar"))
  }

  it("isValidName") {
    Text.isValidName("") should be(false)
    Text.isValidName("_vendor") should be(false)
    Text.isValidName("1vendor") should be(false)
    Text.isValidName("1") should be(false)
    Text.isValidName("_") should be(false)
    Text.isValidName("some vendor") should be(false)
    Text.isValidName("vendor") should be(true)
    Text.isValidName("Vendor") should be(true)
    Text.isValidName("some_vendor") should be(true)
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

    it("KnownPlurals are handled (e.g. fish -> fish)") {
      Seq("data", "people", "species").foreach { word =>
        Text.pluralize(word) should be(word)
      }
    }

    it("pluralizes standard words") {
      // see https://en.wikipedia.org/wiki/English_plurals
      // see http://www.oxforddictionaries.com/us/words/plurals-of-nouns
      val actuals = Map(
        // Where a singular noun ends in a sibilant sound —/s/, /z/,
        // /ʃ/, /ʒ/, /tʃ/ or /dʒ/— the plural is formed by adding
        // /ɨz/. The spelling adds -es, or -s if the singular already
        // ends in -e:
        "kiss" -> "kisses",
        "phase" -> "phases",

        // When the singular form ends in a voiceless consonant (other
        // than a sibilant) —/p/, /t/, /k/, /f/ (sometimes) or /θ/—
        // the plural is formed by adding /s/. The spelling adds -s:
        "lap" -> "laps",
        "cat" -> "cats",

        // For all other words (i.e. words ending in vowels or voiced
        // non-sibilants) the regular plural adds /z/, represented
        // orthographically by -s:
        "boy" -> "boys",
        "gilt" -> "gilts",
        "chair" -> "chairs",

        // With nouns ending in o preceded by a consonant, the plural
        // in many cases is spelled by adding -es (pronounced /z/):
        "hero" -> "heroes",
        "potato" -> "potatoes",

        // Nouns ending in a y preceded by a consonant usually drop
        // the y and add -ies (pronounced /iz/, or /aiz/ in words
        // where the y is pronounced /ai/):
        "cherry" -> "cherries",
        "lady" -> "ladies",
        "sky" -> "skies",

        // Words ending in quy also follow this pattern:
        "soliloquy" -> "soliloquies",

        // Words ending in a y preceded by a vowel form their plurals by adding -s:
        "day" -> "days",
        "monkey" -> "monkeys",

        "berry" -> "berries",
        "activity" -> "activities",
        "daisy" -> "daisies",
        "church" -> "churches",
        "commit" -> "commits",
        "bus" -> "buses",
        "fox" -> "foxes",
        "epoch" -> "epochs",
        "knife" -> "knives",
        "half" -> "halves",
        "scarf" -> "scarves",
        "chief" -> "chiefs",
        "spoof" -> "spoofs",
        "solo" -> "solos",
        "zero" -> "zeros",
        "studio" -> "studios",
        "zoo" -> "zoos",
        "embryo" -> "embryos",
        "domino" -> "dominoes",
        "echo" -> "echoes",
        "embargo" -> "embargoes",
        "user" -> "users",
        "address" -> "addresses",
        "price" -> "prices",
        "metadata" -> "metadata",
        "family" -> "families",
        "datum" -> "data",
        "person" -> "people",
        "species" -> "species",

        // Camelcare, snake case
        "error_message" -> "error_messages",
        "error_messages" -> "error_messages",
        "errorMessage" -> "errorMessages",
        "errorMessages" -> "errorMessages",

        // Random other cases we've seen
        "variants" -> "variants"
      )

      val errors = actuals.flatMap { case (singular, plural) =>
        if (Text.pluralize(singular) != plural) {
          Some("a: %s should have been %s but was %s".format(singular, plural, Text.pluralize(singular)))
        } else if (Text.pluralize(plural) != plural) {
          Some("a: %s should have been %s but was %s".format(plural, plural, Text.pluralize(plural)))
          None // TODO
        } else {
          None
        }
      }

      errors.toSeq.sorted.mkString("\n") should be("")
    }

  }

  it("initCap") {
    Text.initCap("foo") should be("Foo")
    Text.initCap("Foo") should be("Foo")
    Text.initCap("foo_bar") should be("Foo_bar")
    Text.initCap("Foo_bar") should be("Foo_bar")
    Text.initCap("foo") should be("Foo")
    Text.initCap("foo_bar") should be("Foo_bar")
    Text.initCap("foo_bar_baz") should be("Foo_bar_baz")
  }

  it("initLowerCase") {
    Text.initLowerCase("foo") should be("foo")
    Text.initLowerCase("Foo") should be("foo")
    Text.initLowerCase("foo_bar") should be("foo_bar")
    Text.initLowerCase("Foo_bar") should be("foo_bar")
    Text.initLowerCase("FooBar") should be("fooBar")
    Text.initLowerCase("fooBar") should be("fooBar")
  }

  it("safeName") {
    Text.safeName("foo") should be("foo")
    Text.safeName("val") should be("val")
    Text.safeName("foo Bar") should be("fooBar")
  }

  it("underscoreToInitCap") {
    Text.underscoreToInitCap("FooBar") should be("FooBar")
    Text.underscoreToInitCap("fooBar") should be("FooBar")
    Text.underscoreToInitCap("foo_bar") should be("FooBar")
  }

  it("underscoreAndDashToInitCap") {
    Text.underscoreAndDashToInitCap("FooBar") should be("FooBar")
    Text.underscoreAndDashToInitCap("fooBar") should be("FooBar")
    Text.underscoreAndDashToInitCap("foo_bar") should be("FooBar")
    Text.underscoreAndDashToInitCap("FooBar") should be("FooBar")
    Text.underscoreAndDashToInitCap("fooBar") should be("FooBar")
    Text.underscoreAndDashToInitCap("foo-bar") should be("FooBar")
    Text.underscoreAndDashToInitCap("foo-bar_baz") should be("FooBarBaz")
  }

  it("camelCaseToUnderscore") {
    Text.camelCaseToUnderscore("Hey") should be("Hey")
    Text.camelCaseToUnderscore("HeyThere") should be("Hey_There")
    Text.camelCaseToUnderscore("heyThere") should be("hey_There")
    Text.camelCaseToUnderscore("UUID") should be("uuid")
  }

  it("snakeToCamelCase") {
    Text.snakeToCamelCase("foo") should be("foo")
    Text.snakeToCamelCase("foo_bar") should be("fooBar")
    Text.snakeToCamelCase("foo_bar_baz") should be("fooBarBaz")
  }

}
