package lib

import org.scalatest.FlatSpec
import org.junit.Assert._

class UrlKeySpec extends FlatSpec {

  behavior of "Url Key Generator"

  it should "leave good urls alone" in {
    assertEquals("foo", UrlKey.generate("foo"))
    assertEquals("foo-bar", UrlKey.generate("foo-bar"))
  }

  it should "leave numbers alone" in {
    assertEquals("foo123", UrlKey.generate("foo123"))
  }

  it should "lower case" in {
    assertEquals("foo-bar", UrlKey.generate("FOO-BAR"))
  }

  it should "trim" in {
    assertEquals("foo-bar", UrlKey.generate("  FOO-BAR  "))
  }

}
