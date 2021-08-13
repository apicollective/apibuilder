package lib

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class TokenGeneratorSpec extends PlaySpec with GuiceOneAppPerSuite {

  "generates unique tokens" in {
    val tokens = (1 to 100).map { _ => TokenGenerator.generate() }
    tokens.distinct.sorted must be(tokens.sorted)
  }

  "generates tokens that are long" in {
    TokenGenerator.generate().length >= 80 mustBe true
    TokenGenerator.generate(100).length mustBe 100
  }

  "generates tokens that are short" in {
    TokenGenerator.generate(5).length mustBe 5
  }
}
