package db

import com.gilt.apidoc.v0.models.UserForm
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID
import lib.Role

class UsersDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  def createUserForm(
    email: String = UUID.randomUUID.toString + "@test.apidoc.me",
    nickname: Option[String] = None
  ) = UserForm(email = email, password = UUID.randomUUID.toString, nickname = nickname)

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

  describe("validate") {

    it("email is unique") {
      val form = UserForm(
        email = UUID.randomUUID.toString + "@gilttest.com",
        password = "testing"
    )

      UsersDao.validate(form) should be(Seq.empty)

      val user = UsersDao.create(form)

      UsersDao.validate(form).map(_.message) should be(Seq("User with this email address already exists"))
      UsersDao.validate(form, Some(user)).map(_.message) should be(Seq.empty)
      UsersDao.validate(form, Some(Util.upsertUser())).map(_.message) should be(Seq("User with this email address already exists"))
      UsersDao.validate(form.copy(email = "other-" + form.email)).map(_.message) should be(Seq.empty)
    }

    it("password") {
      val form = UserForm(
        email = UUID.randomUUID.toString + "@gilttest.com",
        password = "bad"
      )

      UsersDao.validate(form).map(_.message) should be(Seq("Password must be at least 5 characters"))
    }

    it("nickname is url friendly") {
      UsersDao.validate(createUserForm(nickname = Some("bad nickname"))).map(_.message) should be(Seq("Key must be in all lower case and contain alphanumerics only. A valid key would be: bad-nickname"))
    }

  }

  it("generateNickname defaults to a unique name based on email") {
    val base = UUID.randomUUID.toString
    val email1 = base + "@test1.apidoc.me"
    val email2 = base + "@test2.apidoc.me"

    UsersDao.generateNickname(email1) should be(base)
    val user = UsersDao.create(createUserForm(email1))
    user.nickname should be(base)

    UsersDao.generateNickname(email2) should be(base + "-2")
    val user2 = UsersDao.create(createUserForm(email2))
    user2.nickname should be(base + "-2")
  }

}
