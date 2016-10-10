package db

import com.bryzek.apidoc.api.v0.models.{UserForm, UserUpdateForm}
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID
import lib.Role

class UsersDaoSpec extends FunSpec with Matchers with util.TestApplication {

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
    usersDao.findByGuid("ASDF") should be(None)
  }

  it("findByGuid") {
    val user = Util.upsertUser("michael@mailinator.com")
    usersDao.findByGuid(UUID.randomUUID.toString) should be(None)
    usersDao.findByGuid(user.guid) should be(Some(user))
  }

  it("user can login after creation") {
    val form = UserForm(
      email = UUID.randomUUID.toString + "@test.apidoc.me",
      password = "testing"
    )
    val user = usersDao.create(form)
    userPasswordsDao.isValid(user.guid, "testing") should be(true)
    userPasswordsDao.isValid(user.guid, "password") should be(false)
  }

  describe("validate") {

    it("email is valid") {
      usersDao.validateNewUser(
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

      usersDao.validateNewUser(form) should be(Seq.empty)

      val user = usersDao.create(form)

      usersDao.validateNewUser(form).map(_.message) should be(Seq("User with this email address already exists"))
      usersDao.validateNewUser(form.copy(email = "other-" + form.email)).map(_.message) should be(Seq.empty)

      val updateForm = UserUpdateForm(
        email = form.email,
        nickname = UUID.randomUUID.toString
      )

      usersDao.validate(updateForm, existingUser = Some(user)).map(_.message) should be(Seq.empty)
      usersDao.validate(updateForm, existingUser = Some(Util.upsertUser())).map(_.message) should be(Seq("User with this email address already exists"))
    }

    it("password") {
      val form = UserForm(
        email = "test-user-" + UUID.randomUUID.toString + "@test.apidoc.me",
        password = "bad"
      )

      usersDao.validateNewUser(form).map(_.message) should be(Seq("Password must be at least 5 characters"))
    }

    it("email in upper case with whitespace") {
      val email = "TEST-user-" + UUID.randomUUID.toString + "@test.apidoc.me"
      val form = UserForm(
        email = " " + email,
        password = "testing"
      )

      usersDao.validateNewUser(form) should be(Seq.empty)

      val user = usersDao.create(form)
      user.email should be(email.toLowerCase)

      usersDao.validateNewUser(
        form.copy(email = email.toUpperCase)
      ).map(_.message) should be(
        Seq("User with this email address already exists")
      )
    }
    
    it("nickname is url friendly") {
      usersDao.validateNewUser(createUserForm(nickname = Some("bad nickname"))).map(_.message) should be(
        Seq("Key must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid key would be: bad-nickname")
      )
    }

    it("nickname is unique") {
      val form = UserForm(
        email = "test-user-" + UUID.randomUUID.toString + "@test.apidoc.me",
        password = "testing",
        nickname = Some(UUID.randomUUID.toString)
      )

      usersDao.validateNewUser(form) should be(Seq.empty)
      usersDao.validateNewUser(form.copy(nickname = form.nickname.map(n => "other-" + n))).map(_.message) should be(Seq.empty)

      val user = usersDao.create(form)

      val formWithUniqueEmail = UserUpdateForm(
        email = "other-" + form.email,
        nickname = user.nickname,
        name = form.name
      )

      usersDao.validate(formWithUniqueEmail).map(_.message) should be(Seq("User with this nickname already exists"))
      usersDao.validate(formWithUniqueEmail, existingUser = Some(user)).map(_.message) should be(Seq.empty)
      usersDao.validate(formWithUniqueEmail, existingUser = Some(Util.upsertUser())).map(_.message) should be(Seq("User with this nickname already exists"))
    }

  }

  describe("generateNickname") {

    it("defaults to a unique name based on email") {
      val base = UUID.randomUUID.toString
      val email1 = base + "@test1.apidoc.me"
      val email2 = base + "@test2.apidoc.me"

      usersDao.generateNickname(email1) should be(base)
      val user = usersDao.create(createUserForm(email1))
      user.nickname should be(base)

      usersDao.generateNickname(email2) should be(base + "-2")
      val user2 = usersDao.create(createUserForm(email2))
      user2.nickname should be(base + "-2")
    }

    it("generateNickname creates a url valid token") {
      usersDao.generateNickname("  with a SPACE  ") should be("with-a-space")
    }

  }

  it("update") {
    val user = Util.createRandomUser()
    val email = "test-email-2@" + UUID.randomUUID.toString
    val nickname = user.nickname + "-2"
    val name = "Test User " + UUID.randomUUID.toString

    usersDao.update(Util.createdBy, user, UserUpdateForm(
      email = " " + email + " ",
      nickname = nickname,
      name = Some(name)
    ))

    val updated = usersDao.findByGuid(user.guid).get
    updated.email should be(email)
    updated.nickname should be(nickname)
    updated.name should be(Some(name))
  }

}
