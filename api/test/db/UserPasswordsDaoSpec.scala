package db

import org.scalatest.FlatSpec
import org.junit.Assert._
import java.util.UUID

class UserPasswordsDaoSpec extends FlatSpec {

  new play.core.StaticApplication(new java.io.File("."))

  private val user = Util.upsertUser("michael@mailinator.com")

  it should "have distinct keys for all algorithms" in {
    val keys = PasswordAlgorithm.All.map(_.key.toLowerCase)
    assertEquals(keys, keys.distinct.sorted)
  }

  it should "findByUserGuid" in {
    UserPasswordsDao.create(user, user.guid, "password")

    val up = UserPasswordsDao.findByUserGuid(user.guid).get
    assertEquals(up.userGuid, user.guid)
    assertEquals(up.algorithm, PasswordAlgorithm.Latest)
  }

  it should "validate matching passwords" in {
    UserPasswordsDao.create(user, user.guid, "password")
    assertEquals(UserPasswordsDao.isValid(user.guid, "password"), true)
    assertEquals(UserPasswordsDao.isValid(user.guid, "password2"), false)
    assertEquals(UserPasswordsDao.isValid(user.guid, "test"), false)
    assertEquals(UserPasswordsDao.isValid(user.guid, ""), false)

    UserPasswordsDao.create(user, user.guid, "test")
    assertEquals(UserPasswordsDao.isValid(user.guid, "password"), false)
    assertEquals(UserPasswordsDao.isValid(user.guid, "password2"), false)
    assertEquals(UserPasswordsDao.isValid(user.guid, "test"), true)
    assertEquals(UserPasswordsDao.isValid(user.guid, ""), false)
  }

  it should "hash the password" in {
    UserPasswordsDao.create(user, user.guid, "password")
    val up = UserPasswordsDao.findByUserGuid(user.guid).get
    assertEquals(-1, up.hash.indexOf("password"))
  }

  it should "generates unique hashes, even for same password" in {
    PasswordAlgorithm.All.filter { _ != PasswordAlgorithm.Unknown }.foreach { algo =>
      assertNotEquals(algo.hash("password"), algo.hash("password"))
    }
  }

}
