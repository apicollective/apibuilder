package util

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class SessionIdGeneratorSpec extends PlaySpec with GuiceOneAppPerSuite {

  "starts with prefix" in {
    SessionIdGenerator.generate().startsWith("A51") must be (true)
  }

  "64 characters long" in {
    SessionIdGenerator.generate().length must be(64)
  }

  "generates unique identifiers" in {
    val s = collection.mutable.Set[String]()

    1.to(100000).foreach { _ =>
      val tn = SessionIdGenerator.generate()
      s(tn) must be (false)
      s += tn
    }
  }

}
