package lib

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class TokenGeneratorSpec extends PlaySpec with GuiceOneAppPerSuite {

  "generates unique tokens" in {
    val tokens = (1 to 100).map { _ => TokenGenerator.generate() }
    tokens.distinct.sorted must be(tokens.sorted)
  }

  "generates tokens that are long" in {
    val token = TokenGenerator.generate()
    token.length >= 80
  }

}
