package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class InternalServiceDescriptionSpec extends FunSpec with Matchers {

  describe("InternalParsedDatatype") {

    it("Parses a string") {
      val dt = InternalParsedDatatype("string")
      dt.name should be("string")
      dt.multiple should be(false)
      dt.referencedModelName should be(None)
    }

    it("Parses an array") {
      val dt = InternalParsedDatatype("[string]")
      dt.name should be("string")
      dt.multiple should be(true)
      dt.referencedModelName should be(None)
    }

    it("handles malformed input") {
      val dt = InternalParsedDatatype("[")
      dt.name should be("[")
      dt.multiple should be(false)
      dt.referencedModelName should be(None)

      val dt2 = InternalParsedDatatype("]")
      dt2.name should be("]")
      dt2.multiple should be(false)

      // Questionable how best to handle this. For now we allow empty
      // string - will get caught downstream when validating that the
      // name of the datatype is a valid name
      val dt3 = InternalParsedDatatype("[]")
      dt3.name should be("")
      dt3.multiple should be(true)
    }

    it("parses a reference") {
      val dt = InternalParsedDatatype("reference[user]")
      dt.name should be("reference")
      dt.multiple should be(false)
      dt.referencedModelName should be(Some("user"))

      val dt2 = InternalParsedDatatype("[reference[user]]")
      dt2.name should be("reference")
      dt2.multiple should be(true)
      dt2.referencedModelName should be(Some("user"))
    }

  }

}
