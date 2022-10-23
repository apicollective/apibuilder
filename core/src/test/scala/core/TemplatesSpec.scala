package core

import helpers.ApiJsonHelpers
import io.apibuilder.api.json.v0.models.Field
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TemplatesSpec extends AnyWordSpec with Matchers with ApiJsonHelpers {

  "models" must {
    def setup(templateField: Field, modelField: Option[Field]) = {
      val name = randomName()

      val apiJson = makeApiJson(
        templates = Some(makeTemplates(
          models = Some(Map(
            name -> makeModel(fields = Seq(templateField))
          ))
        )),
        models = Map(name -> makeModel(fields = modelField.toSeq))
      )

      expectValid(apiJson).models.head.fields
    }

    "fields" must {
      "inherit if not defined" in {
        val templateField = makeField()
        setup(templateField, None).map(_.name) mustBe Seq(templateField.name)
      }

      "include all other model fields" in {
        val templateField = makeField()
        val modelField = makeField()
        setup(templateField, Some(modelField)).map(_.name) mustBe Seq(templateField.name, modelField.name)
      }
    }

    "merge" must {
      "description" must {
        def setupModelDesc(modelDesc: Option[String]) = {
          val field = makeField()
          setup(
            templateField = field.copy(description = Some("foo")),
            modelField = Some(field.copy(description = modelDesc))
          ).head.description.get
        }

        "inherit" in {
          setupModelDesc(None) mustBe "foo"
        }

        "not override" in {
          setupModelDesc(Some("bar")) mustBe "bar"
        }
      }
    }
  }

}
