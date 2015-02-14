package lib

import org.scalatest.{FunSpec, Matchers}

class DatatypeSpec extends FunSpec with Matchers {

  it("primitives") {
    val resolver = DatatypeResolver(
      enumNames = Seq.empty,
      modelNames = Seq.empty,
      unionNames = Seq.empty
    )

    resolver.parse("string").map(_.label) should be(Some("string"))
    resolver.parse("long").map(_.label) should be(Some("long"))
    resolver.parse("uuid").map(_.label) should be(Some("uuid"))
    resolver.parse("unit").map(_.label) should be(Some("unit"))
    resolver.parse("integer").map(_.label) should be(Some("integer"))
    resolver.parse("date-time-iso8601").map(_.label) should be(Some("date-time-iso8601"))

    resolver.parse("[string]").map(_.label) should be(Some("[string]"))
    resolver.parse("[long]").map(_.label) should be(Some("[long]"))
    resolver.parse("[uuid]").map(_.label) should be(Some("[uuid]"))
    resolver.parse("[unit]").map(_.label) should be(Some("[unit]"))
    resolver.parse("[integer]").map(_.label) should be(Some("[integer]"))
    resolver.parse("[date-time-iso8601]").map(_.label) should be(Some("[date-time-iso8601]"))

    resolver.parse("map").map(_.label) should be(Some("map[string]"))
    resolver.parse("map[string]").map(_.label) should be(Some("map[string]"))
    resolver.parse("map[long]").map(_.label) should be(Some("map[long]"))
    resolver.parse("map[uuid]").map(_.label) should be(Some("map[uuid]"))
    resolver.parse("map[unit]").map(_.label) should be(Some("map[unit]"))
    resolver.parse("map[integer]").map(_.label) should be(Some("map[integer]"))
    resolver.parse("map[date-time-iso8601]").map(_.label) should be(Some("map[date-time-iso8601]"))

    resolver.parse("user") should be(None)
  }

  it("with enums and models") {
    val resolver = DatatypeResolver(
      enumNames = Seq("age_group"),
      modelNames = Seq("user"),
      unionNames = Seq.empty
    )

    resolver.parse("map[age_group]").map(_.label) should be(Some("map[age_group]"))
    resolver.parse("user").map(_.label) should be(Some("user"))

    resolver.parse("group").map(_.label) should be(None)
  }

  it("precedence rules") {
    val resolver = DatatypeResolver(
      enumNames = Seq("age_group", "string", "other_enum"),
      modelNames = Seq("guest_user", "registered_user", "uuid"),
      unionNames = Seq("user", "other_enum")
    )

    resolver.parse("age_group") should be(Some(Datatype.Singleton(Type(Kind.Enum, "age_group"))))
    resolver.parse("other_enum") should be(Some(Datatype.Singleton(Type(Kind.Enum, "other_enum"))))
    resolver.parse("string") should be(Some(Datatype.Singleton(Type(Kind.Primitive, "string"))))
    resolver.parse("guest_user") should be(Some(Datatype.Singleton(Type(Kind.Model, "guest_user"))))
    resolver.parse("registered_user") should be(Some(Datatype.Singleton(Type(Kind.Model, "registered_user"))))
    resolver.parse("uuid") should be(Some(Datatype.Singleton(Type(Kind.Primitive, "uuid"))))
    resolver.parse("user") should be(Some(Datatype.Singleton(Type(Kind.Union, "user"))))
  }

}
