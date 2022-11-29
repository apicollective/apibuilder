package core

import helpers.ApiJsonHelpers
import io.apibuilder.api.json.v0.models.Field
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class DuplicateNamesSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  private[this] val idField: Field = makeField(name = "id")

  it("disallow model names when camel case vs. snake case") {
    TestHelper.expectSingleError(
      makeApiJson(
        models = Map(
          "some_user" -> makeModel(fields = Seq(idField)),
          "someUser" -> makeModel(fields = Seq(idField)),
        )
      )
    ) should be("Model[some_user] appears more than once")
  }

  it("disallow enum values when camel case vs. snake case") {
    TestHelper.expectSingleError(
      makeApiJson(
        enums = Map(
          "foo" -> makeEnum(values = Seq(
            makeEnumValue(name = "some_id"),
            makeEnumValue(name = "someId")
          )),
        ),
      )
    ) should be("Enum[foo] value[some_id] appears more than once")
  }

  it("disallow model field names when camel case vs. snake case") {
    TestHelper.expectSingleError(
      makeApiJson(
        models = Map(
          "user" -> makeModel(fields = Seq(
            makeField(name = "someId"),
            makeField(name = "some_id"),
          ))
        )
      )
    ) should be("Model[user] field[some_id] appears more than once")
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

    TestHelper.expectSingleError(json.format("user")) should be(
      "Resource[user] GET /users/:guid Parameter[some_id] appears more than once"
    )
  }
}
