package lib

import io.apibuilder.spec.v0.models._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsObject, Json}

class ServiceDiffSpec  extends PlaySpec with GuiceOneAppPerSuite with db.Helpers with TestHelper {

  import DiffFactories._

  private lazy val service = readService("../spec/apibuilder-spec.json")

  "no changes" in {
    ServiceDiff(service, service).differences must be(Nil)
  }

  "info" must {
    lazy val base = service.copy(info = Info(contact = None, license = None))

    "contact" in {
      val contact = Contact(
        name = Some("Mike"),
        url = Some("http://foo.com"),
        email = Some("mbryzek@mailinator.com")
      )

      ServiceDiff(base, base.copy(info = Info(contact = Some(contact)))).differences must be(
        Seq(
          NotMaterial.nonBreaking(s"contact/name added: Mike"),
          NotMaterial.nonBreaking(s"contact/url added: http://foo.com"),
          NotMaterial.nonBreaking(s"contact/email added: mbryzek@mailinator.com")
        )
      )
    }

    "license" in {
      val license = License(
        name = "MIT",
        url = Some("http://opensource.org/licenses/MIT")
      )

      ServiceDiff(base, base.copy(info = Info(license = Some(license)))).differences must be(
        Seq(
          Material.nonBreaking(s"license/name added: MIT"),
          Material.nonBreaking(s"license/url added: http://opensource.org/licenses/MIT")
        )
      )
    }

  }

  "name" in {
    ServiceDiff(service, service.copy(name = "test")).differences must be(
      Seq(
        Material.nonBreaking(s"name changed from ${service.name} to test")
      )
    )
  }

  "organization key" in {
    ServiceDiff(service, service.copy(organization = Organization(key = "foo"))).differences must be(
      Seq(
        Material.nonBreaking(s"organization/key changed from ${service.organization.key} to foo")
      )
    )
  }

  "application key" in {
    ServiceDiff(service, service.copy(application = Application(key = "foo"))).differences must be(
      Seq(
        Material.nonBreaking(s"application/key changed from ${service.application.key} to foo")
      )
    )
  }

  "namespace" in {
    ServiceDiff(service, service.copy(namespace = "test")).differences must be(
      Seq(
        Material.breaking(s"namespace changed from ${service.namespace} to test")
      )
    )
  }

  "version" in {
    ServiceDiff(service, service.copy(version = "0.0.1")).differences must be(
      Seq(
        NotMaterial.nonBreaking(s"version changed from ${service.version} to 0.0.1")
      )
    )
  }

  "baseUrl" in {
    val base = service.copy(baseUrl = None)

    ServiceDiff(base, base.copy(baseUrl = None)).differences must be(Nil)

    ServiceDiff(base, base.copy(baseUrl = Some("http://foo.com"))).differences must be(
      Seq(
        Material.nonBreaking(s"base_url added: http://foo.com")
      )
    )

    ServiceDiff(base.copy(baseUrl = Some("http://foo.com")), base).differences must be(
      Seq(
        Material.nonBreaking(s"base_url removed: http://foo.com")
      )
    )

    ServiceDiff(
      base.copy(baseUrl = Some("http://foo.com")),
      base.copy(baseUrl = Some("http://foobar.com"))
    ).differences must be(
      Seq(
        Material.nonBreaking(s"base_url changed from http://foo.com to http://foobar.com")
      )
    )
  }

  "description" in {
    val base = service.copy(description = None)

    ServiceDiff(base, base.copy(description = None)).differences must be(Nil)

    ServiceDiff(base, base.copy(description = Some("foo"))).differences must be(
      Seq(
        Material.nonBreaking(s"description added: foo")
      )
    )

    ServiceDiff(base.copy(description = Some("foo")), base).differences must be(
      Seq(
        Material.nonBreaking(s"description removed: foo")
      )
    )

    ServiceDiff(
      base.copy(description = Some("foo")),
      base.copy(description = Some("foobar"))
    ).differences must be(
      Seq(
        Material.nonBreaking(s"description changed from foo to foobar")
      )
    )
  }

  "headers" must {
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

    "no change" in {
      ServiceDiff(serviceWithHeader, serviceWithHeader).differences must be(Nil)
    }

    "remove header" in {
      ServiceDiff(serviceWithHeader, base).differences must be(
        Seq(
          Material.nonBreaking("header removed: x-test")
        )
      )
    }

    "add optional header" in {
      ServiceDiff(base, serviceWithHeader).differences must be(
        Seq(
          Material.nonBreaking("optional header added: x-test")
        )
      )
    }

    "add required header" in {
      val serviceWithRequiredHeader = base.copy(headers = Seq(header.copy(required = true)))
      ServiceDiff(base, serviceWithRequiredHeader).differences must be(
        Seq(
          Material.breaking("required header added: x-test")
        )
      )
    }

    "type" in {
      val serviceWithHeader2 = base.copy(headers = Seq(header.copy(`type` = "long")))
      ServiceDiff(serviceWithHeader, serviceWithHeader2).differences must be(
        Seq(
          Material.breaking("header x-test type changed from string to long")
        )
      )
    }

    "description" must {

      "add" in {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(description = Some("foo"))))
        ServiceDiff(serviceWithHeader, serviceWithHeader2).differences must be(
          Seq(
            Material.nonBreaking("header x-test description added: foo")
          )
        )
      }

