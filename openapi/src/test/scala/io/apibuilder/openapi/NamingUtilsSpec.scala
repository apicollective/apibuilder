package io.apibuilder.openapi

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class NamingUtilsSpec extends AnyWordSpec with Matchers {

  import NamingUtils._

  "toSnakeCase" must {
    "convert camelCase" in {
      toSnakeCase("FooBar") must be("foo_bar")
    }
    "convert with hyphens" in {
      toSnakeCase("foo-bar") must be("foo_bar")
    }
    "convert with dots" in {
      toSnakeCase("foo.bar") must be("foo_bar")
    }
    "handle acronyms" in {
      toSnakeCase("HTTPSConnection") must be("https_connection")
    }
  }

  "uniqueSnakeCase" must {
    val uniqueConfig = NamingConfig(uniqueNames = true)
    val normalConfig = NamingConfig(uniqueNames = false)

    "convert to snake_case with consistent hash suffix" in {
      val a1 = uniqueSnakeCase("FooBar", uniqueConfig)
      val a2 = uniqueSnakeCase("FooBar", uniqueConfig)
      val a3 = uniqueSnakeCase("foo_bar", uniqueConfig)

      a1 must be(a2)
      a1 must be(a3)
      a1 must startWith("foo_bar_")
    }

    "correctly handle arrays" in {
      uniqueSnakeCase("[foo_bar]", uniqueConfig) must be("[foo_bar_vouy]")
      uniqueSnakeCase("[BazQux]", uniqueConfig) must be("[baz_qux_bykv]")
      uniqueSnakeCase("[ BazQux ]", uniqueConfig) must be("[baz_qux_bykv]")
    }

    "do not hash primitive types" in {
      uniqueSnakeCase("string", uniqueConfig) must be("string")
      uniqueSnakeCase("[boolean]", uniqueConfig) must be("[boolean]")
    }

    "just convert to snake_case when uniqueNames is false" in {
      uniqueSnakeCase("FooBar", normalConfig) must be("foo_bar")
    }
  }

  "sanitizeEnumName" must {
    "remove quotes" in {
      sanitizeEnumName("USPS_DELIVERING\"") must be("USPS_DELIVERING")
    }
    "replace spaces with underscores" in {
      sanitizeEnumName("foo bar") must be("foo_bar")
    }
    "trim whitespace" in {
      sanitizeEnumName("  foo  ") must be("foo")
    }
  }
}
