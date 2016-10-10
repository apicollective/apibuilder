package db

import org.scalatest.FlatSpec
import org.junit.Assert._
import java.util.UUID

class UserPasswordsDaoSpec extends FlatSpec with util.TestApplication {

  private[this] val user = Util.upsertUser("michael@mailinator.com")

  it should "have distinct keys for all algorithms" in {
    val keys = PasswordAlgorithm.All.map(_.key.toLowerCase)
    assertEquals(keys, keys.distinct.sorted)
  }

  it should "findByUserGuid" in {
    userPasswordsDao.create(user, user.guid, "password")

    val up = userPasswordsDao.findByUserGuid(user.guid).get
    assertEquals(up.userGuid, user.guid)
    assertEquals(up.algorithm, PasswordAlgorithm.Latest)
  }

  it should "validate matching passwords" in {
    userPasswordsDao.create(user, user.guid, "password")
    assertEquals(userPasswordsDao.isValid(user.guid, "password"), true)
    assertEquals(userPasswordsDao.isValid(user.guid, "password2"), false)
    assertEquals(userPasswordsDao.isValid(user.guid, "test"), false)
    assertEquals(userPasswordsDao.isValid(user.guid, ""), false)

    userPasswordsDao.create(user, user.guid, "testing")
    assertEquals(userPasswordsDao.isValid(user.guid, "password"), false)
    assertEquals(userPasswordsDao.isValid(user.guid, "password2"), false)
    assertEquals(userPasswordsDao.isValid(user.guid, "testing"), true)
    assertEquals(userPasswordsDao.isValid(user.guid, ""), false)
  }

  it should "hash the password" in {
    userPasswordsDao.create(user, user.guid, "password")
    val up = userPasswordsDao.findByUserGuid(user.guid).get
    assertEquals(-1, up.hash.indexOf("password"))
  }

  it should "generates unique hashes, even for same password" in {
    PasswordAlgorithm.All.filter { _ != PasswordAlgorithm.Unknown }.foreach { algo =>
      assertNotEquals(algo.hash("password"), algo.hash("password"))
    }
  }

}
