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
    val config = createServiceConfiguration("com.bryzek")
    config.applicationNamespace("mercury-3pl") should be("com.bryzek.mercury3pl.v1")
  }

}
