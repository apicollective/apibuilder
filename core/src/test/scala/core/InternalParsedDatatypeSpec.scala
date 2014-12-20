package core

import com.gilt.apidocgenerator.models.{Container, ParsedDatatype, Type, TypeKind}
import org.scalatest.{FunSpec, Matchers}

class InternalParsedDatatypeSpec extends FunSpec with Matchers {

  it("string") {
    val dt = InternalParsedDatatype("string")
    dt.label should be("string")
  }

  it("uuid") {
    val dt = InternalParsedDatatype("uuid")
    dt.label should be("uuid")
  }

  it("list") {
    val dt = InternalParsedDatatype("[string]")
    dt.label should be("string")
  }

  it("list[uuid]") {
    val dt = InternalParsedDatatype("[uuid]")
    dt.label should be("uuid")
  }

  it("map") {
    val dt = InternalParsedDatatype("map[string]")
    dt.label should be("string")
  }

  it("map defaults to string type") {
    val dt = InternalParsedDatatype("map")
    dt.label should be("string")
  }

  it("map[uuid]") {
    val dt = InternalParsedDatatype("map[uuid]")
    dt.label should be("uuid")
  }

  it("option") {
    val dt = InternalParsedDatatype("option[string]")
    dt.label should be("option[string]")
  }

  it("union") {
    val dt = InternalParsedDatatype("union[unit, string]")
    dt.label should be("union[unit, string]")
  }

  it("union reformated spaces") {
    val dt = InternalParsedDatatype("union[unit,  string]")
    dt.label should be("union[unit, string]")
  }

  it("handles malformed input") {
    InternalParsedDatatype("[").label should be("[")

    InternalParsedDatatype("]").label should be("]")

    // Questionable how best to handle this. For now we allow empty
    // string - will get caught downstream when validating that the
    // name of the datatype is a valid name
    val dt = InternalParsedDatatype("[]")
    dt.label should be("")
    dt should be(InternalParsedDatatype.List(""))
  }

}
