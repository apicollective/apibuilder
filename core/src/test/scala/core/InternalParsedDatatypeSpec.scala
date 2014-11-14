package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class InternalParsedDatatypeSpec extends FunSpec with Matchers {

  it("string") {
    val dt = InternalParsedDatatype("string")
    dt.name should be("string")
    dt.container should be(TypeContainer.Singleton)
  }

  it("uuid") {
    val dt = InternalParsedDatatype("uuid")
    dt.name should be("uuid")
    dt.container should be(TypeContainer.Singleton)
  }

  it("list") {
    val dt = InternalParsedDatatype("[string]")
    dt.name should be("string")
    dt.container should be(TypeContainer.List)
  }

  it("list[uuid]") {
    val dt = InternalParsedDatatype("[uuid]")
    dt.name should be("uuid")
    dt.container should be(TypeContainer.List)
  }

  it("map") {
    val dt = InternalParsedDatatype("map[string]")
    dt.name should be("string")
    dt.container should be(TypeContainer.Map)
  }

  it("map defaults to string type") {
    val dt = InternalParsedDatatype("map")
    dt.name should be("string")
    dt.container should be(TypeContainer.Map)
  }

  it("map[uuid]") {
    val dt = InternalParsedDatatype("map[uuid]")
    dt.name should be("uuid")
    dt.container should be(TypeContainer.Map)
  }

}
