package db

import com.gilt.apidoc.v0.models.{UserForm, UserUpdateForm}
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID
import lib.Role

class UsersDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  def createUserForm(
    email: String = "test-user-" + UUID.randomUUID.toString + "@test.apidoc.me",
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
      email = UUID.randomUUID.toString + "@test.apidoc.me",
      password = "testing"
    )
    val user = UsersDao.create(form)
    UserPasswordsDao.isValid(user.guid, "testing") should be(true)
    UserPasswordsDao.isValid(user.guid, "password") should be(false)
  }

  describe("validate") {

    it("email is valid") {
      UsersDao.validateNewUser(
        UserForm(
          email = "bad-email",
          password = "testing"
        )
      ).map(_.message) should be(Seq("Invalid email address"))
    }

    it("email is unique") {
      val form = UserForm(
        email = "test-user-" + UUID.randomUUID.toString + "@test.apidoc.me",
        password = "testing"
      )

      UsersDao.validateNewUser(form) should be(Seq.empty)

      val user = UsersDao.create(form)

      UsersDao.validateNewUser(form).map(_.message) should be(Seq("User with this email address already exists"))
      UsersDao.validateNewUser(form.copy(email = "other-" + form.email)).map(_.message) should be(Seq.empty)

      val updateForm = UserUpdateForm(
        email = form.email,
        nickname = UUID.randomUUID.toString
      )

      UsersDao.validate(updateForm, existingUser = Some(user)).map(_.message) should be(Seq.empty)
      UsersDao.validate(updateForm, existingUser = Some(Util.upsertUser())).map(_.message) should be(Seq("User with this email address already exists"))
    }

    it("password") {
      val form = UserForm(
        email = "test-user-" + UUID.randomUUID.toString + "@test.apidoc.me",
        password = "bad"
      )

      UsersDao.validateNewUser(form).map(_.message) should be(Seq("Password must be at least 5 characters"))
    }

    it("nickname is url friendly") {
      UsersDao.validateNewUser(createUserForm(nickname = Some("bad nickname"))).map(_.message) should be(Seq("Key must be in all lower case and contain alphanumerics only. A valid key would be: bad-nickname"))
    }

    it("nickname is unique") {
      val form = UserForm(
        email = "test-user-" + UUID.randomUUID.toString + "@test.apidoc.me",
        password = "testing",
        nickname = Some(UUID.randomUUID.toString)
      )

      UsersDao.validateNewUser(form) should be(Seq.empty)
      UsersDao.validateNewUser(form.copy(nickname = form.nickname.map(n => "other-" + n))).map(_.message) should be(Seq.empty)

      val user = UsersDao.create(form)

      val formWithUniqueEmail = UserUpdateForm(
        email = "other-" + form.email,
        nickname = user.nickname,
        name = form.name
      )

      UsersDao.validate(formWithUniqueEmail).map(_.message) should be(Seq("User with this nickname already exists"))
      UsersDao.validate(formWithUniqueEmail, existingUser = Some(user)).map(_.message) should be(Seq.empty)
      UsersDao.validate(formWithUniqueEmail, existingUser = Some(Util.upsertUser())).map(_.message) should be(Seq("User with this nickname already exists"))
    }

  }

  describe("generateNickname") {

    it("defaults to a unique name based on email") {
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

    it("generateNickname creates a url valid token") {
      UsersDao.generateNickname("  with a SPACE  ") should be("with-a-space")
    }

  }

  it("update") {
    val user = Util.createRandomUser()
    val email = "test-email-2@" + UUID.randomUUID.toString
    val nickname = user.nickname + "-2"
    val name = "Test User " + UUID.randomUUID.toString

    UsersDao.update(Util.createdBy, user, UserUpdateForm(
      email = email,
      nickname = nickname,
      name = Some(name)
    ))

    val updated = UsersDao.findByGuid(user.guid).get
    updated.email should be(email)
    updated.nickname should be(nickname)
    updated.name should be(Some(name))
  }

}
