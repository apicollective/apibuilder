package core

import com.gilt.apidocgenerator.models.{Container, Type, TypeKind, TypeInstance}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class InternalParsedDatatypeSpec extends FunSpec with Matchers {

  it("string") {
    val dt = InternalParsedDatatype("string")
    dt.name should be("string")
    dt.container should be(Container.Singleton)
  }

  it("uuid") {
    val dt = InternalParsedDatatype("uuid")
    dt.name should be("uuid")
    dt.container should be(Container.Singleton)
  }

  it("list") {
    val dt = InternalParsedDatatype("[string]")
    dt.name should be("string")
    dt.container should be(Container.List)
  }

  it("list[uuid]") {
    val dt = InternalParsedDatatype("[uuid]")
    dt.name should be("uuid")
    dt.container should be(Container.List)
  }

  it("map") {
    val dt = InternalParsedDatatype("map[string]")
    dt.name should be("string")
    dt.container should be(Container.Map)
  }

  it("map defaults to string type") {
    val dt = InternalParsedDatatype("map")
    dt.name should be("string")
    dt.container should be(Container.Map)
  }

  it("map[uuid]") {
    val dt = InternalParsedDatatype("map[uuid]")
    dt.name should be("uuid")
    dt.container should be(Container.Map)
  }

}
