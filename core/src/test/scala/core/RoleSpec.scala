package core

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class RoleSpec extends AnyFunSpec with Matchers {

  it("fromString") {
    Role.fromString(Role.Admin.key.toString) should be(Some(Role.Admin))
    Role.fromString(Role.Admin.key.toString.toUpperCase) should be(Some(Role.Admin))
    Role.fromString(Role.Admin.key.toString.toLowerCase) should be(Some(Role.Admin))

    Role.fromString(Role.Member.key.toString) should be(Some(Role.Member))
    Role.fromString(Role.Member.key.toString.toUpperCase) should be(Some(Role.Member))
    Role.fromString(Role.Member.key.toString.toLowerCase) should be(Some(Role.Member))

    Role.fromString("other") should be(None)
  }

}
