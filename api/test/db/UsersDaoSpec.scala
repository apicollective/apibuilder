package db

import org.scalatest.{FunSpec, Matchers}
import java.util.UUID
import lib.Role

class UsersDaoSpec extends FunSpec with Matchers {

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
    UsersDao.findByGuid("ASDF") should be(None)
  }

  it("findByGuid") {
    val user = Util.upsertUser("michael@mailinator.com")
    UsersDao.findByGuid(UUID.randomUUID.toString) should be(None)
    UsersDao.findByGuid(user.guid) should be(Some(user))
  }

  it("user can login after creation") {
    val form = UserForm(
      email = UUID.randomUUID.toString + "@gilttest.com",
      password = "testing"
    )
    val user = UsersDao.create(form)
    UserPasswordsDao.isValid(user.guid, "testing") should be(true)
    UserPasswordsDao.isValid(user.guid, "password") should be(false)
  }

  describe("users and orgs") {

    it("are linked if the email domain matches") {
      val gilt = Util.gilt

      if (gilt.domains.find(_.name == "gilt.com").isEmpty) {
        OrganizationDomainsDao.create(Util.createdBy, gilt, "gilt.com")
      }

      val user = UsersDao.create(UserForm(
        email = UUID.randomUUID.toString + "@gilt.com",
        password = "testing"
      ))

      MembershipRequestsDao.findByOrganizationAndUserAndRole(Authorization.All, gilt, user, Role.Member).isEmpty should be(false)
      MembershipRequestsDao.findByOrganizationAndUserAndRole(Authorization.All, gilt, user, Role.Admin) should be(None)
    }

    it("are not linked if the email domain is different") {
      val gilt = Util.gilt
      val user = UsersDao.create(UserForm(
        email = UUID.randomUUID.toString + "@gilttest.com",
        password = "testing"
      ))

      MembershipsDao.findByOrganizationAndUserAndRole(Authorization.All, gilt, user, Role.Member) should be(None)
      MembershipsDao.findByOrganizationAndUserAndRole(Authorization.All, gilt, user, Role.Admin) should be(None)
    }


  }

}
