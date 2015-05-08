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

    describe("deprecation") {

      it("add") {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(deprecation = Some(Deprecation()))))
        ServiceDiff(serviceWithHeader, serviceWithHeader2).differences should be(
          Seq(
            Difference.NonBreaking("header x-test deprecated")
          )
        )
      }

      it("add with description") {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(deprecation = Some(Deprecation(description = Some("test"))))))
        ServiceDiff(serviceWithHeader, serviceWithHeader2).differences should be(
          Seq(
            Difference.NonBreaking("header x-test deprecated: test")
          )
        )
      }

      it("remove") {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(deprecation = Some(Deprecation()))))
        ServiceDiff(serviceWithHeader2, serviceWithHeader).differences should be(
          Seq(
            Difference.NonBreaking("header x-test removed: deprecation")
          )
        )
      }
    }

  }

  describe("import") {

    val imp = Import(
      uri = "http://www.apidoc.me/gilt/apidoc-spec/0.9.6/service.json",
      namespace = "com.gilt.apidoc.spec.v0",
      organization = Organization(key = "gilt"),
      application = Application(key = "apidoc-spec"),
      version = "0.9.6",
      enums = Seq("method", "parameter_location", "response_code_option"),
      unions = Seq("response_code"),
      models = Seq("apidoc", "application")
    )

    val base = service.copy(imports = Nil)
    val serviceWithImport = base.copy(imports = Seq(imp))

    it("no change") {
      ServiceDiff(serviceWithImport, serviceWithImport).differences should be(Nil)
    }

    it("remove import") {
      ServiceDiff(serviceWithImport, base).differences should be(
        Seq(
          Difference.NonBreaking("import removed: http://www.apidoc.me/gilt/apidoc-spec/0.9.6/service.json")
        )
      )
    }

    it("add import") {
      ServiceDiff(base, serviceWithImport).differences should be(
        Seq(
          Difference.NonBreaking("import added: http://www.apidoc.me/gilt/apidoc-spec/0.9.6/service.json")
        )
      )
    }

    it("change import") {
      val imp2 = Import(
        uri = "http://www.apidoc.me/gilt/apidoc-spec/0.9.6/service.json",
        namespace = "com.gilt.apidoc.spec.v1",
        organization = Organization(key = "gilt2"),
        application = Application(key = "apidoc-spec2"),
        version = "1.0.0",
        enums = Seq("foo"),
        unions = Seq("bar"),
        models = Seq("baz")
      )

      val prefix = "import http://www.apidoc.me/gilt/apidoc-spec/0.9.6/service.json"

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(namespace = imp2.namespace)))).differences should be(
        Seq(
          Difference.NonBreaking(s"$prefix namespace changed from com.gilt.apidoc.spec.v0 to com.gilt.apidoc.spec.v1")
        )
      )

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(organization = imp2.organization)))).differences should be(
        Seq(
          Difference.NonBreaking(s"$prefix organization/key changed from gilt to gilt2")
        )
      )

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(application = imp2.application)))).differences should be(
        Seq(
          Difference.NonBreaking(s"$prefix application/key changed from apidoc-spec to apidoc-spec2")
        )
      )

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(version = imp2.version)))).differences should be(
        Seq(
          Difference.NonBreaking(s"$prefix version changed from 0.9.6 to 1.0.0")
        )
      )

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(enums = imp2.enums)))).differences should be(
        Seq(
          Difference.NonBreaking(s"$prefix enums changed from [method, parameter_location, response_code_option] to [foo]")
        )
      )

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(unions = imp2.unions)))).differences should be(
        Seq(
          Difference.NonBreaking(s"$prefix unions changed from [response_code] to [bar]")
        )
      )

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(models = imp2.models)))).differences should be(
        Seq(
          Difference.NonBreaking(s"$prefix models changed from [apidoc, application] to [baz]")
        )
      )

    }

  }

  describe("enum") {

    val value = EnumValue(
      name = "18-25",
      description = None,
      deprecation = None
    )

    val enum = Enum(
      name = "age_group",
      plural = "age_groups",
      description = None,
      deprecation = None,
      values = Seq(value)
    )

    val base = service.copy(enums = Nil)
    val serviceWithEnum = base.copy(enums = Seq(enum))

    it("no change") {
      ServiceDiff(serviceWithEnum, serviceWithEnum).differences should be(Nil)
    }

    it("add enum") {
      ServiceDiff(base, serviceWithEnum).differences should be(
        Seq(
          Difference.NonBreaking("enum added: age_group")
        )
      )
    }

    it("remove enum") {
      ServiceDiff(serviceWithEnum, base).differences should be(
        Seq(
          Difference.Breaking("enum removed: age_group")
        )
      )
    }

    it("change enum") {
      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(enum.copy(plural = "groups")))).differences should be(
        Seq(
          Difference.NonBreaking("enum age_group plural changed from age_groups to groups")
        )
      )

      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(enum.copy(description = Some("test"))))).differences should be(
        Seq(
          Difference.NonBreaking("enum age_group description added: test")
        )
      )

      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(enum.copy(deprecation = Some(Deprecation()))))).differences should be(
        Seq(
          Difference.NonBreaking("enum age_group deprecated")
        )
      )

      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(enum.copy(values = Nil)))).differences should be(
        Seq(
          Difference.Breaking("enum age_group value removed: 18-25")
        )
      )

      val value2 = value.copy(name = "26-35")
      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(enum.copy(values = Seq(value, value2))))).differences should be(
        Seq(
          Difference.NonBreaking("enum age_group value added: 26-35")
        )
      )
    }

    it("change enumValues") {
      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(enum.copy(values = Seq(value.copy(description = Some("test"))))))).differences should be(
        Seq(
          Difference.NonBreaking("enum age_group value 18-25 description added: test")
        )
      )

      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(enum.copy(values = Seq(value.copy(deprecation = Some(Deprecation()))))))).differences should be(
        Seq(
          Difference.NonBreaking("enum age_group value 18-25 deprecated")
        )
      )
    }

  }

  describe("union") {

    val unionType = UnionType(
      `type` = "registered",
      description = None,
      deprecation = None
    )

    val union = Union(
      name = "user",
      plural = "users",
      description = None,
      deprecation = None,
      types = Seq(unionType)
    )

    val base = service.copy(unions = Nil)
    val serviceWithUnion = base.copy(unions = Seq(union))

    it("no change") {
      ServiceDiff(serviceWithUnion, serviceWithUnion).differences should be(Nil)
    }

    it("add union") {
      ServiceDiff(base, serviceWithUnion).differences should be(
        Seq(
          Difference.NonBreaking("union added: user")
        )
      )
    }

    it("remove union") {
      ServiceDiff(serviceWithUnion, base).differences should be(
        Seq(
          Difference.Breaking("union removed: user")
        )
      )
    }

    it("change union") {
      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(plural = "all_users")))).differences should be(
        Seq(
          Difference.NonBreaking("union user plural changed from users to all_users")
        )
      )

      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(description = Some("test"))))).differences should be(
        Seq(
          Difference.NonBreaking("union user description added: test")
        )
      )

      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(deprecation = Some(Deprecation()))))).differences should be(
        Seq(
          Difference.NonBreaking("union user deprecated")
        )
      )

      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(types = Nil)))).differences should be(
        Seq(
          Difference.Breaking("union user type removed: registered")
        )
      )

      val unionType2 = unionType.copy(`type` = "guest")
      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(types = Seq(unionType, unionType2))))).differences should be(
        Seq(
          Difference.NonBreaking("union user type added: guest")
        )
      )
    }

    it("change unionTypes") {
      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(types = Seq(unionType.copy(description = Some("test"))))))).differences should be(
        Seq(
          Difference.NonBreaking("union user type registered description added: test")
        )
      )

      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(types = Seq(unionType.copy(deprecation = Some(Deprecation()))))))).differences should be(
        Seq(
          Difference.NonBreaking("union user type registered deprecated")
        )
      )
    }

  }

}
