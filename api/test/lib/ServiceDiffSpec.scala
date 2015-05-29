package lib

import com.gilt.apidoc.api.v0.models.{Diff, DiffBreaking, DiffNonBreaking}
import com.gilt.apidoc.spec.v0.models._
import org.scalatest.{FunSpec, ShouldMatchers}

class ServiceDiffSpec extends FunSpec with ShouldMatchers {

  new play.core.StaticApplication(new java.io.File("."))

  private lazy val service = TestHelper.readService("../spec/service.json")

  it("no changes") {
    ServiceDiff(service, service).differences should be(Nil)
  }

  it("apidoc version") {
    ServiceDiff(service, service.copy(apidoc = Apidoc(version = "0.0.1"))).differences should be(
      Seq(
        DiffNonBreaking(s"apidoc/version changed from ${service.apidoc.version} to 0.0.1")
      )
    )
  }

  describe("info") {
    lazy val base = service.copy(info = Info(contact = None, license = None))

    it("contact") {
      val contact = Contact(
        name = Some("Mike"),
        url = Some("http://foo.com"),
        email = Some("mbryzek@mailinator.com")
      )

      ServiceDiff(base, base.copy(info = Info(contact = Some(contact)))).differences should be(
        Seq(
          DiffNonBreaking(s"contact/name added: Mike"),
          DiffNonBreaking(s"contact/url added: http://foo.com"),
          DiffNonBreaking(s"contact/email added: mbryzek@mailinator.com")
        )
      )
    }

    it("license") {
      val license = License(
        name = "MIT",
        url = Some("http://opensource.org/licenses/MIT")
      )

      ServiceDiff(base, base.copy(info = Info(license = Some(license)))).differences should be(
        Seq(
          DiffNonBreaking(s"license/name added: MIT"),
          DiffNonBreaking(s"license/url added: http://opensource.org/licenses/MIT")
        )
      )
    }

  }

  it("name") {
    ServiceDiff(service, service.copy(name = "test")).differences should be(
      Seq(
        DiffNonBreaking(s"name changed from ${service.name} to test")
      )
    )
  }

  it("organization key") {
    ServiceDiff(service, service.copy(organization = Organization(key = "foo"))).differences should be(
      Seq(
        DiffNonBreaking(s"organization/key changed from ${service.organization.key} to foo")
      )
    )
  }

  it("application key") {
    ServiceDiff(service, service.copy(application = Application(key = "foo"))).differences should be(
      Seq(
        DiffNonBreaking(s"application/key changed from ${service.application.key} to foo")
      )
    )
  }

  it("namespace") {
    ServiceDiff(service, service.copy(namespace = "test")).differences should be(
      Seq(
        DiffBreaking(s"namespace changed from ${service.namespace} to test")
      )
    )
  }

  it("version") {
    ServiceDiff(service, service.copy(version = "0.0.1")).differences should be(
      Seq(
        DiffNonBreaking(s"version changed from ${service.version} to 0.0.1")
      )
    )
  }

  it("baseUrl") {
    val base = service.copy(baseUrl = None)

    ServiceDiff(base, base.copy(baseUrl = None)).differences should be(Nil)

    ServiceDiff(base, base.copy(baseUrl = Some("http://foo.com"))).differences should be(
      Seq(
        DiffNonBreaking(s"base_url added: http://foo.com")
      )
    )

    ServiceDiff(base.copy(baseUrl = Some("http://foo.com")), base).differences should be(
      Seq(
        DiffNonBreaking(s"base_url removed: http://foo.com")
      )
    )

    ServiceDiff(
      base.copy(baseUrl = Some("http://foo.com")),
      base.copy(baseUrl = Some("http://foobar.com"))
    ).differences should be(
      Seq(
        DiffNonBreaking(s"base_url changed from http://foo.com to http://foobar.com")
      )
    )
  }