      "remove" in {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(description = Some("foo"))))
        ServiceDiff(serviceWithHeader2, serviceWithHeader).differences must be(
          Seq(
            Material.nonBreaking("header x-test description removed: foo")
          )
        )
      }

      "change" in {
        val serviceWithHeader1 = base.copy(headers = Seq(header.copy(description = Some("foo"))))
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(description = Some("bar"))))
        ServiceDiff(serviceWithHeader1, serviceWithHeader2).differences must be(
          Seq(
            Material.nonBreaking("header x-test description changed from foo to bar")
          )
        )
      }
    }

    "deprecation" must {

      "add" in {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(deprecation = Some(Deprecation()))))
        ServiceDiff(serviceWithHeader, serviceWithHeader2).differences must be(
          Seq(
            Material.nonBreaking("header x-test deprecated")
          )
        )
      }

      "add with description" in {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(deprecation = Some(Deprecation(description = Some("test"))))))
        ServiceDiff(serviceWithHeader, serviceWithHeader2).differences must be(
          Seq(
            Material.nonBreaking("header x-test deprecated: test")
          )
        )
      }

      "remove" in {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(deprecation = Some(Deprecation()))))
        ServiceDiff(serviceWithHeader2, serviceWithHeader).differences must be(
          Seq(
            Material.nonBreaking("header x-test removed: deprecation")
          )
        )
      }
    }

    "default" must {

      "add" in {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(default = Some("test"))))
        ServiceDiff(serviceWithHeader, serviceWithHeader2).differences must be(
          Seq(
            Material.nonBreaking("header x-test default added: test")
          )
        )
      }

      "remove" in {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(default = Some("test"))))
        ServiceDiff(serviceWithHeader2, serviceWithHeader).differences must be(
          Seq(
            Material.breaking("header x-test default removed: test")
          )
        )
      }

      "change" in {
        val serviceWithHeader2 = base.copy(headers = Seq(header.copy(default = Some("foo"))))
        val serviceWithHeader3 = base.copy(headers = Seq(header.copy(default = Some("bar"))))
        ServiceDiff(serviceWithHeader2, serviceWithHeader3).differences must be(
          Seq(
            Material.nonBreaking("header x-test default changed from foo to bar")
          )
        )
      }
    }

  }

  "import" must {

    val imp = Import(
      uri = "https://www.apibuilder.io/apicollective/apibuilder-spec/0.9.6/service.json",
      namespace = "io.apibuilder.spec.v0",
      organization = Organization(key = "gilt"),
      application = Application(key = "apibuilder-spec"),
      version = "0.9.6",
      enums = Seq("method", "parameter_location", "response_code_option"),
      unions = Seq("response_code"),
      models = Seq("apibuilder", "application")
    )

    val base = service.copy(imports = Nil)
    val serviceWithImport = base.copy(imports = Seq(imp))

    "no change" in {
      ServiceDiff(serviceWithImport, serviceWithImport).differences must be(Nil)
    }

    "remove import" in {
      ServiceDiff(serviceWithImport, base).differences must be(
        Seq(
          NotMaterial.nonBreaking("import removed: https://www.apibuilder.io/apicollective/apibuilder-spec/0.9.6/service.json")
        )
      )
    }

    "add import" in {
      ServiceDiff(base, serviceWithImport).differences must be(
        Seq(
          NotMaterial.nonBreaking("import added: https://www.apibuilder.io/apicollective/apibuilder-spec/0.9.6/service.json")
        )
      )
    }

    "change import" in {
      val imp2 = Import(
        uri = "https://www.apibuilder.io/apicollective/apibuilder-spec/0.9.6/service.json",
        namespace = "io.apibuilder.spec.v1",
        organization = Organization(key = "gilt2"),
        application = Application(key = "apibuilder-spec2"),
        version = "1.0.0",
        enums = Seq("foo"),
        unions = Seq("bar"),
        models = Seq("baz")
      )

      val prefix = "import https://www.apibuilder.io/apicollective/apibuilder-spec/0.9.6/service.json"

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(namespace = imp2.namespace)))).differences must be(
        Seq(
          NotMaterial.nonBreaking(s"$prefix namespace changed from io.apibuilder.spec.v0 to io.apibuilder.spec.v1")
        )
      )

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(organization = imp2.organization)))).differences must be(
        Seq(
          NotMaterial.nonBreaking(s"$prefix organization/key changed from gilt to gilt2")
        )
      )

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(application = imp2.application)))).differences must be(
        Seq(
          NotMaterial.nonBreaking(s"$prefix application/key changed from apibuilder-spec to apibuilder-spec2")
        )
      )

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(version = imp2.version)))).differences must be(
        Seq(
          NotMaterial.nonBreaking(s"$prefix version changed from 0.9.6 to 1.0.0")
        )
      )

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(enums = imp2.enums)))).differences must be(
        Seq(
          NotMaterial.nonBreaking(s"$prefix enums changed from [method, parameter_location, response_code_option] to [foo]")
        )
      )

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(unions = imp2.unions)))).differences must be(
        Seq(
          NotMaterial.nonBreaking(s"$prefix unions changed from [response_code] to [bar]")
        )
      )

      ServiceDiff(serviceWithImport, base.copy(imports = Seq(imp.copy(models = imp2.models)))).differences must be(
        Seq(
          NotMaterial.nonBreaking(s"$prefix models changed from [apibuilder, application] to [baz]")
        )
      )
    }

  }

  "enum" must {

    val value = EnumValue(
      name = "18-25",
      description = None,
      deprecation = None
    )

    val `enum` = Enum(
      name = "age_group",
      plural = "age_groups",
      description = None,
      deprecation = None,
      values = Seq(value)
    )

    val base = service.copy(enums = Nil)
    val serviceWithEnum = base.copy(enums = Seq(`enum`))

    "no change" in {
      ServiceDiff(serviceWithEnum, serviceWithEnum).differences must be(Nil)
    }

    "add enum" in {
      ServiceDiff(base, serviceWithEnum).differences must be(
        Seq(
          Material.nonBreaking("enum added: age_group")
        )
      )
    }

    "remove enum" in {
      ServiceDiff(serviceWithEnum, base).differences must be(
        Seq(
          Material.breaking("enum removed: age_group")
        )
      )
    }

    "change enum" in {
      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(`enum`.copy(plural = "groups")))).differences must be(
        Seq(
          Material.nonBreaking("enum age_group plural changed from age_groups to groups")
        )
      )

      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(`enum`.copy(description = Some("test"))))).differences must be(
        Seq(
          Material.nonBreaking("enum age_group description added: test")
        )
      )

      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(`enum`.copy(deprecation = Some(Deprecation()))))).differences must be(
        Seq(
          Material.nonBreaking("enum age_group deprecated")
        )
      )

      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(`enum`.copy(values = Nil)))).differences must be(
        Seq(
          Material.breaking("enum age_group value removed: 18-25")
        )
      )

      val value2 = value.copy(name = "26-35")
      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(`enum`.copy(values = Seq(value, value2))))).differences must be(
        Seq(
          Material.nonBreaking("enum age_group value added: 26-35")
        )
      )
    }

    "change enumValues" in {
      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(`enum`.copy(values = Seq(value.copy(description = Some("test"))))))).differences must be(
        Seq(
          Material.nonBreaking("enum age_group value 18-25 description added: test")
        )
      )

      ServiceDiff(serviceWithEnum, base.copy(enums = Seq(`enum`.copy(values = Seq(value.copy(deprecation = Some(Deprecation()))))))).differences must be(
        Seq(
          Material.nonBreaking("enum age_group value 18-25 deprecated")
        )
      )
    }

  }

  "union" must {

    val unionType = UnionType(
      `type` = "registered",
      description = None,
      deprecation = None
    )

    val union = Union(
      name = "user",
      plural = "users",
      discriminator = None,
      description = None,
      deprecation = None,
      types = Seq(unionType)
    )

    val base = service.copy(unions = Nil)
    val serviceWithUnion = base.copy(unions = Seq(union))

    "no change" in {
      ServiceDiff(serviceWithUnion, serviceWithUnion).differences must be(Nil)
    }

    "add union" in {
      ServiceDiff(base, serviceWithUnion).differences must be(
        Seq(
          Material.nonBreaking("union added: user")
        )
      )
    }

    "remove union" in {
      ServiceDiff(serviceWithUnion, base).differences must be(
        Seq(
          Material.breaking("union removed: user")
        )
      )
    }

    "change union" in {
      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(plural = "all_users")))).differences must be(
        Seq(
          Material.nonBreaking("union user plural changed from users to all_users")
        )
      )

      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(description = Some("test"))))).differences must be(
        Seq(
          Material.nonBreaking("union user description added: test")
        )
      )

      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(deprecation = Some(Deprecation()))))).differences must be(
        Seq(
          Material.nonBreaking("union user deprecated")
        )
      )

      val serviceWithFooInterface = base.copy(unions = Seq(union.copy(interfaces = Seq("foo"))))
      ServiceDiff(serviceWithUnion, serviceWithFooInterface).differences must be(
        Seq(
          Material.breaking("union user interface added: foo")
        )
      )

      ServiceDiff(serviceWithFooInterface, serviceWithUnion).differences must be(
        Seq(
          Material.breaking("union user interface removed: foo")
        )
      )

      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(types = Nil)))).differences must be(
        Seq(
          Material.breaking("union user type removed: registered")
        )
      )

      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(discriminator = Some("type_identifier"))))).differences must be(
        Seq(
          Material.breaking("union user discriminator added: type_identifier")
        )
      )

      ServiceDiff(base.copy(unions = Seq(union.copy(discriminator = Some("type_identifier")))), serviceWithUnion).differences must be(
        Seq(
          Material.breaking("union user discriminator removed: type_identifier")
        )
      )

      val unionType2 = unionType.copy(`type` = "guest")
      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(types = Seq(unionType, unionType2))))).differences must be(
        Seq(
          Material.nonBreaking("union user type added: guest")
        )
      )
    }

    "change unionTypes" in {
      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(types = Seq(unionType.copy(description = Some("test"))))))).differences must be(
        Seq(
          Material.nonBreaking("union user type registered description added: test")
        )
      )

      ServiceDiff(serviceWithUnion, base.copy(unions = Seq(union.copy(types = Seq(unionType.copy(deprecation = Some(Deprecation()))))))).differences must be(
        Seq(
          Material.nonBreaking("union user type registered deprecated")
        )
      )
    }

    "change union type default" in {
      val unionWithDiscriminator = union.copy(
        discriminator = Some("discriminator")
      )

      val unionWithDiscriminatorAndDefault = union.copy(
        discriminator = Some("discriminator"),
        types = Seq(
          unionType.copy(default = Some(true))
        )
      )

      val serviceNoDefault = base.copy(unions = Seq(unionWithDiscriminator))
      val serviceWithDefault = base.copy(unions = Seq(unionWithDiscriminatorAndDefault))

      ServiceDiff(serviceNoDefault, serviceWithDefault).differences must be(
        Seq(
          Material.nonBreaking("union user default type added: registered")
        )
      )

      ServiceDiff(serviceWithDefault, serviceNoDefault).differences must be(
        Seq(
          Material.breaking("union user default type removed: registered")
        )
      )
    }

  }

  "model" must {

    val field = Field(
      name = "id",
      `type` = "long",
      description = None,
      deprecation = None,
      default = None,
      required = false,
      minimum = None,
      maximum = None,
      example = None,
      attributes = Nil
    )

    val model = Model(
      name = "user",
      plural = "users",
      description = None,
      deprecation = None,
      fields = Seq(field),
      attributes = Nil
    )

    val base = service.copy(models = Nil)
    val serviceWithModel = base.copy(models = Seq(model))

    "no change" in {
      ServiceDiff(serviceWithModel, serviceWithModel).differences must be(Nil)
    }

    "add model" in {
      ServiceDiff(base, serviceWithModel).differences must be(
        Seq(
          Material.nonBreaking("model added: user")
        )
      )
    }

    "remove model" in {
      ServiceDiff(serviceWithModel, base).differences must be(
        Seq(
          Material.breaking("model removed: user")
        )
      )
    }

    "change model" in {
      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(plural = "all_users")))).differences must be(
        Seq(
          Material.nonBreaking("model user plural changed from users to all_users")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(description = Some("test"))))).differences must be(
        Seq(
          Material.nonBreaking("model user description added: test")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(deprecation = Some(Deprecation()))))).differences must be(
        Seq(
          Material.nonBreaking("model user deprecated")
        )
      )

      val serviceWithFooInterface = base.copy(models = Seq(model.copy(interfaces = Seq("foo"))))
      ServiceDiff(serviceWithModel, serviceWithFooInterface).differences must be(
        Seq(
          Material.breaking("model user interface added: foo")
        )
      )

      ServiceDiff(serviceWithFooInterface, serviceWithModel).differences must be(
        Seq(
          Material.breaking("model user interface removed: foo")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Nil)))).differences must be(
        Seq(
          Material.breaking("model user field removed: id")
        )
      )

      val field2 = field.copy(name = "name")
      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field, field2))))).differences must be(
        Seq(
          Material.nonBreaking("model user optional field added: name")
        )
      )

      val field2WithDefault = field.copy(name = "name", default = Some("test"))
      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field, field2WithDefault))))).differences must be(
        Seq(
          Material.nonBreaking("model user optional field added: name, defaults to test")
        )
      )

      val field2Required = field.copy(name = "name", required = true)
      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field, field2Required))))).differences must be(
        Seq(
          Material.breaking("model user required field added: name")
        )
      )

      val field2RequiredWithDefault = field.copy(name = "name", required = true, default = Some("test"))
      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field, field2RequiredWithDefault))))).differences must be(
        Seq(
          Material.nonBreaking("model user required field added: name, defaults to test")
        )
      )

      val attribute1 = Attribute("attribute1", Json.parse(""" {"name": "value1"}""").as[JsObject], Some("Description 1"))
      val attribute1ValueUpdated = Attribute("attribute1", Json.parse(""" {"name": "value updated"}""").as[JsObject], Some("Description 1"))
      val attribute1DescriptionUpdated = Attribute("attribute1", Json.parse(""" {"name": "value1"}""").as[JsObject], Some("Description updated"))
      val attribute1DescriptionRemoved = Attribute("attribute1", Json.parse(""" {"name": "value1"}""").as[JsObject], None)

      val attribute2 = Attribute("attribute2", Json.parse(""" {"name": "value2"}""").as[JsObject], Some("Description 2"))

      val fieldWithAttribute = field.copy(attributes = Seq(attribute1))
      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(fieldWithAttribute))))
      ).differences must be(
        Seq(
          Material.nonBreaking("model user field id attribute added: attribute1")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(attributes = Seq(attribute1))))).differences must be(
        Seq(
          Material.nonBreaking("model user attribute added: attribute1")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(attributes = Seq(attribute1, attribute2))))).differences must be(
        Seq(
          Material.nonBreaking("model user attribute added: attribute1"),
          Material.nonBreaking("model user attribute added: attribute2")
        )
      )

      ServiceDiff(base.copy(models = Seq(model.copy(attributes = Seq(attribute1)))), serviceWithModel).differences must be(
        Seq(
          Material.nonBreaking("model user attribute removed: attribute1")
        )
      )

      ServiceDiff(
        base.copy(models = Seq(model.copy(attributes = Seq(attribute1)))),
        base.copy(models = Seq(model.copy(attributes = Seq(attribute2))))
      ).differences must be(
        Seq(
          Material.nonBreaking("model user attribute removed: attribute1"),
          Material.nonBreaking("model user attribute added: attribute2")
        )
      )

      ServiceDiff(
        base.copy(models = Seq(model.copy(attributes = Seq(attribute1)))),
        base.copy(models = Seq(model.copy(attributes = Seq(attribute1ValueUpdated))))
      ).differences must be(
        Seq(
          Material.nonBreaking("""model user attribute 'attribute1' value changed from {"name":"value1"} to {"name":"value updated"}""")
        )
      )

      ServiceDiff(
        base.copy(models = Seq(model.copy(attributes = Seq(attribute1)))),
        base.copy(models = Seq(model.copy(attributes = Seq(attribute1DescriptionUpdated))))
      ).differences must be(
        Seq(
          Material.nonBreaking("model user attribute 'attribute1' description changed from Description 1 to Description updated")
        )
      )

      ServiceDiff(
        base.copy(models = Seq(model.copy(attributes = Seq(attribute1)))),
        base.copy(models = Seq(model.copy(attributes = Seq(attribute1DescriptionRemoved))))
      ).differences must be(
        Seq(
          Material.nonBreaking("model user attribute 'attribute1' description removed: Description 1")
        )
      )

      ServiceDiff(
        base.copy(models = Seq(model.copy(attributes = Seq(attribute1DescriptionRemoved)))),
        base.copy(models = Seq(model.copy(attributes = Seq(attribute1))))
      ).differences must be(
        Seq(
          Material.nonBreaking("model user attribute 'attribute1' description added: Description 1")
        )
      )

      ServiceDiff(
        base.copy(models = Seq(model.copy(attributes = Seq(attribute1)))),
        base.copy(models = Seq(model.copy(attributes = Seq(attribute1.copy(deprecation = Some(Deprecation()))))))
      ).differences must be(
        Seq(
          Material.nonBreaking("model user attribute 'attribute1' deprecated")
        )
      )
    }


    "change fields" in {
      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field.copy(`type` = "uuid")))))).differences must be(
        Seq(
          Material.breaking("model user field id type changed from long to uuid")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field.copy(description = Some("test"))))))).differences must be(
        Seq(
          Material.nonBreaking("model user field id description added: test")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field.copy(deprecation = Some(Deprecation()))))))).differences must be(
        Seq(
          Material.nonBreaking("model user field id deprecated")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field.copy(default = Some("1"))))))).differences must be(
        Seq(
          Material.nonBreaking("model user field id default added: 1")
        )
      )

      val requiredField = field.copy(required = true)
      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(requiredField))))).differences must be(
        Seq(
          Material.breaking("model user field id is now required")
        )
      )

      ServiceDiff(base.copy(models = Seq(model.copy(fields = Seq(requiredField)))), serviceWithModel).differences must be(
        Seq(
          Material.nonBreaking("model user field id is no longer required")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field.copy(minimum = Some(1))))))).differences must be(
        Seq(
          Material.breaking("model user field id minimum added: 1")
        )
      )

      ServiceDiff(
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(minimum = Some(1)))))),
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(minimum = Some(1))))))
      ).differences must be(Nil)

      ServiceDiff(
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(minimum = Some(1)))))),
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(minimum = Some(0))))))
      ).differences must be(
        Seq(
          Material.nonBreaking("model user field id minimum changed from 1 to 0")
        )
      )

      ServiceDiff(
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(minimum = Some(0)))))),
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(minimum = Some(1))))))
      ).differences must be(
        Seq(
          Material.breaking("model user field id minimum changed from 0 to 1")
        )
      )

      ServiceDiff(
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(minimum = Some(0)))))),
        base.copy(models = Seq(model.copy(fields = Seq(field))))
      ).differences must be(
        Seq(
          Material.nonBreaking("model user field id minimum removed: 0")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field.copy(maximum = Some(1))))))).differences must be(
        Seq(
          Material.breaking("model user field id maximum added: 1")
        )
      )

      ServiceDiff(
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(maximum = Some(1)))))),
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(maximum = Some(1))))))
      ).differences must be(Nil)

      ServiceDiff(
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(maximum = Some(1)))))),
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(maximum = Some(0))))))
      ).differences must be(
        Seq(
          Material.breaking("model user field id maximum changed from 1 to 0")
        )
      )

      ServiceDiff(
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(maximum = Some(0)))))),
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(maximum = Some(1))))))
      ).differences must be(
        Seq(
          Material.nonBreaking("model user field id maximum changed from 0 to 1")
        )
      )

      ServiceDiff(
        base.copy(models = Seq(model.copy(fields = Seq(field.copy(maximum = Some(0)))))),
        base.copy(models = Seq(model.copy(fields = Seq(field))))
      ).differences must be(
        Seq(
          Material.nonBreaking("model user field id maximum removed: 0")
        )
      )

      ServiceDiff(serviceWithModel, base.copy(models = Seq(model.copy(fields = Seq(field.copy(example = Some("foo"))))))).differences must be(
        Seq(
          Material.nonBreaking("model user field id example added: foo")
        )
      )
    }

  }

  //annotations inform how data provided to or extracted from a service might be used. they should be add/removable
  //without breaking the builds of dependent code (though warnings may be created and linters and other analytical tools may fail)
  "annotations" must {
    val annot = Annotation(
      "red",
      description = Some("Field interests the red team"),
      deprecation = None
    )

    val base = service.copy(annotations = Nil)
    val withAnnotation = base.copy(annotations = Seq(annot))

    "no change" in {
      ServiceDiff(withAnnotation, withAnnotation).differences must be (Nil)
    }

    "add annotation" in {
      ServiceDiff(base, withAnnotation).differences must be(
        Seq(
          Material.nonBreaking("annotation added: red")
        )
      )
    }

    "remove annotation" in {
      ServiceDiff(withAnnotation, base).differences must be(
        Seq(
          Material.nonBreaking("annotation removed: red")
        )
      )
    }

    "change resource" in {
      ServiceDiff(withAnnotation, base.copy(annotations = Seq(annot.copy(description = None)))).differences must be(
        Seq(
          Material.nonBreaking("annotation red description removed: Field interests the red team")
        )
      )

      ServiceDiff(withAnnotation, base.copy(annotations = Seq(annot.copy(deprecation = Some(Deprecation()))))).differences must be(
        Seq(
          Material.nonBreaking("annotation red deprecated")
        )
      )
    }
  }

  "resource" must {

    val resource = Resource(
      `type` = "user",
      plural = "users",
      description = None,
      deprecation = None,
      operations = Nil
    )

    val base = service.copy(resources = Nil)
    val serviceWithResource = base.copy(resources = Seq(resource))

    "no change" in {
      ServiceDiff(serviceWithResource, serviceWithResource).differences must be(Nil)
    }

    "add resource" in {
      ServiceDiff(base, serviceWithResource).differences must be(
        Seq(
          Material.nonBreaking("resource added: user")
        )
      )
    }

    "remove resource" in {
      ServiceDiff(serviceWithResource, base).differences must be(
        Seq(
          Material.breaking("resource removed: user")
        )
      )
    }

    "change resource" in {
      ServiceDiff(serviceWithResource, base.copy(resources = Seq(resource.copy(plural = "all_users")))).differences must be(
        Seq(
          Material.nonBreaking("resource user plural changed from users to all_users")
        )
      )

      ServiceDiff(serviceWithResource, base.copy(resources = Seq(resource.copy(description = Some("test"))))).differences must be(
        Seq(
          Material.nonBreaking("resource user description added: test")
        )
      )

      ServiceDiff(serviceWithResource, base.copy(resources = Seq(resource.copy(deprecation = Some(Deprecation()))))).differences must be(
        Seq(
          Material.nonBreaking("resource user deprecated")
        )
      )

      val op = Operation(
        method = Method.Get,
        path = "/users"
      )

      ServiceDiff(serviceWithResource, base.copy(resources = Seq(resource.copy(operations = Seq(op))))).differences must be(
        Seq(
          Material.nonBreaking("resource user operation added: GET /users")
        )
      )

      ServiceDiff(base.copy(resources = Seq(resource.copy(operations = Seq(op)))), serviceWithResource).differences must be(
        Seq(
          Material.breaking("resource user operation removed: GET /users")
        )
      )
    }

    "operation" must {

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

      "no change" in {
        ServiceDiff(serviceWithOperation, serviceWithOperation).differences must be(Nil)
      }

      "add operation" in {
        ServiceDiff(base, serviceWithOperation).differences must be(
          Seq(
            Material.nonBreaking("resource user operation added: GET /users/:guid")
          )
        )
      }

      "remove operation" in {
        ServiceDiff(serviceWithOperation, base).differences must be(
          Seq(
            Material.breaking("resource user operation removed: GET /users/:guid")
          )
        )
      }

      "change operation" in {
        ServiceDiff(serviceWithOperation, withOp(operation.copy(method = Method.Post))).differences must be(
          Seq(
            Material.breaking("resource user operation removed: GET /users/:guid"),
            Material.nonBreaking("resource user operation added: POST /users/:guid")
          )
        )

        ServiceDiff(serviceWithOperation, withOp(operation.copy(path = "/users/:id"))).differences must be(
          Seq(
            Material.breaking("resource user operation removed: GET /users/:guid"),
            Material.nonBreaking("resource user operation added: GET /users/:id")
          )
        )

        ServiceDiff(serviceWithOperation, withOp(operation.copy(description = Some("test")))).differences must be(
          Seq(
            Material.nonBreaking("resource user operation GET /users/:guid description added: test")
          )
        )

        ServiceDiff(serviceWithOperation, withOp(operation.copy(deprecation = Some(Deprecation())))).differences must be(
          Seq(
            Material.nonBreaking("resource user operation GET /users/:guid deprecated")
          )
        )

        val body = Body(`type` = "user_form")
        ServiceDiff(serviceWithOperation, withOp(operation.copy(body = Some(body)))).differences must be(
          Seq(
            Material.breaking("resource user operation GET /users/:guid added: body")
          )
        )

        ServiceDiff(
          withOp(operation.copy(body = Some(body))),
          withOp(operation.copy(body = Some(body.copy(`type` = "string"))))
        ).differences must be(
          Seq(
            Material.breaking("resource user operation GET /users/:guid body type changed from user_form to string")
          )
        )

        ServiceDiff(
          withOp(operation.copy(body = Some(body))),
          withOp(operation.copy(body = Some(body.copy(description = Some("test")))))
        ).differences must be(
          Seq(
            Material.nonBreaking("resource user operation GET /users/:guid body description added: test")
          )
        )

        ServiceDiff(
          withOp(operation.copy(body = Some(body))),
          withOp(operation.copy(body = Some(body.copy(deprecation = Some(Deprecation())))))
        ).differences must be(
          Seq(
            Material.nonBreaking("resource user operation GET /users/:guid body deprecated")
          )
        )

        ServiceDiff(withOp(operation.copy(body = Some(body))), serviceWithOperation).differences must be(
          Seq(
            Material.breaking("resource user operation GET /users/:guid removed: body")
          )
        )

      }

      "operation parameters" must {
        val parameter = Parameter(
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

        def withParam(param: Parameter): Service = {
          service.copy(resources = Seq(resource.copy(operations = Seq(operation.copy(parameters = Seq(param))))))
        }

        val base = service.copy(resources = Seq(resource.copy(operations = Seq(operation))))
        val serviceWithOpParam = withParam(parameter)

        "no change" in {
          ServiceDiff(serviceWithOpParam, serviceWithOpParam).differences must be(Nil)
        }

        "add optional parameter" in {
          ServiceDiff(serviceWithOperation, serviceWithOpParam).differences must be(
            Seq(
              Material.nonBreaking("resource user operation GET /users/:guid optional parameter added: id")
            )
          )
        }

        "add required parameter" in {
          ServiceDiff(serviceWithOperation, withParam(parameter.copy(required = true))).differences must be(
            Seq(
              Material.breaking("resource user operation GET /users/:guid required parameter added: id")
            )
          )
        }

        "remove parameter" in {
          ServiceDiff(serviceWithOpParam, serviceWithOperation).differences must be(
            Seq(
              Material.breaking("resource user operation GET /users/:guid parameter removed: id")
            )
          )
        }

        "change type" in {
          ServiceDiff(serviceWithOpParam, withParam(parameter.copy(`type` = "string"))).differences must be(
            Seq(
              Material.breaking("resource user operation GET /users/:guid parameter id type changed from long to string")
            )
          )
        }

        "change location" in {
          ServiceDiff(serviceWithOpParam, withParam(parameter.copy(location = ParameterLocation.Form))).differences must be(
            Seq(
              Material.breaking("resource user operation GET /users/:guid parameter id location changed from Query to Form")
            )
          )
        }

        "add description" in {
          ServiceDiff(serviceWithOpParam, withParam(parameter.copy(description = Some("test")))).differences must be(
            Seq(
              Material.nonBreaking("resource user operation GET /users/:guid parameter id description added: test")
            )
          )
        }

        "add deprecation" in {
          ServiceDiff(serviceWithOpParam, withParam(parameter.copy(deprecation = Some(Deprecation())))).differences must be(
            Seq(
              Material.nonBreaking("resource user operation GET /users/:guid parameter id deprecated")
            )
          )
        }

        "add default" in {
          ServiceDiff(serviceWithOpParam, withParam(parameter.copy(default = Some("5")))).differences must be(
            Seq(
              Material.nonBreaking("resource user operation GET /users/:guid parameter id default added: 5")
            )
          )
        }

        "add minimum" in {
          ServiceDiff(serviceWithOpParam, withParam(parameter.copy(minimum = Some(1)))).differences must be(
            Seq(
              Material.breaking("resource user operation GET /users/:guid parameter id minimum added: 1")
            )
          )
        }

        "add maximum" in {
          ServiceDiff(serviceWithOpParam, withParam(parameter.copy(maximum = Some(1)))).differences must be(
            Seq(
              Material.breaking("resource user operation GET /users/:guid parameter id maximum added: 1")
            )
          )
        }

        "add example" in {
          ServiceDiff(serviceWithOpParam, withParam(parameter.copy(example = Some("1")))).differences must be(
            Seq(
              Material.nonBreaking("resource user operation GET /users/:guid parameter id example added: 1")
            )
          )
        }

        val attribute1 = Attribute("attribute1", Json.parse(""" {"name": "value1"}""").as[JsObject], Some("Description 1"))
        val attribute1ValueUpdated = Attribute("attribute1", Json.parse(""" {"name": "value updated"}""").as[JsObject], Some("Description 1"))

        "add attribute" in {
          ServiceDiff(serviceWithOpParam, withParam(parameter.copy(attributes = Some(Seq(attribute1))))).differences must be(
            Seq(
              Material.nonBreaking("resource user operation GET /users/:guid attribute added: attribute1")
            )
          )
        }

        "update attribute" in {
          ServiceDiff(withParam(parameter.copy(attributes = Some(Seq(attribute1)))), withParam(parameter.copy(attributes = Some(Seq(attribute1ValueUpdated))))).differences must be(
            Seq(
              Material.nonBreaking("""resource user operation GET /users/:guid attribute 'attribute1' value changed from {"name":"value1"} to {"name":"value updated"}""")
            )
          )
        }

        "remove attribute" in {
          ServiceDiff(withParam(parameter.copy(attributes = Some(Seq(attribute1)))), serviceWithOpParam).differences must be(
            Seq(
              Material.nonBreaking("resource user operation GET /users/:guid attribute removed: attribute1")
            )
          )
        }

      }

      "change operation responses" in {
        val success = Response(
          code = ResponseCodeInt(200),
          `type` = "user",
          description = None,
          deprecation = None
        )

        ServiceDiff(serviceWithOperation, withOp(operation.copy(responses = Seq(success)))).differences must be(
          Seq(
            Material.nonBreaking("resource user operation GET /users/:guid response added: 200")
          )
        )

        ServiceDiff(withOp(operation.copy(responses = Seq(success))), serviceWithOperation).differences must be(
          Seq(
            Material.breaking("resource user operation GET /users/:guid response removed: 200")
          )
        )

        ServiceDiff(withOp(operation.copy(responses = Seq(success))), withOp(operation.copy(responses = Seq(success.copy(`type` = "string"))))).differences must be(
          Seq(
            Material.breaking("resource user operation GET /users/:guid response 200 type changed from user to string")
          )
        )

        ServiceDiff(withOp(operation.copy(responses = Seq(success))), withOp(operation.copy(responses = Seq(success.copy(description = Some("test")))))).differences must be(
          Seq(
            Material.nonBreaking("resource user operation GET /users/:guid response 200 description added: test")
          )
        )

        ServiceDiff(withOp(operation.copy(responses = Seq(success))), withOp(operation.copy(responses = Seq(success.copy(deprecation = Some(Deprecation())))))).differences must be(
          Seq(
            Material.nonBreaking("resource user operation GET /users/:guid response 200 deprecated")
          )
        )

      }

    }

  }
}
