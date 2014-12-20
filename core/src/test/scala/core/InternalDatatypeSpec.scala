package core

import com.gilt.apidocgenerator.models.{Container, Datatype, Type, TypeKind}
import org.scalatest.{FunSpec, Matchers}

class InternalDatatypeSpec extends FunSpec with Matchers {

  it("string") {
    val dt = InternalDatatype("string")
    dt.label should be("string")
  }

  it("uuid") {
    val dt = InternalDatatype("uuid")
    dt.label should be("uuid")
  }

  it("list") {
    val dt = InternalDatatype("[string]")
    dt.label should be("string")
  }

  it("list[uuid]") {
    val dt = InternalDatatype("[uuid]")
    dt.label should be("uuid")
  }

  it("map") {
    val dt = InternalDatatype("map[string]")
    dt.label should be("string")
  }

  it("map defaults to string type") {
    val dt = InternalDatatype("map")
    dt.label should be("string")
  }

  it("map[uuid]") {
    val dt = InternalDatatype("map[uuid]")
    dt.label should be("uuid")
  }

  it("option") {
    val dt = InternalDatatype("option[string]")
    dt.label should be("option[string]")
  }

  it("union") {
    val dt = InternalDatatype("union[unit, string]")
    dt.label should be("union[unit, string]")
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
    val dt = InternalDatatype("[]")
    dt.label should be("")
    dt should be(InternalDatatype.List(""))
  }

}