  it("description") {
    val base = service.copy(description = None)

    ServiceDiff(base, base.copy(description = None)).differences should be(Nil)

    ServiceDiff(base, base.copy(description = Some("foo"))).differences should be(
      Seq(
        DiffNonBreaking(s"description added: foo")
      )
    )

    ServiceDiff(base.copy(description = Some("foo")), base).differences should be(
      Seq(
        DiffNonBreaking(s"description removed: foo")
      )
    )

    ServiceDiff(
      base.copy(description = Some("foo")),
      base.copy(description = Some("foobar"))
    ).differences should be(
      Seq(
        DiffNonBreaking(s"description changed from foo to foobar")
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
          DiffNonBreaking("header removed: x-test")
        )
      )
    }

    it("add optional header") {
      ServiceDiff(base, serviceWithHeader).differences should be(
        Seq(
          DiffNonBreaking("optional header added: x-test")
        )
      )
    }

    it("add required header") {
      val serviceWithRequiredHeader = base.copy(headers = Seq(header.copy(required = true)))
      ServiceDiff(base, serviceWithRequiredHeader).differences should be(
        Seq(
          DiffBreaking("required header added: x-test")
        )
      )
    }

    it("type") {
      val serviceWithHeader2 = base.copy(headers = Seq(header.copy(`type` = "long")))
      ServiceDiff(serviceWithHeader, serviceWithHeader2).differences should be(
        Seq(
          DiffBreaking("header x-test type changed from string to long")
        )
      )
    }

