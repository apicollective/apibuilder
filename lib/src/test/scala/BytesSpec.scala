package lib

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class BytesSpec extends AnyFunSpec with Matchers {

  describe("label") {

    val OneKb = 1024

    it("bytes") {
      Bytes.label(0) should be("0 bytes")
      Bytes.label(1) should be("1 byte")
      Bytes.label(2) should be("2 bytes")
      Bytes.label(1023) should be("1023 bytes")
    }

    it("kb") {
      Bytes.label(OneKb) should be("1 kb")
      Bytes.label(OneKb + 1) should be("1 kb")
      Bytes.label(OneKb + (OneKb / 10 + 1)) should be("1.1 kb")
      Bytes.label(OneKb + (OneKb / 2)) should be("1.5 kb")
      Bytes.label(OneKb * OneKb - 1) should be("1 mb")
    }

    it("mb") {
      val OneMb = 1024*1024
      Bytes.label(OneMb) should be("1 mb")
      Bytes.label(OneMb + 1) should be("1 mb")
      Bytes.label(OneMb + (OneMb / 10 + 1)) should be("1.1 mb")
      Bytes.label(OneMb + (OneMb / 2)) should be("1.5 mb")
      Bytes.label(OneMb * OneKb - 1) should be("1 gb")
    }

    it("gb") {
      val OneGb = 1024*1024*1024
      Bytes.label(OneGb) should be("1 gb")
      Bytes.label(OneGb + 1) should be("1 gb")
      Bytes.label(OneGb + (OneGb / 10 + 1)) should be("1.1 gb")
      Bytes.label(OneGb + (OneGb / 2)) should be("1.5 gb")
    }

  }
}
