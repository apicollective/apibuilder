package builder.api_json.templates

import helpers.ApiJsonHelpers
import io.apibuilder.api.json.v0.models.{Deprecation, Field}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ModelMergeSpec extends AnyWordSpec with Matchers with ApiJsonHelpers {

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
        def setupModel(modelDesc: Option[String]) = {
          val field = makeField()
          setup(
            templateField = field.copy(description = Some("foo")),
            modelField = Some(field.copy(description = modelDesc))
          ).head.description.get
        }

        "inherit" in {
          setupModel(None) mustBe "foo"
        }

        "preserve model description" in {
          setupModel(Some("bar")) mustBe "bar"
        }
      }

      "deprecation" must {
        def setupModel(modelDeprecation: Option[Deprecation]) = {
          val field = makeField()
          setup(
            templateField = field.copy(deprecation = Some(makeDeprecation(description = Some("foo")))),
            modelField = Some(field.copy(deprecation = modelDeprecation))
          ).head.deprecation.get.description.get
        }

        "inherit" in {
          setupModel(None) mustBe "foo"
        }

        "preserve model deprecation" in {
          setupModel(Some(makeDeprecation(description = Some("bar")))) mustBe "bar"
        }
      }
    }
  }

}
