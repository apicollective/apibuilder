package lib

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

class TokenGeneratorSpec extends PlaySpec with OneAppPerSuite {

  "generates unique tokens" in {
    val tokens = (1 to 100).map { _ => TokenGenerator.generate() }
    tokens.distinct.sorted must be(tokens.sorted)
  }

  "generates tokens that are long" in {
    val token = TokenGenerator.generate()
    token.length >= 80
  }

}
