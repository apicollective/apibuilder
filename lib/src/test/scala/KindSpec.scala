package lib

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class KindSpec extends AnyFunSpec with Matchers {

  private val resolver = DatatypeResolver(
    enumNames = Nil,
    interfaceNames = Nil,
    modelNames = Nil,
    unionNames = Nil
  )

  it("primitives") {
    resolver.parse("string").map(_.toString) should be(Some("string"))
    resolver.parse("long").map(_.toString) should be(Some("long"))
    resolver.parse("uuid").map(_.toString) should be(Some("uuid"))
    resolver.parse("unit").map(_.toString) should be(Some("unit"))
    resolver.parse("integer").map(_.toString) should be(Some("integer"))
    resolver.parse("date-time-iso8601").map(_.toString) should be(Some("date-time-iso8601"))

    resolver.parse("[string]").map(_.toString) should be(Some("[string]"))
    resolver.parse("[long]").map(_.toString) should be(Some("[long]"))
    resolver.parse("[uuid]").map(_.toString) should be(Some("[uuid]"))
    resolver.parse("[unit]").map(_.toString) should be(Some("[unit]"))
    resolver.parse("[integer]").map(_.toString) should be(Some("[integer]"))
    resolver.parse("[date-time-iso8601]").map(_.toString) should be(Some("[date-time-iso8601]"))

    resolver.parse("map").map(_.toString) should be(Some("map[string]"))
    resolver.parse("map[string]").map(_.toString) should be(Some("map[string]"))
    resolver.parse("map[long]").map(_.toString) should be(Some("map[long]"))
    resolver.parse("map[uuid]").map(_.toString) should be(Some("map[uuid]"))
    resolver.parse("map[unit]").map(_.toString) should be(Some("map[unit]"))
    resolver.parse("map[integer]").map(_.toString) should be(Some("map[integer]"))
    resolver.parse("map[date-time-iso8601]").map(_.toString) should be(Some("map[date-time-iso8601]"))

    resolver.parse("user") should be(None)
  }

  it("requires a concerete type") {
    resolver.parse("[]") should be(None)
    resolver.parse("map[[]]") should be(None)
  }

  it("with enums and models") {
    val resolver = DatatypeResolver(
      enumNames = Seq("age_group"),
      interfaceNames = Nil,
      modelNames = Seq("user"),
      unionNames = Nil,
    )

    resolver.parse("map[age_group]").map(_.toString) should be(Some("map[age_group]"))
    resolver.parse("map[[age_group]]").map(_.toString) should be(Some("map[[age_group]]"))
    resolver.parse("map[map[age_group]]").map(_.toString) should be(Some("map[map[age_group]]"))
    resolver.parse("user").map(_.toString) should be(Some("user"))

    resolver.parse("group").map(_.toString) should be(None)
  }

  it("precedence rules") {
    val resolver = DatatypeResolver(
      enumNames = Seq("age_group", "string", "other_enum"),
      interfaceNames = Seq("visitor"),
      modelNames = Seq("guest_user", "registered_user", "uuid"),
      unionNames = Seq("user", "other_enum")
    )

    resolver.parse("age_group") should be(Some(Kind.Enum("age_group")))
    resolver.parse("other_enum") should be(Some(Kind.Enum("other_enum")))
    resolver.parse("string") should be(Some(Kind.Primitive("string")))
    resolver.parse("visitor") should be(Some(Kind.Interface("visitor")))
    resolver.parse("guest_user") should be(Some(Kind.Model("guest_user")))
    resolver.parse("registered_user") should be(Some(Kind.Model("registered_user")))
    resolver.parse("uuid") should be(Some(Kind.Primitive("uuid")))
    resolver.parse("user") should be(Some(Kind.Union("user")))
  }

}
