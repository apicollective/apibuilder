package lib

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ReviewSpec extends AnyFunSpec with Matchers {

  it("fromString") {
    Review.fromString(Review.Accept.key) should be(Some(Review.Accept))
    Review.fromString(Review.Accept.key.toUpperCase) should be(Some(Review.Accept))
    Review.fromString(Review.Accept.key.toLowerCase) should be(Some(Review.Accept))

    Review.fromString(Review.Decline.key) should be(Some(Review.Decline))
    Review.fromString(Review.Decline.key.toUpperCase) should be(Some(Review.Decline))
    Review.fromString(Review.Decline.key.toLowerCase) should be(Some(Review.Decline))

    Review.fromString("other") should be(None)
  }

}
