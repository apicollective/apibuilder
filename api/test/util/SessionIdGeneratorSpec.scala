package util

import org.scalatest.{FunSpec, Matchers}

class SessionIdGeneratorSpec extends FunSpec with Matchers {

  it("starts with prefix") {
    SessionIdGenerator.generate().startsWith("A51") should be (true)
  }

  it("64 characters long") {
    SessionIdGenerator.generate().length should be(64)
  }

  it("generates unique identifiers") {
    val s = collection.mutable.Set[String]()

    1.to(100000).foreach { _ =>
      val tn = SessionIdGenerator.generate()
      s(tn) should be (false)
      s += tn
    }
  }

}
