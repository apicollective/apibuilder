package core

import org.scalatest.{FunSpec, Matchers}

class InternalDatatypeSpec extends FunSpec with Matchers {

  it("label") {
    Seq("string", "uuid", "[string]", "[uuid]", "map[string]", "map[uuid]").foreach { name =>
      val dt = InternalDatatype(name)
      dt.label should be(name)
      dt.required should be(true)
    }
  }

  it("map defaults to string type") {
    InternalDatatype("map").label should be("map[string]")
  }

  it("handles malformed input") {
    InternalDatatype("[").label should be("[")

    InternalDatatype("]").label should be("]")

    // Questionable how best to handle this. For now we allow empty
    // string - will get caught downstream when validating that the
    // name of the datatype is a valid name
    InternalDatatype("[]").label should be("[]")
  }

}
