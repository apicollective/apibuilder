package core

import core.TestHelper.ServiceValidatorForSpecs
import io.apibuilder.api.json.v0.models.{EnumValue, Field, Parameter}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.JsString

class ServiceEnumSpec extends AnyFunSpec with Matchers with helpers.ApiJsonHelpers {

  private[this] val ageGroupField: Field = makeField(name = "age_group", `type` = "age_group")
  private[this] val ageGroupParameter: Parameter = makeParameter(name = "age_group", `type` = "age_group")
  private[this] val twentiesEnumValue: EnumValue = makeEnumValue(name = "Twenties")

  private[this] def setup[T](
    twentiesEnumValue: EnumValue = twentiesEnumValue,
    field: Field = ageGroupField,
    parameter: Parameter = ageGroupParameter,
  )(
    f: ServiceValidatorForSpecs => T
  ) = {
    f(
      TestHelper.serviceValidator(
        makeApiJson(
          enums = Map("age_group" -> makeEnum(
            values = Seq(
              twentiesEnumValue,
              makeEnumValue(name = "Thirties"),
            )
          )),
          models = Map("user" -> makeModel(fields = Seq(field))),
          resources = Map("user" -> makeResource(
            operations = Seq(makeOperation(parameters = Some(Seq(parameter))))
          ))
        )
      )
    )
  }

  describe("defaults") {

    it("supports a known default") {
      setup(
        field = ageGroupField.copy(
          default = Some(JsString("Twenties")),
        )
      ) { v =>
        v.errors() should be(Nil)
        v.service().models.head.fields.find(_.name == "age_group").get.default should be(Some("Twenties"))
      }
    }

    it("validates unknown defaults") {
      setup(
        field = ageGroupField.copy(
          default = Some(JsString("other")),
        )
      ) { v =>
        v.errors() should be(
          Seq("Model[user] Field[age_group] default[other] is not a valid value for enum[age_group]. Valid values are: Twenties, Thirties")
        )
      }
    }

    it("validates unknown defaults in parameters") {
      setup(
        parameter = ageGroupParameter.copy(
          default = Some(JsString("other")),
        )
      ) { v =>
        v.errors() should be(
          Seq("Resource[user] GET /users/ param[age_group] default[other] is not a valid value for enum[age_group]. Valid values are: Twenties, Thirties")
        )
      }
    }

  }

  it("field can be defined as an enum") {
    setup() { v =>
      v.errors() should be(Nil)
      v.service().models.head.fields.find {
        _.name == "age_group"
      }.get.`type` should be("age_group")
    }
  }

  it("validates that enum values do not start with numbers") {
    setup(
      twentiesEnumValue = twentiesEnumValue.copy(name = "1")
    ) { v =>
      v.errors() should be(
        Seq("Enum[age_group] name[1] is invalid: Name must start with a letter")
      )
    }
  }

}
