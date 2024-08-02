package db

import java.util.UUID

import io.apibuilder.api.v0.models.{UserForm, UserUpdateForm}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class InternalUsersDaoSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers with helpers.ValidatedTestHelpers {

  private def makeUserForm(
    email: String = "test-user-" + UUID.randomUUID.toString + "@test.apibuilder.io",
    nickname: Option[String] = None
  ): UserForm = UserForm(
    email = email,
    password = UUID.randomUUID.toString,
    nickname = nickname
  )

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
    val user = createUser(form)
    userPasswordsDao.isValid(user.guid, "testing") must be(true)
    userPasswordsDao.isValid(user.guid, "password") must be(false)
  }

  "validate" must {

    def expectInvalidNewUser(form: UserForm): Seq[String] = {
      expectInvalid(
        usersDao.validateNewUser(form)
      ).map(_.message)
    }

    "email is valid" in {
      expectInvalidNewUser(
        UserForm(
          email = "bad-email",
          password = "testing"
        )
      ) must be(Seq("Email must have an '@' symbol"))
    }

    "email is unique" in {
      val form = UserForm(
        email = "test-user-" + UUID.randomUUID.toString + "@test.apibuilder.io",
        password = "testing"
      )

      expectValid {
        usersDao.validateNewUser(form)
      }

      val user = createUser(form)

      expectInvalidNewUser(form) must be(Seq("User with this email address already exists"))
      expectValid {
        usersDao.validateNewUser(form.copy(email = "other-" + form.email))
      }

      val updateForm = UserUpdateForm(
        email = form.email,
        nickname = UUID.randomUUID.toString
      )

      expectValid {
        usersDao.validate(updateForm, existingUser = Some(user))
      }
      expectInvalid {
        usersDao.validate(updateForm, existingUser = Some(upsertUser())) 
      }.map(_.message) must be(Seq("User with this email address already exists"))
    }

    "password" in {
      val form = UserForm(
        email = "test-user-" + UUID.randomUUID.toString + "@test.apibuilder.io",
        password = "bad"
      )

      expectInvalidNewUser(form) must be(Seq("Password must be at least 5 characters"))
    }

    "email in upper case with whitespace" in {
      val email = "TEST-user-" + UUID.randomUUID.toString + "@test.apibuilder.io"
      val form = UserForm(
        email = " " + email,
        password = "testing"
      )

      expectValid {
        usersDao.validateNewUser(form)
      }

      val user = createUser(form)
      user.email must be(email.toLowerCase)

      expectInvalidNewUser(
        form.copy(email = email.toUpperCase)
      ) must be(
        Seq("User with this email address already exists")
      )
    }
    
    "nickname is url friendly" in {
      expectInvalidNewUser(makeUserForm(nickname = Some("bad nickname"))) must be(
        Seq("Key must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid key would be: bad-nickname")
      )
    }

    "nickname is unique" in {
      val form = UserForm(
        email = "test-user-" + UUID.randomUUID.toString + "@test.apibuilder.io",
        password = "testing",
        nickname = Some(UUID.randomUUID.toString)
      )

      expectValid {
        usersDao.validateNewUser(form)
      }
      expectValid {
        usersDao.validateNewUser(form.copy(nickname = form.nickname.map(n => "other-" + n)))
      }

      val user = createUser(form)

      val formWithUniqueEmail = UserUpdateForm(
        email = "other-" + form.email,
        nickname = user.nickname,
        name = form.name
      )

      expectInvalid {
        usersDao.validate(formWithUniqueEmail)
      }.map(_.message) must be(Seq("User with this nickname already exists"))
      expectValid {
        usersDao.validate(formWithUniqueEmail, existingUser = Some(user))
      }
      expectInvalid {
        usersDao.validate(formWithUniqueEmail, existingUser = Some(upsertUser()))
      }.map(_.message) must be(Seq("User with this nickname already exists"))
    }

  }

  "generateNickname" must {

    "defaults to a unique name based on email" in {
      val base = UUID.randomUUID.toString
      val email1 = base + "@test1.apibuilder.io"
      val email2 = base + "@test2.apibuilder.io"

      usersDao.generateNickname(email1) must be(base)
      val user = createUser(makeUserForm(email1))
      user.nickname must be(base)

      usersDao.generateNickname(email2) must be(base + "-2")
      val user2 = createUser(makeUserForm(email2))
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
