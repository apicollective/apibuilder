package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers
import java.util.UUID

class TypeValidatorSpec extends FunSpec with Matchers {

  val validator = TypeValidator(
    enums = Seq(
      TypeValidatorEnums("age_group", Seq("youth", "adult"))
    )
  )

  it("enum values") {
    validator.validate(Type.Enum("age_group"), "youth") should be(None)
    validator.validate(Type.Enum("age_group"), "adult") should be(None)
    validator.validate(Type.Enum("age_group"), "o") should be(
      Some("default[o] is not a valid value for enum[age_group]. Valid values are: youth, adult")
    )
  }

  it("enum invalid name") {
    validator.validate(Type.Enum("foo"), "youth") should be(
      Some("could not find enum named[foo]")
    )
  }

  it("models cannot have defaults") {
    validator.validate(Type.Model("foo"), "bar") should be(
      Some("default[bar] is not valid for model[foo]. apidoc does not support default values for models")
    )
  }

  it("boolean") {
    validator.validate(Type.Primitive(Primitives.Boolean), "false") should be(None)
    validator.validate(Type.Primitive(Primitives.Boolean), "true") should be(None)
    validator.validate(Type.Primitive(Primitives.Boolean), "bar") should be(
      Some(s"Value[bar] is not a valid boolean. Must be one of: true, false")
    )
  }

  it("double") {
    validator.validate(Type.Primitive(Primitives.Double), "1") should be(None)
    validator.validate(Type.Primitive(Primitives.Double), "0") should be(None)
    validator.validate(Type.Primitive(Primitives.Double), "-1") should be(None)
    validator.validate(Type.Primitive(Primitives.Double), "123128397.1") should be(None)
    validator.validate(Type.Primitive(Primitives.Double), "bar") should be(
      Some(s"Value[bar] is not a valid double")
    )
  }

  it("integer") {
    validator.validate(Type.Primitive(Primitives.Integer), "1") should be(None)
    validator.validate(Type.Primitive(Primitives.Integer), "0") should be(None)
    validator.validate(Type.Primitive(Primitives.Integer), "-1") should be(None)
    validator.validate(Type.Primitive(Primitives.Integer), "1.2") should be(
      Some(s"Value[1.2] is not a valid integer")
    )
    validator.validate(Type.Primitive(Primitives.Integer), "bar") should be(
      Some(s"Value[bar] is not a valid integer")
    )
  }

  it("long") {
    validator.validate(Type.Primitive(Primitives.Long), "1") should be(None)
    validator.validate(Type.Primitive(Primitives.Long), "0") should be(None)
    validator.validate(Type.Primitive(Primitives.Long), "-1") should be(None)
    validator.validate(Type.Primitive(Primitives.Long), "1.2") should be(
      Some(s"Value[1.2] is not a valid long")
    )
    validator.validate(Type.Primitive(Primitives.Long), "bar") should be(
      Some(s"Value[bar] is not a valid long")
    )
  }

  it("decimal") {
    validator.validate(Type.Primitive(Primitives.Decimal), "1") should be(None)
    validator.validate(Type.Primitive(Primitives.Decimal), "0") should be(None)
    validator.validate(Type.Primitive(Primitives.Decimal), "-1") should be(None)
    validator.validate(Type.Primitive(Primitives.Decimal), "123128397.1") should be(None)
    validator.validate(Type.Primitive(Primitives.Decimal), "bar") should be(
      Some(s"Value[bar] is not a valid decimal")
    )
  }

  it("unit") {
    validator.validate(Type.Primitive(Primitives.Unit), "") should be(None)
    validator.validate(Type.Primitive(Primitives.Unit), "1") should be(
      Some(s"Value[1] is not a valid unit type - must be the empty string")
    )
  }

  it("uuid") {
    validator.validate(Type.Primitive(Primitives.Uuid), UUID.randomUUID.toString) should be(None)
    validator.validate(Type.Primitive(Primitives.Uuid), "1") should be(
      Some(s"Value[1] is not a valid uuid")
    )
  }

  it("date-iso8601") {
    validator.validate(Type.Primitive(Primitives.DateIso8601), "2014-11-12") should be(None)
    validator.validate(Type.Primitive(Primitives.DateIso8601), "2014-11-90") should be(
      Some("Value[2014-11-90] is not a valid date-iso8601")
    )
    validator.validate(Type.Primitive(Primitives.DateIso8601), "bar") should be(
      Some("Value[bar] is not a valid date-iso8601")
    )
    validator.validate(Type.Primitive(Primitives.DateIso8601), "2014-04-29T11:56:52Z") should be(
      Some("Value[2014-04-29T11:56:52Z] is not a valid date-iso8601")
    )
  }

  it("date-time-iso8601") {
    validator.validate(Type.Primitive(Primitives.DateTimeIso8601), "2014-04-29T11:56:52Z") should be(None)
    validator.validate(Type.Primitive(Primitives.DateTimeIso8601), "2014-11-90") should be(
      Some("Value[2014-11-90] is not a valid date-time-iso8601")
    )
    validator.validate(Type.Primitive(Primitives.DateTimeIso8601), "bar") should be(
      Some("Value[bar] is not a valid date-time-iso8601")
    )
  }

  it("string") {
    validator.validate(Type.Primitive(Primitives.String), "") should be(None)
    validator.validate(Type.Primitive(Primitives.String), "foo") should be(None)
  }

}
