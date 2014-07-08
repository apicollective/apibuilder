package db

import org.scalatest.{ FunSpec, Matchers }
import java.util.UUID
import core.Role

class UserDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  it("upsert") {
    val user1 = Util.upsertUser("michael@mailinator.com")
    val user2 = Util.upsertUser("michael@mailinator.com")
    user1.guid should be(user2.guid)
  }

  it("create different records for different emails") {
    val user1 = Util.upsertUser("michael@mailinator.com")
    val user2 = Util.upsertUser("other@mailinator.com")
    user1.guid should not be(user2.guid)
  }

  it("return empty list without error guid is not a valid format") {
    UserDao.findByGuid("ASDF") should be(None)
  }

  it("findByGuid") {
    val user = Util.upsertUser("michael@mailinator.com")
    UserDao.findByGuid(UUID.randomUUID.toString) should be(None)
    UserDao.findByGuid(user.guid) should be(Some(user))
  }

  it("user can login after creation") {
    val form = UserForm(
      email = UUID.randomUUID.toString + "@gilttest.com",
      password = "testing"
    )
    val user = UserDao.create(form)
    val guid = UUID.fromString(user.guid)
    UserPasswordDao.isValid(guid, "testing") should be(true)
    UserPasswordDao.isValid(guid, "password") should be(false)
  }

  describe("users and orgs") {

    it("are linked if the email domain matches") {
      val gilt = Util.gilt
      val user = UserDao.create(UserForm(
        email = UUID.randomUUID.toString + "@gilt.com",
        password = "testing"
      ))

      Membership.findByOrganizationAndUserAndRole(gilt, user, Role.Member).isEmpty should be(false)
      Membership.findByOrganizationAndUserAndRole(gilt, user, Role.Admin) should be(None)
    }

    it("are not linked if the email domain is different") {
      val gilt = Util.gilt
      val user = UserDao.create(UserForm(
        email = UUID.randomUUID.toString + "@gilttest.com",
        password = "testing"
      ))

      Membership.findByOrganizationAndUserAndRole(gilt, user, Role.Member) should be(None)
      Membership.findByOrganizationAndUserAndRole(gilt, user, Role.Admin) should be(None)
    }


  }

}
