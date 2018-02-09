package lib

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

class TokenGeneratorSpec extends PlaySpec with OneAppPerSuite {

  it("generates unique tokens") {
    val tokens = (1 to 100).map { _ => TokenGenerator.generate() }
    tokens.distinct.sorted must be(tokens.sorted)
  }

  it("generates tokens that are long") {
    val token = TokenGenerator.generate()
    token.length >= 80
  }

}
