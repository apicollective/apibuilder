package lib

import org.scalatest.{FunSpec, Matchers}

class DatatypeSpec extends FunSpec with Matchers {

  it("primitives") {
    val resolver = DatatypeResolver()

    resolver.parse("string").map(_.label) should be(Some("string"))
    resolver.parse("long").map(_.label) should be(Some("long"))
    resolver.parse("uuid").map(_.label) should be(Some("uuid"))
    resolver.parse("unit").map(_.label) should be(Some("unit"))
    resolver.parse("integer").map(_.label) should be(Some("integer"))
    resolver.parse("date-time-iso8601").map(_.label) should be(Some("date-time-iso8601"))
    resolver.parse("string | uuid").map(_.label) should be(Some("string | uuid"))
    resolver.parse("string | uuid | unit").map(_.label) should be(Some("string | uuid | unit"))

    resolver.parse("[string]").map(_.label) should be(Some("[string]"))
    resolver.parse("[long]").map(_.label) should be(Some("[long]"))
    resolver.parse("[uuid]").map(_.label) should be(Some("[uuid]"))
    resolver.parse("[unit]").map(_.label) should be(Some("[unit]"))
    resolver.parse("[integer]").map(_.label) should be(Some("[integer]"))
    resolver.parse("[date-time-iso8601]").map(_.label) should be(Some("[date-time-iso8601]"))
    resolver.parse("[string | uuid]").map(_.label) should be(Some("[string | uuid]"))
    resolver.parse("[string | uuid | unit]").map(_.label) should be(Some("[string | uuid | unit]"))

    resolver.parse("map").map(_.label) should be(Some("map[string]"))
    resolver.parse("map[string]").map(_.label) should be(Some("map[string]"))
    resolver.parse("map[long]").map(_.label) should be(Some("map[long]"))
    resolver.parse("map[uuid]").map(_.label) should be(Some("map[uuid]"))
    resolver.parse("map[unit]").map(_.label) should be(Some("map[unit]"))
    resolver.parse("map[integer]").map(_.label) should be(Some("map[integer]"))
    resolver.parse("map[date-time-iso8601]").map(_.label) should be(Some("map[date-time-iso8601]"))
    resolver.parse("map[string | uuid]").map(_.label) should be(Some("map[string | uuid]"))
    resolver.parse("map[string | uuid | unit]").map(_.label) should be(Some("map[string | uuid | unit]"))

    resolver.parse("option[string]").map(_.label) should be(Some("option[string]"))
    resolver.parse("option[long]").map(_.label) should be(Some("option[long]"))
    resolver.parse("option[uuid]").map(_.label) should be(Some("option[uuid]"))
    resolver.parse("option[unit]").map(_.label) should be(Some("option[unit]"))
    resolver.parse("option[integer]").map(_.label) should be(Some("option[integer]"))
    resolver.parse("option[date-time-iso8601]").map(_.label) should be(Some("option[date-time-iso8601]"))
    resolver.parse("option[string | uuid]").map(_.label) should be(Some("option[string | uuid]"))
    resolver.parse("option[string | uuid | unit]").map(_.label) should be(Some("option[string | uuid | unit]"))

    resolver.parse("user") should be(None)
    resolver.parse("string | user") should be(None)
  }

  it("with enums and models") {
    val resolver = DatatypeResolver(
      enumNames = Seq("age_group"),
      modelNames = Seq("user")
    )

    resolver.parse("map[age_group]").map(_.label) should be(Some("map[age_group]"))
    resolver.parse("user").map(_.label) should be(Some("user"))
    resolver.parse("uuid | user").map(_.label) should be(Some("uuid | user"))

    resolver.parse("group").map(_.label) should be(None)
    resolver.parse("uuid | user | group").map(_.label) should be(None)
  }

  it("precedence rules") {
    val resolver = DatatypeResolver(
      enumNames = Seq("age_group", "string"),
      modelNames = Seq("user", "uuid")
    )

    resolver.parse("age_group") should be(Some(Datatype.Singleton(Seq(Type(TypeKind.Enum, "age_group")))))
    resolver.parse("string") should be(Some(Datatype.Singleton(Seq(Type(TypeKind.Primitive, "string")))))
    resolver.parse("age_group | string") should be(Some(Datatype.Singleton(
      Seq(
        Type(TypeKind.Enum, "age_group"),
        Type(TypeKind.Primitive, "string")
      )
    )))

    resolver.parse("user") should be(Some(Datatype.Singleton(Seq(Type(TypeKind.Model, "user")))))
    resolver.parse("uuid") should be(Some(Datatype.Singleton(Seq(Type(TypeKind.Primitive, "uuid")))))
    resolver.parse("user | uuid") should be(Some(Datatype.Singleton(
      Seq(
        Type(TypeKind.Model, "user"),
        Type(TypeKind.Primitive, "uuid")
      )
    )))


  }

}
