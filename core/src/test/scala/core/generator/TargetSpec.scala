package core.generator

import org.scalatest._

class TargetSpec extends FunSpec with Matchers with PartialFunctionValues {

  describe("generator") {
    it("should be defined for all implemented values") {
      Target.values.foreach { v =>
        if (Target.status(v) != "Proposal") {
          Target.generator valueAt v
        }
      }
    }
  }

  describe("description") {
    it("should be defined for all values") {
      Target.values.foreach { v =>
        Target.description valueAt v
      }
    }
  }

  describe("status") {
    it("should be defined for all values") {
      Target.values.foreach { v =>
        List("Alpha", "Proposal") should contain(Target.status valueAt v)
      }
    }
  }

  describe("transform") {
    it("should be well defined") {
      Target.values.foreach { v =>
        Target.mechanize(v) should equal(v)
        Target.humanize(v) should not equal(v)
        Target.mechanize(Target.humanize(v)) should equal(v)
        Target.humanize(Target.mechanize(Target.humanize(v))) should equal(Target.humanize(v))
      }
    }
  }
}
