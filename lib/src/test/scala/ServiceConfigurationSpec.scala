package lib

import helpers.ServiceConfigurationHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ServiceConfigurationSpec extends AnyFunSpec with Matchers
  with ServiceConfigurationHelpers
{

  it("applicationNamespace") {
    val config = makeServiceConfiguration("me.apidoc")
    config.applicationNamespace("api") should be("me.apidoc.api")
    config.applicationNamespace("spec") should be("me.apidoc.spec")
    config.applicationNamespace("fooBar") should be("me.apidoc.foo.bar")
    config.applicationNamespace("foo-bar") should be("me.apidoc.foo.bar")
    config.applicationNamespace("foo_bar") should be("me.apidoc.foo.bar")
    config.applicationNamespace("Foo.bar") should be("me.apidoc.foo.bar")
    config.applicationNamespace("fooBarBaz") should be("me.apidoc.foo.bar.baz")
  }

  it("applicationNamespace is in lower case") {
    val config = makeServiceConfiguration("ME.APIDOC")
    config.applicationNamespace("API") should be("ME.APIDOC.api")
  }

  it("applicationNamespace is trimmed") {
    val config = makeServiceConfiguration("me.apidoc")
    config.applicationNamespace("  api  ") should be("me.apidoc.api")
  }

  it("applicationNamespace with numbers") {
    val config = makeServiceConfiguration("io.apibuilder")
    config.applicationNamespace("mercury-3pl") should be("io.apibuilder.mercury3pl")
  }

}
