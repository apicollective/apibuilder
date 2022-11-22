package core

import io.apibuilder.api.json.v0.models.Field
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class DuplicateNamesSpec extends AnyFunSpec with Matchers with helpers.ApiJsonHelpers {

  private[this] val idField: Field = makeField(name = "id")

  it("disallow model names when camel case vs. snake case") {
    TestHelper.serviceValidator(
      makeApiJson(
        models = Map(
          "some_user" -> makeModel(fields = Seq(idField)),
          "someUser" -> makeModel(fields = Seq(idField)),
        )
      )
    ).errors() should be(
      Seq("Model[some_user] appears more than once")
    )
  }

  it("disallow enum values when camel case vs. snake case") {
    TestHelper.serviceValidator(
      makeApiJson(
        enums = Map(
          "foo" -> makeEnum(values = Seq(
            makeEnumValue(name = "some_id"),
            makeEnumValue(name = "someId")
          )),
        ),
      )
    ).errors() should be(
      Seq("Enum[foo] value[some_id] appears more than once")
    )
  }

  it("disallow model field names when camel case vs. snake case") {
    TestHelper.serviceValidator(
      makeApiJson(
        models = Map(
          "user" -> makeModel(fields = Seq(
            makeField(name = "someId"),
            makeField(name = "some_id"),
          ))
        )
      )
    ).errors() should be(
      Seq("Model[user] field[some_id] appears more than once")
    )
  }

  it("disallow parameters duplicate param names when camel case vs. snake case") {
    val json =
      """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "string" }
          ]
        }
      },
      "resources": {
        "user": {
          "operations": [
            {
              "method": "GET",
              "path": "/:guid",
              "parameters": [
                { "name": "some_id", "type": "string" },
                { "name": "someId", "type": "string" }
              ],
              "responses": {
                "204": { "type": "unit" }
              }
            }
          ]
        }
      }
    }
    """

    TestHelper.serviceValidatorFromApiJson(json.format("user")).errors() should be(
      Seq("Resource[user] GET /users/:guid Parameter[some_id] appears more than once")
    )
  }
}