    describe("description") {

      it("add") {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(description = Some("foo"))))
        ServiceDiff(serviceWithHeader, serviceWithHeader2).differences should be(
          Seq(
            DiffNonBreaking("header x-test description added: foo")
          )
        )
      }

      it("remove") {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(description = Some("foo"))))
        ServiceDiff(serviceWithHeader2, serviceWithHeader).differences should be(
          Seq(
            DiffNonBreaking("header x-test description removed: foo")
          )
        )
      }

      it("change") {
        val serviceWithHeader1 = base.copy(headers = Seq(header.copy(description = Some("foo"))))
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(description = Some("bar"))))
        ServiceDiff(serviceWithHeader1, serviceWithHeader2).differences should be(
          Seq(
            DiffNonBreaking("header x-test description changed from foo to bar")
          )
        )
      }
    }

    describe("deprecation") {

      it("add") {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(deprecation = Some(Deprecation()))))
        ServiceDiff(serviceWithHeader, serviceWithHeader2).differences should be(
          Seq(
            DiffNonBreaking("header x-test deprecated")
          )
        )
      }

      it("add with description") {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(deprecation = Some(Deprecation(description = Some("test"))))))
        ServiceDiff(serviceWithHeader, serviceWithHeader2).differences should be(
          Seq(
            DiffNonBreaking("header x-test deprecated: test")
          )
        )
      }

      it("remove") {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(deprecation = Some(Deprecation()))))
        ServiceDiff(serviceWithHeader2, serviceWithHeader).differences should be(
          Seq(
            DiffNonBreaking("header x-test removed: deprecation")
          )
        )
      }
    }

    describe("default") {

      it("add") {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(default = Some("test"))))
        ServiceDiff(serviceWithHeader, serviceWithHeader2).differences should be(
          Seq(
            DiffNonBreaking("header x-test default added: test")
          )
        )
      }

      it("remove") {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(default = Some("test"))))
        ServiceDiff(serviceWithHeader2, serviceWithHeader).differences should be(
          Seq(
            DiffBreaking("header x-test default removed: test")
          )
        )
      }

      it("change") {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(default = Some("foo"))))
        val serviceWithHeader3 = base.copy(headers = Seq(header.copy(default = Some("bar"))))
        ServiceDiff(serviceWithHeader2, serviceWithHeader3).differences should be(
          Seq(
            DiffNonBreaking("header x-test default changed from foo to bar")
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
          DiffNonBreaking("import removed: http://www.apidoc.me/gilt/apidoc-spec/0.9.6/service.json")
        )
      )
    }

    it("add import") {
      ServiceDiff(base, serviceWithImport).differences should be(
        Seq(
          DiffNonBreaking("import added: http://www.apidoc.me/gilt/apidoc-spec/0.9.6/service.json")
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
          DiffNonBreaking(s"$prefix namespace changed from com.gilt.apidoc.spec.v0 to com.gilt.apidoc.spec.v1")
        )
      )

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(organization = imp2.organization)))).differences should be(
        Seq(
          DiffNonBreaking(s"$prefix organization/key changed from gilt to gilt2")
        )
      )

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(application = imp2.application)))).differences should be(
        Seq(
          DiffNonBreaking(s"$prefix application/key changed from apidoc-spec to apidoc-spec2")
        )
      )

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(version = imp2.version)))).differences should be(
        Seq(
          DiffNonBreaking(s"$prefix version changed from 0.9.6 to 1.0.0")
        )
      )

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(enums = imp2.enums)))).differences should be(
        Seq(
          DiffNonBreaking(s"$prefix enums changed from [method, parameter_location, response_code_option] to [foo]")
        )
      )

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(unions = imp2.unions)))).differences should be(
        Seq(
          DiffNonBreaking(s"$prefix unions changed from [response_code] to [bar]")
        )
      )

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(models = imp2.models)))).differences should be(
        Seq(
          DiffNonBreaking(s"$prefix models changed from [apidoc, application] to [baz]")
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
          DiffNonBreaking("enum added: age_group")
        )
      )
    }

    it("remove enum") {
      ServiceDiff(serviceWithEnum, base).differences should be(
        Seq(
          DiffBreaking("enum removed: age_group")
        )
      )
    }

    it("change enum") {
      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(enum.copy(plural = "groups")))).differences should be(
        Seq(
          DiffNonBreaking("enum age_group plural changed from age_groups to groups")
        )
      )

      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(enum.copy(description = Some("test"))))).differences should be(
        Seq(
          DiffNonBreaking("enum age_group description added: test")
        )
      )

      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(enum.copy(deprecation = Some(Deprecation()))))).differences should be(
        Seq(
          DiffNonBreaking("enum age_group deprecated")
        )
      )

      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(enum.copy(values = Nil)))).differences should be(
        Seq(
          DiffBreaking("enum age_group value removed: 18-25")
        )
      )

      val value2 = value.copy(name = "26-35")
      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(enum.copy(values = Seq(value, value2))))).differences should be(
        Seq(
          DiffNonBreaking("enum age_group value added: 26-35")
        )
      )
    }

    it("change enumValues") {
      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(enum.copy(values = Seq(value.copy(description = Some("test"))))))).differences should be(
        Seq(
          DiffNonBreaking("enum age_group value 18-25 description added: test")
        )
      )

      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(enum.copy(values = Seq(value.copy(deprecation = Some(Deprecation()))))))).differences should be(
        Seq(
          DiffNonBreaking("enum age_group value 18-25 deprecated")
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
          DiffNonBreaking("union added: user")
        )
      )
    }

    it("remove union") {
      ServiceDiff(serviceWithUnion, base).differences should be(
        Seq(
          DiffBreaking("union removed: user")
        )
      )
    }

    it("change union") {
      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(plural = "all_users")))).differences should be(
        Seq(
          DiffNonBreaking("union user plural changed from users to all_users")
        )
      )

      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(description = Some("test"))))).differences should be(
        Seq(
          DiffNonBreaking("union user description added: test")
        )
      )

      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(deprecation = Some(Deprecation()))))).differences should be(
        Seq(
          DiffNonBreaking("union user deprecated")
        )
      )

      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(types = Nil)))).differences should be(
        Seq(
          DiffBreaking("union user type removed: registered")
        )
      )

      val unionType2 = unionType.copy(`type` = "guest")
      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(types = Seq(unionType, unionType2))))).differences should be(
        Seq(
          DiffNonBreaking("union user type added: guest")
        )
      )
    }

    it("change unionTypes") {
      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(types = Seq(unionType.copy(description = Some("test"))))))).differences should be(
        Seq(
          DiffNonBreaking("union user type registered description added: test")
        )
      )

      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(types = Seq(unionType.copy(deprecation = Some(Deprecation()))))))).differences should be(
        Seq(
          DiffNonBreaking("union user type registered deprecated")
        )
      )
    }

  }

  describe("model") {

    val field = Field(
      name = "id",
      `type` = "long",
      description = None,
      deprecation = None,
      default = None,
      required = false,
      minimum = None,
      maximum = None,
      example = None
    )

    val model = Model(
      name = "user",
      plural = "users",
      description = None,
      deprecation = None,
      fields = Seq(field)
    )

    val base = service.copy(models = Nil)
    val serviceWithModel = base.copy(models = Seq(model))

    it("no change") {
      ServiceDiff(serviceWithModel, serviceWithModel).differences should be(Nil)
    }

    it("add model") {
      ServiceDiff(base, serviceWithModel).differences should be(
        Seq(
          DiffNonBreaking("model added: user")
        )
      )
    }

    it("remove model") {
      ServiceDiff(serviceWithModel, base).differences should be(
        Seq(
          DiffBreaking("model removed: user")
        )
      )
    }

    it("change model") {
      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(plural = "all_users")))).differences should be(
        Seq(
          DiffNonBreaking("model user plural changed from users to all_users")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(description = Some("test"))))).differences should be(
        Seq(
          DiffNonBreaking("model user description added: test")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(deprecation = Some(Deprecation()))))).differences should be(
        Seq(
          DiffNonBreaking("model user deprecated")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Nil)))).differences should be(
        Seq(
          DiffBreaking("model user field removed: id")
        )
      )

      val field2 = field.copy(name = "name")
      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field, field2))))).differences should be(
        Seq(
          DiffNonBreaking("model user optional field added: name")
        )
      )

      val field2WithDefault = field.copy(name = "name", default = Some("test"))
      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field, field2WithDefault))))).differences should be(
        Seq(
          DiffNonBreaking("model user optional field added: name, defaults to test")
        )
      )

      val field2Required = field.copy(name = "name", required = true)
      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field, field2Required))))).differences should be(
        Seq(
          DiffBreaking("model user required field added: name")
        )
      )

      val field2RequiredWithDefault = field.copy(name = "name", required = true, default = Some("test"))
      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field, field2RequiredWithDefault))))).differences should be(
        Seq(
          DiffNonBreaking("model user required field added: name, defaults to test")
        )
      )
    }

    it("change fields") {
      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field.copy(`type` = "uuid")))))).differences should be(
        Seq(
          DiffBreaking("model user field id type changed from long to uuid")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field.copy(description = Some("test"))))))).differences should be(
        Seq(
          DiffNonBreaking("model user field id description added: test")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field.copy(deprecation = Some(Deprecation()))))))).differences should be(
        Seq(
          DiffNonBreaking("model user field id deprecated")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field.copy(default = Some("1"))))))).differences should be(
        Seq(
          DiffNonBreaking("model user field id default added: 1")
        )
      )

      val requiredField = field.copy(required = true)
      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(requiredField))))).differences should be(
        Seq(
          DiffBreaking("model user field id is now required")
        )
      )

      ServiceDiff(base.copy(models = Seq(model.copy(fields = Seq(requiredField)))), serviceWithModel).differences should be(
        Seq(
          DiffNonBreaking("model user field id is no longer required")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field.copy(minimum = Some(1))))))).differences should be(
        Seq(
          DiffBreaking("model user field id minimum added: 1")
        )
      )

      ServiceDiff(
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(minimum = Some(1)))))),
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(minimum = Some(1))))))
      ).differences should be(Nil)

      ServiceDiff(
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(minimum = Some(1)))))),
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(minimum = Some(0))))))
      ).differences should be(
        Seq(
          DiffNonBreaking("model user field id minimum changed from 1 to 0")
        )
      )

      ServiceDiff(
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(minimum = Some(0)))))),
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(minimum = Some(1))))))
      ).differences should be(
        Seq(
          DiffBreaking("model user field id minimum changed from 0 to 1")
        )
      )

      ServiceDiff(
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(minimum = Some(0)))))),
        base.copy(models = Seq(model.copy(fields = Seq(field))))
      ).differences should be(
        Seq(
          DiffNonBreaking("model user field id minimum removed: 0")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field.copy(maximum = Some(1))))))).differences should be(
        Seq(
          DiffBreaking("model user field id maximum added: 1")
        )
      )

      ServiceDiff(
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(maximum = Some(1)))))),
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(maximum = Some(1))))))
      ).differences should be(Nil)

      ServiceDiff(
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(maximum = Some(1)))))),
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(maximum = Some(0))))))
      ).differences should be(
        Seq(
          DiffBreaking("model user field id maximum changed from 1 to 0")
        )
      )

      ServiceDiff(
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(maximum = Some(0)))))),
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(maximum = Some(1))))))
      ).differences should be(
        Seq(
          DiffNonBreaking("model user field id maximum changed from 0 to 1")
        )
      )

      ServiceDiff(
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(maximum = Some(0)))))),
        base.copy(models = Seq(model.copy(fields = Seq(field))))
      ).differences should be(
        Seq(
          DiffNonBreaking("model user field id maximum removed: 0")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field.copy(example = Some("foo"))))))).differences should be(
        Seq(
          DiffNonBreaking("model user field id example added: foo")
        )
      )
    }

  }

  describe("resource") {

    val resource = Resource(
      `type` = "user",
      plural = "users",
      description = None,
      deprecation = None,
      operations = Nil
    )

    val base = service.copy(resources = Nil)
    val serviceWithResource = base.copy(resources = Seq(resource))

    it("no change") {
      ServiceDiff(serviceWithResource, serviceWithResource).differences should be(Nil)
    }

    it("add resource") {
      ServiceDiff(base, serviceWithResource).differences should be(
        Seq(
          DiffNonBreaking("resource added: user")
        )
      )
    }

    it("remove resource") {
      ServiceDiff(serviceWithResource, base).differences should be(
        Seq(
          DiffBreaking("resource removed: user")
        )
      )
    }

    it("change resource") {
      ServiceDiff(serviceWithResource, base.copy(resources = Seq(resource.copy(plural = "all_users")))).differences should be(
        Seq(
          DiffNonBreaking("resource user plural changed from users to all_users")
        )
      )

      ServiceDiff(serviceWithResource, base.copy(resources = Seq(resource.copy(description = Some("test"))))).differences should be(
        Seq(
          DiffNonBreaking("resource user description added: test")
        )
      )

      ServiceDiff(serviceWithResource, base.copy(resources = Seq(resource.copy(deprecation = Some(Deprecation()))))).differences should be(
        Seq(
          DiffNonBreaking("resource user deprecated")
        )
      )

      val op = Operation(
        method = Method.Get,
        path = "/users"
      )

      ServiceDiff(serviceWithResource, base.copy(resources = Seq(resource.copy(operations = Seq(op))))).differences should be(
        Seq(
          DiffNonBreaking("resource user operation added: GET /users")
        )
      )

      ServiceDiff(base.copy(resources = Seq(resource.copy(operations = Seq(op)))), serviceWithResource).differences should be(
        Seq(
          DiffBreaking("resource user operation removed: GET /users")
        )
      )
    }

    describe("operation") {

      val operation = Operation(
        method = Method.Get,
        path = "/users/:guid",
        description = None,
        deprecation = None,
        body = None,
        parameters = Nil,
        responses = Nil
      )

      def withOp(op: Operation): Service = {
        service.copy(resources = Seq(resource.copy(operations = Seq(op))))
      }

      val base = service.copy(resources = Seq(resource))
      val serviceWithOperation = withOp(operation)

      it("no change") {
        ServiceDiff(serviceWithOperation, serviceWithOperation).differences should be(Nil)
      }

      it("add operation") {
        ServiceDiff(base, serviceWithOperation).differences should be(
          Seq(
            DiffNonBreaking("resource user operation added: GET /users/:guid")
          )
        )
      }

      it("remove operation") {
        ServiceDiff(serviceWithOperation, base).differences should be(
          Seq(
            DiffBreaking("resource user operation removed: GET /users/:guid")
          )
        )
      }

      it("change operation") {
        ServiceDiff(serviceWithOperation, withOp(operation.copy(method = Method.Post))).differences should be(
          Seq(
            DiffBreaking("resource user operation removed: GET /users/:guid"),
            DiffNonBreaking("resource user operation added: POST /users/:guid")
          )
        )

        ServiceDiff(serviceWithOperation, withOp(operation.copy(path = "/users/:id"))).differences should be(
          Seq(
            DiffBreaking("resource user operation removed: GET /users/:guid"),
            DiffNonBreaking("resource user operation added: GET /users/:id")
          )
        )

        ServiceDiff(serviceWithOperation, withOp(operation.copy(description = Some("test")))).differences should be(
          Seq(
            DiffNonBreaking("resource user operation GET /users/:guid description added: test")
          )
        )

        ServiceDiff(serviceWithOperation, withOp(operation.copy(deprecation = Some(Deprecation())))).differences should be(
          Seq(
            DiffNonBreaking("resource user operation GET /users/:guid deprecated")
          )
        )

        val body = Body(`type` = "user_form")
        ServiceDiff(serviceWithOperation, withOp(operation.copy(body = Some(body)))).differences should be(
          Seq(
            DiffBreaking("resource user operation GET /users/:guid added: body")
          )
        )

        ServiceDiff(
          withOp(operation.copy(body = Some(body))),
          withOp(operation.copy(body = Some(body.copy(`type` = "string"))))
        ).differences should be(
          Seq(
            DiffBreaking("resource user operation GET /users/:guid body type changed from user_form to string")
          )
        )

        ServiceDiff(
          withOp(operation.copy(body = Some(body))),
          withOp(operation.copy(body = Some(body.copy(description = Some("test")))))
        ).differences should be(
          Seq(
            DiffNonBreaking("resource user operation GET /users/:guid body description added: test")
          )
        )

        ServiceDiff(
          withOp(operation.copy(body = Some(body))),
          withOp(operation.copy(body = Some(body.copy(deprecation = Some(Deprecation())))))
        ).differences should be(
          Seq(
            DiffNonBreaking("resource user operation GET /users/:guid body deprecated")
          )
        )

        ServiceDiff(withOp(operation.copy(body = Some(body))), serviceWithOperation).differences should be(
          Seq(
            DiffBreaking("resource user operation GET /users/:guid removed: body")
          )
        )

      }

      it("change operation parameters") {
        val id = Parameter(
          name = "id",
          `type` = "long",
          location = ParameterLocation.Query,
          description = None,
          deprecation = None,
          default = None,
          required = false,
          minimum = None,
          maximum = None,
          example = None
        )

        ServiceDiff(serviceWithOperation, withOp(operation.copy(parameters = Seq(id)))).differences should be(
          Seq(
            DiffNonBreaking("resource user operation GET /users/:guid optional parameter added: id")
          )
        )

        ServiceDiff(serviceWithOperation, withOp(operation.copy(parameters = Seq(id.copy(required = true))))).differences should be(
          Seq(
            DiffBreaking("resource user operation GET /users/:guid required parameter added: id")
          )
        )

        ServiceDiff(withOp(operation.copy(parameters = Seq(id))), serviceWithOperation).differences should be(
          Seq(
            DiffBreaking("resource user operation GET /users/:guid parameter removed: id")
          )
        )

        ServiceDiff(withOp(operation.copy(parameters = Seq(id))), withOp(operation.copy(parameters = Seq(id.copy(`type` = "string"))))).differences should be(
          Seq(
            DiffBreaking("resource user operation GET /users/:guid parameter id type changed from long to string")
          )
        )

        ServiceDiff(withOp(operation.copy(parameters = Seq(id))), withOp(operation.copy(parameters = Seq(id.copy(location = ParameterLocation.Form))))).differences should be(
          Seq(
            DiffBreaking("resource user operation GET /users/:guid parameter id location changed from Query to Form")
          )
        )

        ServiceDiff(withOp(operation.copy(parameters = Seq(id))), withOp(operation.copy(parameters = Seq(id.copy(description = Some("test")))))).differences should be(
          Seq(
            DiffNonBreaking("resource user operation GET /users/:guid parameter id description added: test")
          )
        )

        ServiceDiff(withOp(operation.copy(parameters = Seq(id))), withOp(operation.copy(parameters = Seq(id.copy(deprecation = Some(Deprecation())))))).differences should be(
          Seq(
            DiffNonBreaking("resource user operation GET /users/:guid parameter id deprecated")
          )
        )

        ServiceDiff(withOp(operation.copy(parameters = Seq(id))), withOp(operation.copy(parameters = Seq(id.copy(default = Some("5")))))).differences should be(
          Seq(
            DiffNonBreaking("resource user operation GET /users/:guid parameter id default added: 5")
          )
        )

        ServiceDiff(withOp(operation.copy(parameters = Seq(id))), withOp(operation.copy(parameters = Seq(id.copy(minimum = Some(1)))))).differences should be(
          Seq(
            DiffBreaking("resource user operation GET /users/:guid parameter id minimum added: 1")
          )
        )

        ServiceDiff(withOp(operation.copy(parameters = Seq(id))), withOp(operation.copy(parameters = Seq(id.copy(maximum = Some(1)))))).differences should be(
          Seq(
            DiffBreaking("resource user operation GET /users/:guid parameter id maximum added: 1")
          )
        )

        ServiceDiff(withOp(operation.copy(parameters = Seq(id))), withOp(operation.copy(parameters = Seq(id.copy(example = Some("1")))))).differences should be(
          Seq(
            DiffNonBreaking("resource user operation GET /users/:guid parameter id example added: 1")
          )
        )

      }

      it("change operation responses") {
        val success = Response(
          code = ResponseCodeInt(200),
          `type` = "user",
          description = None,
          deprecation = None
        )

        ServiceDiff(serviceWithOperation, withOp(operation.copy(responses = Seq(success)))).differences should be(
          Seq(
            DiffNonBreaking("resource user operation GET /users/:guid response added: 200")
          )
        )

        ServiceDiff(withOp(operation.copy(responses = Seq(success))), serviceWithOperation).differences should be(
          Seq(
            DiffBreaking("resource user operation GET /users/:guid response removed: 200")
          )
        )

        ServiceDiff(withOp(operation.copy(responses = Seq(success))), withOp(operation.copy(responses = Seq(success.copy(`type` = "string"))))).differences should be(
          Seq(
            DiffBreaking("resource user operation GET /users/:guid response 200 type changed from user to string")
          )
        )

        ServiceDiff(withOp(operation.copy(responses = Seq(success))), withOp(operation.copy(responses = Seq(success.copy(description = Some("test")))))).differences should be(
          Seq(
            DiffNonBreaking("resource user operation GET /users/:guid response 200 description added: test")
          )
        )

        ServiceDiff(withOp(operation.copy(responses = Seq(success))), withOp(operation.copy(responses = Seq(success.copy(deprecation = Some(Deprecation())))))).differences should be(
          Seq(
            DiffNonBreaking("resource user operation GET /users/:guid response 200 deprecated")
          )
        )

      }

    }

  }
}
