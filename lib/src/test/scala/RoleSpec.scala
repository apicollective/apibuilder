package lib

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class RoleSpec extends FunSpec with Matchers {

  it("fromString") {
    Role.fromString(Role.Admin.key) should be(Some(Role.Admin))
    Role.fromString(Role.Admin.key.toUpperCase) should be(Some(Role.Admin))
    Role.fromString(Role.Admin.key.toLowerCase) should be(Some(Role.Admin))

    Role.fromString(Role.Member.key) should be(Some(Role.Member))
    Role.fromString(Role.Member.key.toUpperCase) should be(Some(Role.Member))
    Role.fromString(Role.Member.key.toLowerCase) should be(Some(Role.Member))

    Role.fromString("other") should be(None)
  }

}
