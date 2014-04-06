package lib

import org.scalatest.FlatSpec
import org.junit.Assert._

class TextSpec extends FlatSpec {

  it should "leave small text alone" in {
    assertEquals("foo", Text.truncate("foo"))
    assertEquals("This is", Text.truncate("This is"))
  }

  it should "respect limit" in {
    assertEquals("1234567890", Text.truncate("1234567890", 10))
    assertEquals("1234567...", Text.truncate("12345678900", 10))
    assertEquals("This is a long sentence", Text.truncate("This is a long sentence", 50))
    assertEquals("This is...", Text.truncate("This is a long sentence", 10))
  }

}
