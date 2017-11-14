package builder.api_json

import org.scalatest.{FunSpec, Matchers}

class InternalDatatypeSpec extends FunSpec with Matchers {

  private[this] val internalDatatypeBuilder = InternalDatatypeBuilder()

  it("label") {
    Seq("string", "uuid", "[string]", "[uuid]", "map[string]", "map[uuid]").foreach { name =>
      val dt = internalDatatypeBuilder.fromString(name).right.get
      dt.label should be(name)
      dt.required should be(true)
    }
  }

  it("map defaults to string type") {
    internalDatatypeBuilder.fromString("map").right.get.label should be("map[string]")
  }

  it("handles malformed input") {
    internalDatatypeBuilder.fromString("[").right.get.label should be("[")

    internalDatatypeBuilder.fromString("]").right.get.label should be("]")

    // Questionable how best to handle this. For now we allow empty
    // string - will get caught downstream when validating that the
    // name of the datatype is a valid name
    internalDatatypeBuilder.fromString("[]").right.get.label should be("[]")
  }

}
