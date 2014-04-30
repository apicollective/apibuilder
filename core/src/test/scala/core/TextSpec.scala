package core

import org.scalatest.FlatSpec

class TextSpec extends FlatSpec {

  it should "leave small text alone" in {
    assert("foo" === Text.truncate("foo"))
    assert("This is" === Text.truncate("This is"))
  }

  it should "respect limit" in {
    assert("1234567890" === Text.truncate("1234567890", 10))
    assert("1234567..." === Text.truncate("12345678900", 10))
    assert("This is a long sentence" === Text.truncate("This is a long sentence", 50))
    assert("This is..." === Text.truncate("This is a long sentence", 10))
  }

}
