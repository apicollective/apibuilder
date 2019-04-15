package lib

import org.scalatest.{FunSpec, Matchers}

class ServiceConfigurationSpec extends FunSpec with Matchers {

  def createServiceConfiguration(orgNamespace: String) = {
    ServiceConfiguration(
      orgKey = "apidoc",
      orgNamespace = orgNamespace,
      version = "1.0"
    )
  }

  it("applicationNamespace") {
    val config = createServiceConfiguration("me.apidoc")
    config.applicationNamespace("api") should be("me.apidoc.api.v1")
    config.applicationNamespace("spec") should be("me.apidoc.spec.v1")
    config.applicationNamespace("fooBar") should be("me.apidoc.foo.bar.v1")
    config.applicationNamespace("foo-bar") should be("me.apidoc.foo.bar.v1")
    config.applicationNamespace("foo_bar") should be("me.apidoc.foo.bar.v1")
    config.applicationNamespace("Foo.bar") should be("me.apidoc.foo.bar.v1")
    config.applicationNamespace("fooBarBaz") should be("me.apidoc.foo.bar.baz.v1")
  }

  it("applicationNamespace is in lower case") {
    val config = createServiceConfiguration("ME.APIDOC")
    config.applicationNamespace("API") should be("ME.APIDOC.api.v1")
  }

  it("applicationNamespace is trimmed") {
    val config = createServiceConfiguration("me.apidoc")
    config.applicationNamespace("  api  ") should be("me.apidoc.api.v1")
  }

  it("applicationNamespace with numbers") {
    val config = createServiceConfiguration("io.apibuilder")
    config.applicationNamespace("mercury-3pl") should be("io.apibuilder.mercury3pl.v1")
  }

  it("dev - reproduce apibuilder/issues/792") {
    // TODO #792 - the below example demonstrates that orgNamespace is assumed to be appFullNamespace 😱😱😱 => hence the bug
    val appFullNamespace = "me.apidoc.my.api.v1"
    createServiceConfiguration(orgNamespace = appFullNamespace).applicationNamespace("my-api") should be("me.apidoc.my.api.v1.my.api.v1") // 💥
  }

}
