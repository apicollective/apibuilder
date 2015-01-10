package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

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
    config.applicationNamespace("api") should be("me.apidoc.api")
    config.applicationNamespace("spec") should be("me.apidoc.spec")
    config.applicationNamespace("fooBar") should be("me.apidoc.foo.bar")
    config.applicationNamespace("foo-bar") should be("me.apidoc.foo.bar")
    config.applicationNamespace("foo_bar") should be("me.apidoc.foo.bar")
    config.applicationNamespace("Foo.Bar") should be("me.apidoc.foo.bar")
    config.applicationNamespace("fooBarBaz") should be("me.apidoc.foo.bar.baz")
  }

  it("applicationNamespace is in lower case") {
    val config = createServiceConfiguration("ME.APIDOC")
    config.applicationNamespace("API") should be("ME.APIDOC.api")
  }

  it("applicationNamespace is trimmed") {
    val config = createServiceConfiguration("me.apidoc")
    config.applicationNamespace("  api  ") should be("me.apidoc.api")
  }

}
