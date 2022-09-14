package lib

import helpers.ServiceConfigurationHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ServiceConfigurationSpec extends AnyFunSpec with Matchers
  with ServiceConfigurationHelpers
{

  it("applicationNamespace") {
    val config = makeServiceConfiguration("me.apidoc")
    config.applicationNamespace("api") should be("me.apidoc.api.v1")
    config.applicationNamespace("spec") should be("me.apidoc.spec.v1")
    config.applicationNamespace("fooBar") should be("me.apidoc.foo.bar.v1")
    config.applicationNamespace("foo-bar") should be("me.apidoc.foo.bar.v1")
    config.applicationNamespace("foo_bar") should be("me.apidoc.foo.bar.v1")
    config.applicationNamespace("Foo.bar") should be("me.apidoc.foo.bar.v1")
    config.applicationNamespace("fooBarBaz") should be("me.apidoc.foo.bar.baz.v1")
  }

  it("applicationNamespace is in lower case") {
    val config = makeServiceConfiguration("ME.APIDOC")
    config.applicationNamespace("API") should be("ME.APIDOC.api.v1")
  }

  it("applicationNamespace is trimmed") {
    val config = makeServiceConfiguration("me.apidoc")
    config.applicationNamespace("  api  ") should be("me.apidoc.api.v1")
  }

  it("applicationNamespace with numbers") {
    val config = makeServiceConfiguration("io.apibuilder")
    config.applicationNamespace("mercury-3pl") should be("io.apibuilder.mercury3pl.v1")
  }

}
