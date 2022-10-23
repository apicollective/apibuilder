package core

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TemplatesSpec extends AnyWordSpec with Matchers with helpers.ApiJsonHelpers {

  "models" must {
    "fields" must {
      "inherit if not defined" in {
        val name = randomName()
        val templateField = makeField()

        val apiJson = makeApiJson(
          templates = Some(makeTemplates(
            models = Some(Map(
              name -> makeModel(fields = Seq(templateField))
            ))
          )),
          models = Map(name -> makeModel())
        )

        expectValid(apiJson).models.head.fields.map(_.name) mustBe Seq(templateField.name)
      }

      "include all other model fields" in {
        val name = randomName()
        val templateField = makeField()
        val modelField = makeField()

        val apiJson = makeApiJson(
          templates = Some(makeTemplates(
            models = Some(Map(
              name -> makeModel(fields = Seq(templateField))
            ))
          )),
          models = Map(name -> makeModel(fields = Seq(modelField)))
        )

        expectValid(apiJson).models.head.fields.map(_.name) mustBe Seq(templateField.name, modelField.name)
      }
    }

    "merge" must {
      "description" must {
        def setup(modelDesc: Option[String]) = {
          val name = randomName()
          val templateField = makeField()

          val apiJson = makeApiJson(
            templates = Some(makeTemplates(
              models = Some(Map(
                name -> makeModel(fields = Seq(templateField.copy(description = Some("foo"))))
              ))
            )),
            models = Map(name -> makeModel(fields = Seq(templateField.copy(description = modelDesc))))
          )

          expectValid(apiJson).models.head.fields.head.description.get
        }

        "inherit" in {
          setup(None) mustBe "foo"
        }
        "not override" in {
          setup(Some("bar")) mustBe "bar"
        }

      }
    }
  }

}
