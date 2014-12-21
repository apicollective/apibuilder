package core

import com.gilt.apidocspec.models.{Container, Datatype, Type, TypeKind}
import org.scalatest.{FunSpec, Matchers}

class InternalDatatypeSpec extends FunSpec with Matchers {

  it("label") {
    Seq("string", "uuid", "[string]", "[uuid]", "map[string]", "map[uuid]", "option[string]", "union[unit, string]", "union[string, boolean]").foreach { name =>
      InternalDatatype(name).label should be(name)
    }
  }

  it("map defaults to string type") {
    InternalDatatype("map").label should be("map[string]")
  }

  it("union reformated spaces") {
    val dt = InternalDatatype("union[unit,  string]")
    dt.label should be("union[unit, string]")
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
