package lib

import helpers.ServiceHelpers
import io.apibuilder.api.v0.models.{DiffBreaking, DiffNonBreaking}
import io.apibuilder.spec.v0.models.{Attribute, Deprecation, Interface}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json

class ServiceInterfaceDiffSpec extends PlaySpec
  with GuiceOneAppPerSuite
  with ServiceHelpers {

  "no change" in {
    val svc = makeService(
      interfaces = Seq(makeInterface())
    )
    ServiceDiff(svc, svc).differences must be(Nil)
  }

  "add/remove interface" in {
    val svc1 = makeService()
    val svc2 = svc1.copy(
      interfaces = Seq(makeInterface(name = "person"))
    )
    ServiceDiff(svc1, svc2).differences must be(
      Seq(
        DiffNonBreaking("interface added: person")
      )
    )

    ServiceDiff(svc2, svc1).differences must be(
      Seq(
        DiffBreaking("interface removed: person")
      )
    )
  }

  "change interface" in {
    def test(f: Interface => Interface) = {
      val interface = makeInterface(
        name = "person",
        plural = "people",
      )
      val svc = makeService(
        interfaces = Seq(interface),
      )
      val update = svc.copy(
        interfaces = Seq(f(interface)),
      )
      ServiceDiff(svc, update).differences
    }

    test(_.copy(plural = "persons")) must be(
      Seq(
        DiffNonBreaking("interface person plural changed from people to persons")
      )
    )

    test(_.copy(description = Some("test"))) must be(
      Seq(
        DiffNonBreaking("interface person description added: test")
      )
    )

    test(_.copy(deprecation = Some(Deprecation()))) must be(
      Seq(
        DiffNonBreaking("interface person deprecated")
      )
    )
  }

  "fields" in {
    def test(f: Interface => Interface) = {
      val interface = makeInterface(
        name = "person",
        fields = Seq(makeField(name = "id")),
      )
      val svc = makeService(
        interfaces = Seq(interface),
      )
      val update = svc.copy(
        interfaces = Seq(f(interface)),
      )
      ServiceDiff(svc, update).differences
    }

    test(_.copy(fields = Nil)) must be(
      Seq(
        DiffBreaking("interface person field removed: id")
      )
    )

    test { i => i.copy(fields = i.fields ++ Seq(makeField(name = "name", required = false))) } must be(
      Seq(
        DiffNonBreaking("interface person optional field added: name")
      )
    )

    test { i => i.copy(fields = i.fields ++ Seq(makeField(name = "name", required = false, default = Some("test")))) } must be(
      Seq(
        DiffNonBreaking("interface person optional field added: name, defaults to test")
      )
    )

    test { i => i.copy(fields = i.fields ++ Seq(makeField(name = "name", required = true))) } must be(
      Seq(
        DiffBreaking("interface person required field added: name")
      )
    )

    test { i => i.copy(fields = i.fields ++ Seq(makeField(name = "name", required = true, default = Some("test")))) } must be(
      Seq(
        DiffNonBreaking("interface person required field added: name, defaults to test")
      )
    )
  }

  "attributes" in {
    def test(f: Seq[Attribute] => Seq[Attribute]) = {
      val interface = makeInterface(
        name = "person",
        attributes = Seq(
          makeAttribute(name = "test", value = Json.obj("a" -> "b"))
        )
      )
      val svc = makeService(
        interfaces = Seq(interface),
      )
      val update = svc.copy(
        interfaces = Seq(interface.copy(attributes = f(interface.attributes))),
      )
      ServiceDiff(svc, update).differences
    }

    test { _ => Nil } must be(
      Seq(
        DiffNonBreaking("interface person attribute removed: test")
      )
    )

    test { a => a ++ Seq(makeAttribute(name = "foo")) } must be(
      Seq(
        DiffNonBreaking("interface person attribute added: foo")
      )
    )

    test { a => a.map(_.copy(value = Json.obj())) } must be(
      Seq(
        DiffNonBreaking("""interface person attribute 'test' value changed from {"a":"b"} to {}""")
      )
    )

  }

}
