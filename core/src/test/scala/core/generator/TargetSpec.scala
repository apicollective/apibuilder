package core.generator

import org.scalatest._

class TargetSpec extends FunSpec with Matchers {
  describe("generator") {
    it("should succeed on all values") {
      Target.values.foreach { v =>
        Target.generator(v) should be ('success)
      }
    }
  }
}
