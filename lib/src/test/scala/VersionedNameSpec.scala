package lib

import org.scalatest.{FunSpec, Matchers}

class VersionedNameSpec extends FunSpec with Matchers {

  it("label") {
    VersionedName("user").label should be("user")
    VersionedName("user", Some("1.0.0")).label should be("user:1.0.0")
    VersionedName("user", Some("latest")).label should be("user:latest")
  }

  it("orders") {
    val a = VersionedName("user", Some("latest"))
    val b = VersionedName("user", Some("1.0.0"))
    val c = VersionedName("user", Some("1.0.1"))
    val d = VersionedName("user")
    val e = VersionedName("user")

    Seq(a, b, c, d, e).sorted should be(Seq(a, b, c, d, e))
    Seq(e, d, c, b, a).sorted should be(Seq(a, b, c, d, e))

    d.compare(e) should be(0)
  }

}
