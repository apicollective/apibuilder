package core

import lib.{Datatype, PrimitiveMetadata, Primitives, Type, TypeKind}
import java.util.UUID
import org.scalatest.{FunSpec, Matchers}

class TypeValidatorSpec extends FunSpec with Matchers {

  val validator = TypeValidator(
    enums = Seq(
      TypesProviderEnum("age_group", Seq("youth", "adult"))
    )
  )

  it("enum values") {
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Enum, "age_group"))), "youth") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Enum, "age_group"))), "adult") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Enum, "age_group"))), "o") should be(
      Some("default[o] is not a valid value for enum[age_group]. Valid values are: youth, adult")
    )
  }

  it("enum invalid name") {
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Enum, "foo"))), "youth") should be(
      Some("could not find enum named[foo]")
    )
  }

  it("models cannot have defaults") {
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Model, "foo"))), "bar") should be(
      Some("default[bar] is not valid for model[foo]. apidoc does not support default values for models")
    )
  }

  it("boolean") {
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Boolean.toString))), "false") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Boolean.toString))), "true") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Boolean.toString))), "bar") should be(
      Some(s"Value[bar] is not a valid boolean. Must be one of: true, false")
    )
  }

  it("double") {
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Double.toString))), "1") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Double.toString))), "0") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Double.toString))), "-1") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Double.toString))), "123128397.1") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Double.toString))), "bar") should be(
      Some(s"Value[bar] is not a valid double")
    )
  }

  it("integer") {
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Integer.toString))), "1") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Integer.toString))), "0") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Integer.toString))), "-1") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Integer.toString))), "1.2") should be(
      Some(s"Value[1.2] is not a valid integer")
    )
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Integer.toString))), "bar") should be(
      Some(s"Value[bar] is not a valid integer")
    )
  }

  it("long") {
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Long.toString))), "1") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Long.toString))), "0") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Long.toString))), "-1") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Long.toString))), "1.2") should be(
      Some(s"Value[1.2] is not a valid long")
    )
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Long.toString))), "bar") should be(
      Some(s"Value[bar] is not a valid long")
    )
  }

  it("decimal") {
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Decimal.toString))), "1") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Decimal.toString))), "0") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Decimal.toString))), "-1") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Decimal.toString))), "123128397.1") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Decimal.toString))), "bar") should be(
      Some(s"Value[bar] is not a valid decimal")
    )
  }

  it("unit") {
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Unit.toString))), "") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Unit.toString))), "1") should be(
      Some(s"Value[1] is not a valid unit type - must be the empty string")
    )
  }

  it("uuid") {
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Uuid.toString))), UUID.randomUUID.toString) should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.Uuid.toString))), "1") should be(
      Some(s"Value[1] is not a valid uuid")
    )
  }

  it("date-iso8601") {
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.DateIso8601.toString))), "2014-11-12") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.DateIso8601.toString))), "2014-11-90") should be(
      Some("Value[2014-11-90] is not a valid date-iso8601")
    )
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.DateIso8601.toString))), "bar") should be(
      Some("Value[bar] is not a valid date-iso8601")
    )
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.DateIso8601.toString))), "2014-04-29T11:56:52Z") should be(
      Some("Value[2014-04-29T11:56:52Z] is not a valid date-iso8601")
    )
  }

  it("date-time-iso8601") {
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.DateTimeIso8601.toString))), "2014-04-29T11:56:52Z") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.DateTimeIso8601.toString))), "2014-11-90") should be(
      Some("Value[2014-11-90] is not a valid date-time-iso8601")
    )
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.DateTimeIso8601.toString))), "bar") should be(
      Some("Value[bar] is not a valid date-time-iso8601")
    )
  }

  it("string") {
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.String.toString))), "") should be(None)
    validator.validate(Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.String.toString))), "foo") should be(None)
  }

  it("PrimitiveMetadata defined for all primitives") {
    val missing = Primitives.All.filter( p => PrimitiveMetadata.All.find( pm => pm.primitive == p ).isEmpty )
    if (!missing.isEmpty) {
      fail("Missing PrimitiveMetadata for: " + missing.mkString(" "))
    }
  }

  it("PrimitiveMetadata examples are valid") {
    PrimitiveMetadata.All.foreach { pm =>
      pm.examples.foreach { example =>
        validator.validate(
          Datatype.Singleton(Seq(Type(TypeKind.Primitive, pm.primitive.toString))),
          example
        ) should be(None)
      }
    }
  }

  it("PrimitiveMetadata is alphabetized") {
    PrimitiveMetadata.All.map(_.primitive.toString) should be(PrimitiveMetadata.All.map(_.primitive.toString).sorted)
  }

}
