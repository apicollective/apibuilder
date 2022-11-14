package builder.api_json.templates

import helpers.ApiJsonHelpers
import io.apibuilder.spec.v0.models.{Model => SpecModel}
import io.apibuilder.api.json.v0.models._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsString, Json}

class ModelMergeSpec extends AnyWordSpec with Matchers with ApiJsonHelpers {

  "models" must {
    def setup(
      templateField: Option[Field] = None,
      modelField: Option[Field] = None,
      templateAttributes: Option[Seq[Attribute]] = None,
      modelAttributes: Option[Seq[Attribute]] = None,
      templateInterfaces: Option[Seq[String]] = None,
      modelInterfaces: Option[Seq[String]] = None,
      annotations: Map[String, Annotation] = Map.empty,
      interfaces: Map[String, Interface] = Map.empty,
    ): SpecModel = {
      val name = randomName()
      val apiJson = makeApiJson(
        templates = Some(makeTemplates(
          models = Some(Map(
            name -> makeModel(
              fields = templateField.toSeq,
              attributes = templateAttributes,
              interfaces = templateInterfaces
            )
          ))
        )),
        models = Map(randomName() -> makeModel(
          templates = Some(Seq(makeTemplateDeclaration(name))),
          fields = modelField.toSeq,
          attributes = modelAttributes,
          interfaces = modelInterfaces
        )),
        interfaces = interfaces,
        annotations = annotations,
      )

      expectValid(apiJson).models.head
    }

    "fields" must {
      "inherit if not defined" in {
        val templateField = makeField()
        setup(
          templateField = Some(templateField),
          modelField = None
        ).fields.map(_.name) mustBe Seq(templateField.name)
      }

      "include all other model fields" in {
        val templateField = makeField()
        val modelField = makeField()
        setup(
          templateField = Some(templateField),
          modelField = Some(modelField)
        ).fields.map(_.name) mustBe Seq(templateField.name, modelField.name)
      }
    }

    "merge" must {
      "description" must {
        def setupModel(modelDesc: Option[String]) = {
          val field = makeField()
          setup(
            templateField = Some(field.copy(description = Some("foo"))),
            modelField = Some(field.copy(description = modelDesc))
          ).fields.head.description.get
        }

        "inherit" in {
          setupModel(None) mustBe "foo"
        }

        "preserve" in {
          setupModel(Some("bar")) mustBe "bar"
        }
      }

      "deprecation" must {
        def setupModel(model: Option[Deprecation]) = {
          val field = makeField()
          setup(
            templateField = Some(field.copy(deprecation = Some(makeDeprecation(description = Some("foo"))))),
            modelField = Some(field.copy(deprecation = model))
          ).fields.head.deprecation.get.description.get
        }

        "inherit" in {
          setupModel(None) mustBe "foo"
        }

        "preserve" in {
          setupModel(Some(makeDeprecation(description = Some("bar")))) mustBe "bar"
        }
      }

      "fields" must {
        def setupFields(templateField: Field, modelField: Field, annotations: Map[String, Annotation] = Map.empty) = {
          setup(
            templateField = Some(templateField),
            modelField = Some(modelField),
            annotations = annotations
          ).fields
        }

        "inherit" in {
          val templateField = makeField()
          val modelField = makeField()
          setupFields(templateField, modelField).map(_.name) mustBe Seq(templateField.name, modelField.name)
        }

        "description" must {
          def setupField(model: Option[String]) = {
            val field = makeField(description = None)
            setupFields(
              templateField = field.copy(description = Some("foo")),
              modelField = field.copy(description = model),
            ).head.description.get
          }

          "inherit" in {
            setupField(None) mustBe "foo"
          }

          "preserve" in {
            setupField(Some("bar")) mustBe "bar"
          }
        }

        "default" must {
          def setupField(modelFieldDefault: Option[String]) = {
            val field = makeField(default = None)
            setupFields(
              templateField = field.copy(default = Some(JsString("foo"))),
              modelField = field.copy(default = modelFieldDefault.map(JsString)),
            ).head.default.get
          }

          "inherit" in {
            setupField(None) mustBe "foo"
          }

          "preserve" in {
            setupField(Some("bar")) mustBe "bar"
          }
        }

        "example" must {
          def setupField(model: Option[String]) = {
            val field = makeField(example = None)
            setupFields(
              templateField = field.copy(example = Some("foo")),
              modelField = field.copy(example = model),
            ).head.example.get
          }

          "inherit" in {
            setupField(None) mustBe "foo"
          }

          "preserve" in {
            setupField(Some("bar")) mustBe "bar"
          }
        }

        "minimum" must {
          def setupField(model: Option[Long]) = {
            val field = makeField(minimum = None)
            setupFields(
              templateField = field.copy(minimum = Some(0)),
              modelField = field.copy(minimum = model),
            ).head.minimum.get
          }

          "inherit" in {
            setupField(None) mustBe 0
          }

          "preserve" in {
            setupField(Some(1)) mustBe 1
          }
        }

        "maximum" must {
          def setupField(model: Option[Long]) = {
            val field = makeField(maximum = None)
            setupFields(
              templateField = field.copy(maximum = Some(0)),
              modelField = field.copy(maximum = model),
            ).head.maximum.get
          }

          "inherit" in {
            setupField(None) mustBe 0
          }

          "preserve" in {
            setupField(Some(1)) mustBe 1
          }
        }

        "attributes" must {
          def setupField(model: Option[String]) = {
            val field = makeField(attributes = None)
            setupFields(
              templateField = field.copy(attributes = Some(Seq(makeAttribute(name = "foo")))),
              modelField = field.copy(attributes = model.map { n => Seq(makeAttribute(name = n))}),
            ).head.attributes.map(_.name)
          }

          "inherit" in {
            setupField(None) mustBe Seq("foo")
          }

          "preserve" in {
            setupField(Some("bar")) mustBe Seq("foo", "bar")
          }
        }

        "annotations" must {
          def setupField(model: Option[String]) = {
            val field = makeField(annotations = None)
            setupFields(
              templateField = field.copy(annotations = Some(Seq("foo"))),
              modelField = field.copy(annotations = model.map(Seq(_))),
              annotations = (Seq("foo") ++ model.toSeq).map { n =>
                n -> makeAnnotation()
              }.toMap
            ).head.annotations
          }

          "inherit" in {
            setupField(None) mustBe Seq("foo")
          }

          "preserve" in {
            setupField(Some("bar")) mustBe Seq("bar", "foo")
          }
        }
      }

      "attributes" must {
        def setupModel(template: Option[Attribute], model: Option[Attribute]) = {
          setup(
            templateAttributes = Some(template.toSeq),
            modelAttributes = Some(model.toSeq),
          ).attributes
        }

        "inherit" in {
          setupModel(
            template = Some(makeAttribute(name = "foo")),
            model = None
          ).map(_.name) mustBe Seq("foo")
        }

        "preserve" in {
          setupModel(
            template = Some(makeAttribute(name = "foo")),
            model = Some(makeAttribute(name = "bar")),
          ).map(_.name) mustBe Seq("foo", "bar")
        }

        "merge" must {
          "value" in {
            val attr = makeAttribute()
            val value = Json.obj("bar" -> randomName())
            val value2 = Json.obj("bar" -> randomName(), randomName() -> randomName())

            setupModel(
              template = Some(attr.copy(value = value)),
              model = Some(attr),
            ).head.value mustBe value

            setupModel(
              template = Some(attr.copy(value = value)),
              model = Some(attr.copy(value = value2)),
            ).head.value mustBe value2
          }
        }
      }

      "interfaces" must {
        def setupModel(template: Option[String], model: Option[String]) = {
          setup(
            templateInterfaces = Some(template.toSeq),
            modelInterfaces = Some(model.toSeq),
            interfaces = (template.toSeq ++ model.toSeq).map { n =>
              n -> makeInterface()
            }.toMap
          ).interfaces.filter { i =>
            // Remove the interface from the template itself
            template.contains(i) || model.contains(i)
          }
        }

        "inherit" in {
          setupModel(
            template = Some("foo"),
            model = None
          ) mustBe Seq("foo")
        }

        "preserve" in {
          setupModel(
            template = Some("foo"),
            model = Some("bar")
          ) mustBe Seq("bar", "foo")
        }
      }

      "templates" must {
        def setupTemplate(template: Model, name: String = randomName()) = {
          val apiJson = makeApiJson(
            templates = Some(makeTemplates(
              models = Some(Map(name -> template))
            )),
            models = Map(randomName() -> makeModel(
              templates = Some(Seq(makeTemplateDeclaration(name)))
            ))
          )
          expectValid(apiJson)
        }

        "inherit" in {
          def setup(templates: Seq[String]) = {
            val apiJson = makeApiJson(
              templates = Some(makeTemplates(
                models = Some(Map(
                  "a" -> makeModel(fields = Seq(makeField("a"))),
                  "b" -> makeModel(fields = Seq(makeField("b")), templates = Some(Seq(makeTemplateDeclaration("a")))),
                  "c" -> makeModel(fields = Seq(makeField("c")), templates = Some(Seq(makeTemplateDeclaration("b")))),
                ))
              )),
              models = Map(randomName() -> makeModel(
                templates = Some(templates.map { name => makeTemplateDeclaration(name) })
              ))
            )
            expectValid(apiJson).models.head.fields.map(_.name)
          }

          setup(Nil) mustBe Nil
          setup(Seq("a")) mustBe Seq("a")
          setup(Seq("b")) mustBe Seq("a", "b")
          setup(Seq("c")) mustBe Seq("a", "b", "c")
          setup(Seq("a", "b" ,"c")) mustBe Seq("a", "b", "c")
          setup(Seq("a", "b" , "c")) mustBe Seq("a", "b", "c")
        }

        "setup interfaces" in {
          val apiJson = setupTemplate(
            name = "foo",
            template = makeModel(
              fields = Seq(makeField("foo"))
            )
          )
          apiJson.models.head.interfaces mustBe Seq("foo")

          val i = apiJson.interfaces.head
          i.name mustBe "foo"
          i.fields.map(_.name) mustBe Seq("foo")
        }
      }
    }

  }

  "validates field type" in {
    expectErrors {
      makeApiJson(
        templates = Some(makeTemplates(
          models = Some(Map(
            "group" -> makeModel(fields = Seq(makeField(`type` = "group")))
          ))
        )),
        models = Map("user_group" -> makeModel(
          templates = Some(Seq(makeTemplateDeclaration("group")))
        ))
      )
    }.head.contains("type[group] is an interface and cannot be used as a field type") mustBe true
  }

}
