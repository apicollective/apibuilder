package db

import org.scalatest.FlatSpec
import org.junit.Assert._
import java.util.UUID

class UserPasswordDaoSpec extends FlatSpec {

  new play.core.StaticApplication(new java.io.File("."))

  private val user = UserDao.upsert("michael@mailinator.com")
  private val userGuid = UUID.fromString(user.guid)

  it should "have distinct keys for all algorithms" in {
    val keys = PasswordAlgorithm.All.map(_.key.toLowerCase)
    assertEquals(keys, keys.distinct.sorted)
  }

  it should "findByUserGuid" in {
    UserPasswordDao.softDeleteByUserGuid(user, userGuid)

    assertEquals(None, UserPasswordDao.findByUserGuid(userGuid))

    UserPasswordDao.create(user, userGuid, "password")

    val up = UserPasswordDao.findByUserGuid(userGuid).get
    assertEquals(up.userGuid.toString, user.guid)
    assertEquals(up.algorithm, PasswordAlgorithm.Latest)
  }

  it should "validate matching passwords" in {
    UserPasswordDao.create(user, userGuid, "password")
    assertEquals(UserPasswordDao.isValid(userGuid, "password"), true)
    assertEquals(UserPasswordDao.isValid(userGuid, "password2"), false)
    assertEquals(UserPasswordDao.isValid(userGuid, "test"), false)
    assertEquals(UserPasswordDao.isValid(userGuid, ""), false)

    UserPasswordDao.create(user, userGuid, "test")
    assertEquals(UserPasswordDao.isValid(userGuid, "password"), false)
    assertEquals(UserPasswordDao.isValid(userGuid, "password2"), false)
    assertEquals(UserPasswordDao.isValid(userGuid, "test"), true)
    assertEquals(UserPasswordDao.isValid(userGuid, ""), false)
  }

  it should "hash the password" in {
    UserPasswordDao.create(user, userGuid, "password")
    val up = UserPasswordDao.findByUserGuid(userGuid).get
    assertEquals(-1, up.hash.indexOf("password"))
  }

  it should "generates unique hashes, even for same password" in {
    PasswordAlgorithm.All.filter { _ != PasswordAlgorithm.Unknown }.foreach { algo =>
      assertNotEquals(algo.hash("password"), algo.hash("password"))
    }
  }

}
