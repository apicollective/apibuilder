package db

import java.util.UUID

import io.apibuilder.api.v0.models.{UserForm, UserUpdateForm}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class UsersDaoSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers {

  def createUserForm(
    email: String = "test-user-" + UUID.randomUUID.toString + "@test.apibuilder.io",
    nickname: Option[String] = None
  ) = UserForm(email = email, password = UUID.randomUUID.toString, nickname = nickname)

  "upsert" in {
    val user1 = upsertUser("michael@mailinator.com")
    val user2 = upsertUser("michael@mailinator.com")
    user1.guid must be(user2.guid)
  }

  "create different records for different emails" in {
    val user1 = upsertUser("michael@mailinator.com")
    val user2 = upsertUser("other@mailinator.com")
    user1.guid != user2.guid must be(true)
  }

  "return empty list without error guid is not a valid format" in {
    usersDao.findByGuid("ASDF") must be(None)
  }

  "findByGuid" in {
    val user = upsertUser("michael@mailinator.com")
    usersDao.findByGuid(UUID.randomUUID.toString) must be(None)
    usersDao.findByGuid(user.guid) must be(Some(user))
  }

  "user can login after creation" in {
    val form = UserForm(
      email = UUID.randomUUID.toString + "@test.apibuilder.io",
      password = "testing"
    )
    val user = usersDao.create(form)
    userPasswordsDao.isValid(user.guid, "testing") must be(true)
    userPasswordsDao.isValid(user.guid, "password") must be(false)
  }

  "validate" must {

    "email is valid" in {
      usersDao.validateNewUser(
        UserForm(
          email = "bad-email",
          password = "testing"
        )
      ).map(_.message) must be(Seq("Invalid email address"))
    }

    "email is unique" in {
      val form = UserForm(
        email = "test-user-" + UUID.randomUUID.toString + "@test.apibuilder.io",
        password = "testing"
      )

      usersDao.validateNewUser(form) must be(Nil)

      val user = usersDao.create(form)

      usersDao.validateNewUser(form).map(_.message) must be(Seq("User with this email address already exists"))
      usersDao.validateNewUser(form.copy(email = "other-" + form.email)).map(_.message) must be(Nil)

      val updateForm = UserUpdateForm(
        email = form.email,
        nickname = UUID.randomUUID.toString
      )

      usersDao.validate(updateForm, existingUser = Some(user)).map(_.message) must be(Nil)
      usersDao.validate(updateForm, existingUser = Some(upsertUser())).map(_.message) must be(Seq("User with this email address already exists"))
    }

    "password" in {
      val form = UserForm(
        email = "test-user-" + UUID.randomUUID.toString + "@test.apibuilder.io",
        password = "bad"
      )

      usersDao.validateNewUser(form).map(_.message) must be(Seq("Password must be at least 5 characters"))
    }

    "email in upper case with whitespace" in {
      val email = "TEST-user-" + UUID.randomUUID.toString + "@test.apibuilder.io"
      val form = UserForm(
        email = " " + email,
        password = "testing"
      )

      usersDao.validateNewUser(form) must be(Nil)

      val user = usersDao.create(form)
      user.email must be(email.toLowerCase)

      usersDao.validateNewUser(
        form.copy(email = email.toUpperCase)
      ).map(_.message) must be(
        Seq("User with this email address already exists")
      )
    }
    
    "nickname is url friendly" in {
      usersDao.validateNewUser(createUserForm(nickname = Some("bad nickname"))).map(_.message) must be(
        Seq("Key must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid key would be: bad-nickname")
      )
    }

    "nickname is unique" in {
      val form = UserForm(
        email = "test-user-" + UUID.randomUUID.toString + "@test.apibuilder.io",
        password = "testing",
        nickname = Some(UUID.randomUUID.toString)
      )

      usersDao.validateNewUser(form) must be(Nil)
      usersDao.validateNewUser(form.copy(nickname = form.nickname.map(n => "other-" + n))).map(_.message) must be(Nil)

      val user = usersDao.create(form)

      val formWithUniqueEmail = UserUpdateForm(
        email = "other-" + form.email,
        nickname = user.nickname,
        name = form.name
      )

      usersDao.validate(formWithUniqueEmail).map(_.message) must be(Seq("User with this nickname already exists"))
      usersDao.validate(formWithUniqueEmail, existingUser = Some(user)).map(_.message) must be(Nil)
      usersDao.validate(formWithUniqueEmail, existingUser = Some(upsertUser())).map(_.message) must be(Seq("User with this nickname already exists"))
    }

  }

  "generateNickname" must {

    "defaults to a unique name based on email" in {
      val base = UUID.randomUUID.toString
      val email1 = base + "@test1.apibuilder.io"
      val email2 = base + "@test2.apibuilder.io"

      usersDao.generateNickname(email1) must be(base)
      val user = usersDao.create(createUserForm(email1))
      user.nickname must be(base)

      usersDao.generateNickname(email2) must be(base + "-2")
      val user2 = usersDao.create(createUserForm(email2))
      user2.nickname must be(base + "-2")
    }

    "generateNickname creates a url valid token" in {
      usersDao.generateNickname("  with a SPACE  ") must be("with-a-space")
    }

  }

  "update" in {
    val user = createRandomUser()
    val email = "test-email-2@" + UUID.randomUUID.toString
    val nickname = user.nickname + "-2"
    val name = "Test User " + UUID.randomUUID.toString

    usersDao.update(testUser, user, UserUpdateForm(
      email = " " + email + " ",
      nickname = nickname,
      name = Some(name)
    ))

    val updated = usersDao.findByGuid(user.guid).get
    updated.email must be(email)
    updated.nickname must be(nickname)
    updated.name must be(Some(name))
  }

}
