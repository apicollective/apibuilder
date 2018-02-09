package util

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

class SessionIdGeneratorSpec extends PlaySpec with OneAppPerSuite {

  it("starts with prefix") {
    SessionIdGenerator.generate().startsWith("A51") must be (true)
  }

  it("64 characters long") {
    SessionIdGenerator.generate().length must be(64)
  }

  it("generates unique identifiers") {
    val s = collection.mutable.Set[String]()

    1.to(100000).foreach { _ =>
      val tn = SessionIdGenerator.generate()
      s(tn) must be (false)
      s += tn
    }
  }

}
