package lib

import play.api.test.Helpers._
import org.scalatest.{FunSpec, ShouldMatchers}

class TokenGeneratorSpec extends FunSpec with ShouldMatchers {

  it("generates unique tokens") {
    val tokens = (1 to 100).map { _ => TokenGenerator.generate() }
    tokens.distinct.sorted should be(tokens.sorted)
  }

  it("generates tokens that are long") {
    val token = TokenGenerator.generate()
    token.length >= 80
  }

}
