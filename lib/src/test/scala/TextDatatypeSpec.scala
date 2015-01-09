package lib

import org.scalatest.{FunSpec, Matchers}

class TextDatatypeSpec extends FunSpec with Matchers {

  it("primitives") {
    TextDatatype("string").label should be("string")
    TextDatatype("long").label should be("long")
    TextDatatype("uuid").label should be("uuid")
    TextDatatype("unit").label should be("unit")
    TextDatatype("integer").label should be("integer")
    TextDatatype("date-time-iso8601").label should be("date-time-iso8601")
    TextDatatype("string | uuid").label should be("string | uuid")
    TextDatatype("string | uuid | unit").label should be("string | uuid | unit")

    TextDatatype("[string]").label should be("[string]")
    TextDatatype("[long]").label should be("[long]")
    TextDatatype("[uuid]").label should be("[uuid]")
    TextDatatype("[unit]").label should be("[unit]")
    TextDatatype("[integer]").label should be("[integer]")
    TextDatatype("[date-time-iso8601]").label should be("[date-time-iso8601]")
    TextDatatype("[string | uuid]").label should be("[string | uuid]")
    TextDatatype("[string | uuid | unit]").label should be("[string | uuid | unit]")

    TextDatatype("map").label should be("map[string]")
    TextDatatype("map[string]").label should be("map[string]")
    TextDatatype("map[long]").label should be("map[long]")
    TextDatatype("map[uuid]").label should be("map[uuid]")
    TextDatatype("map[unit]").label should be("map[unit]")
    TextDatatype("map[integer]").label should be("map[integer]")
    TextDatatype("map[date-time-iso8601]").label should be("map[date-time-iso8601]")
    TextDatatype("map[string | uuid]").label should be("map[string | uuid]")
    TextDatatype("map[string | uuid | unit]").label should be("map[string | uuid | unit]")

    TextDatatype("option[string]").label should be("option[string]")
    TextDatatype("option[long]").label should be("option[long]")
    TextDatatype("option[uuid]").label should be("option[uuid]")
    TextDatatype("option[unit]").label should be("option[unit]")
    TextDatatype("option[integer]").label should be("option[integer]")
    TextDatatype("option[date-time-iso8601]").label should be("option[date-time-iso8601]")
    TextDatatype("option[string | uuid]").label should be("option[string | uuid]")
    TextDatatype("option[string | uuid | unit]").label should be("option[string | uuid | unit]")

    TextDatatype("user").label should be("user")
    TextDatatype("string | user").label should be("string | user")
  }

}
