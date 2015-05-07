package lib

import com.gilt.apidoc.spec.v0.models._
import org.scalatest.{FunSpec, ShouldMatchers}

class ServiceDiffSpec extends FunSpec with ShouldMatchers {

  new play.core.StaticApplication(new java.io.File("."))

  private lazy val service = TestHelper.readService("../spec/api.json")

  it("no changes") {
    ServiceDiff(service, service).differences should be(Nil)
  }

  it("apidoc version") {
    ServiceDiff(service, service.copy(apidoc = Apidoc(version = "0.0.1"))).differences should be(
      Seq(
        Difference.NonBreaking(s"apidoc/version changed from ${service.apidoc.version} to 0.0.1")
      )
    )
  }

  it("name") {
    ServiceDiff(service, service.copy(name = "test")).differences should be(
      Seq(
        Difference.NonBreaking(s"name changed from ${service.name} to test")
      )
    )
  }

  it("organization key") {
    ServiceDiff(service, service.copy(organization = Organization(key = "foo"))).differences should be(
      Seq(
        Difference.NonBreaking(s"organization/key changed from ${service.organization.key} to foo")
      )
    )
  }

  it("application key") {
    ServiceDiff(service, service.copy(application = Application(key = "foo"))).differences should be(
      Seq(
        Difference.NonBreaking(s"application/key changed from ${service.application.key} to foo")
      )
    )
  }

  it("namespace") {
    ServiceDiff(service, service.copy(namespace = "test")).differences should be(
      Seq(
        Difference.Breaking(s"namespace changed from ${service.namespace} to test")
      )
    )
  }

  it("version") {
    ServiceDiff(service, service.copy(version = "0.0.1")).differences should be(
      Seq(
        Difference.NonBreaking(s"version changed from ${service.version} to 0.0.1")
      )
    )
  }

  it("baseUrl") {
    val base = service.copy(baseUrl = None)

    ServiceDiff(base, base.copy(baseUrl = None)).differences should be(Nil)

    ServiceDiff(base, base.copy(baseUrl = Some("http://foo.com"))).differences should be(
      Seq(
        Difference.NonBreaking(s"base_url added: http://foo.com")
      )
    )

    ServiceDiff(base.copy(baseUrl = Some("http://foo.com")), base).differences should be(
      Seq(
        Difference.NonBreaking(s"base_url removed: http://foo.com")
      )
    )

    ServiceDiff(
      base.copy(baseUrl = Some("http://foo.com")),
      base.copy(baseUrl = Some("http://foobar.com"))
    ).differences should be(
      Seq(
        Difference.NonBreaking(s"base_url changed from http://foo.com to http://foobar.com")
      )
    )
  }

  it("description") {
    val base = service.copy(description = None)

    ServiceDiff(base, base.copy(description = None)).differences should be(Nil)

    ServiceDiff(base, base.copy(description = Some("foo"))).differences should be(
      Seq(
        Difference.NonBreaking(s"description added: foo")
      )
    )

    ServiceDiff(base.copy(description = Some("foo")), base).differences should be(
      Seq(
        Difference.NonBreaking(s"description removed: foo")
      )
    )

    ServiceDiff(
      base.copy(description = Some("foo")),
      base.copy(description = Some("foobar"))
    ).differences should be(
      Seq(
        Difference.NonBreaking(s"description changed from foo to foobar")
      )
    )
  }

  describe("headers") {
    val header = Header(
      name = "x-test",
      `type` = "string",
      description = None,
      deprecation = None,
      required = false,
      default = None
    )

    val base = service.copy(headers = Nil)
    val serviceWithHeader = base.copy(headers = Seq(header))

    it("no change") {
      ServiceDiff(serviceWithHeader, serviceWithHeader).differences should be(Nil)
    }

    it("remove header") {
      ServiceDiff(serviceWithHeader, base).differences should be(
        Seq(
          Difference.NonBreaking("header removed: x-test")
        )
      )
    }

    it("add optional header") {
      ServiceDiff(base, serviceWithHeader).differences should be(
        Seq(
          Difference.NonBreaking("optional header added: x-test")
        )
      )
    }

    it("add required header") {
      val serviceWithRequiredHeader = base.copy(headers = Seq(header.copy(required = true)))
      ServiceDiff(base, serviceWithRequiredHeader).differences should be(
        Seq(
          Difference.Breaking("required header added: x-test")
        )
      )
    }

    it("type") {
      val serviceWithHeader2 = base.copy(headers = Seq(header.copy(`type` = "long")))
      ServiceDiff(serviceWithHeader, serviceWithHeader2).differences should be(
        Seq(
          Difference.Breaking("header x-test type changed from string to long")
        )
      )
    }

    describe("description") {

      it("add") {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(description = Some("foo"))))
        ServiceDiff(serviceWithHeader, serviceWithHeader2).differences should be(
          Seq(
            Difference.NonBreaking("header x-test description added: foo")
          )
        )
      }

      it("remove") {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(description = Some("foo"))))
        ServiceDiff(serviceWithHeader2, serviceWithHeader).differences should be(
          Seq(
            Difference.NonBreaking("header x-test description removed: foo")
          )
        )
      }

      it("change") {
        val serviceWithHeader1 = base.copy(headers = Seq(header.copy(description = Some("foo"))))
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(description = Some("bar"))))
        ServiceDiff(serviceWithHeader1, serviceWithHeader2).differences should be(
          Seq(
            Difference.NonBreaking("header x-test description changed from foo to bar")
          )
        )
      }

    }
  }
}
