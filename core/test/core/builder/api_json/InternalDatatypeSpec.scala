package builder.api_json

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class InternalDatatypeSpec extends AnyFunSpec with Matchers {

  private val internalDatatypeBuilder = InternalDatatypeBuilder()

  it("label") {
    Seq("string", "uuid", "[string]", "[uuid]", "map[string]", "map[uuid]").foreach { name =>
      val dt = internalDatatypeBuilder.fromString(name).toOption.get
      dt.label should be(name)
      dt.required should be(true)
    }
  }

  it("map defaults to string type") {
    internalDatatypeBuilder.fromString("map").toOption.get.label should be("map[string]")
  }

  it("handles malformed input") {
    internalDatatypeBuilder.fromString("[").toOption.get.label should be("[")

    internalDatatypeBuilder.fromString("]").toOption.get.label should be("]")

    // Questionable how best to handle this. For now we allow empty
    // string - will get caught downstream when validating that the
    // name of the datatype is a valid name
    internalDatatypeBuilder.fromString("[]").toOption.get.label should be("[]")
  }

}
