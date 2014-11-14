package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class InternalServiceDescriptionSpec extends FunSpec with Matchers {

  describe("InternalParsedDatatype") {

    it("Parses a string") {
      val dt = InternalParsedDatatype("string")
      dt.name should be("string")
      dt.container should be(Container.Singleton)
    }

    it("Parses an array") {
      val dt = InternalParsedDatatype("[string]")
      dt.name should be("string")
      dt.container should be(Container.List)
    }

    it("Parses a map of strings") {
      val dt = InternalParsedDatatype("map[string]")
      dt.name should be("string")
      dt.container should be(Container.Map)
    }

    it("Parses a map of Uuid") {
      val dt = InternalParsedDatatype("map[uuid]")
      dt.name should be("uuid")
      dt.container should be(Container.Map)
    }

    it("handles malformed input") {
      val dt = InternalParsedDatatype("[")
      dt.name should be("[")
      dt.container should be(Container.Singleton)

      val dt2 = InternalParsedDatatype("]")
      dt2.name should be("]")
      dt2.container should be(Container.Singleton)

      // Questionable how best to handle this. For now we allow empty
      // string - will get caught downstream when validating that the
      // name of the datatype is a valid name
      val dt3 = InternalParsedDatatype("[]")
      dt3.name should be("")
      dt3.container should be(Container.List)
    }

  }

}
