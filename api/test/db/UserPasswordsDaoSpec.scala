package db

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

class UserPasswordsDaoSpec extends PlaySpec with OneAppPerSuite with util.Daos {

  private[this] val user = Util.upsertUser("michael@mailinator.com")

  it "have distinct keys for all algorithms" in {
    val keys = PasswordAlgorithm.All.map(_.key.toLowerCase)
    keys must equal(keys.distinct.sorted)
  }

  it "findByUserGuid" in {
    userPasswordsDao.create(user, user.guid, "password")

    val up = userPasswordsDao.findByUserGuid(user.guid).get
    up.userGuid must equal(user.guid)
    up.algorithm must equal(PasswordAlgorithm.Latest)
  }

  it "validate matching passwords" in {
    userPasswordsDao.create(user, user.guid, "password")
    userPasswordsDao.isValid(user.guid, "password") must equal(true)
    userPasswordsDao.isValid(user.guid, "password2") must equal(false)
    userPasswordsDao.isValid(user.guid, "test") must equal(false)
    userPasswordsDao.isValid(user.guid, "") must equal(false)

    userPasswordsDao.create(user, user.guid, "testing")
    userPasswordsDao.isValid(user.guid, "password") must equal(false)
    userPasswordsDao.isValid(user.guid, "password2") must equal(false)
    userPasswordsDao.isValid(user.guid, "testing") must equal(true)
    userPasswordsDao.isValid(user.guid, "") must equal(false)
  }

  it "hash the password" in {
    userPasswordsDao.create(user, user.guid, "password")
    val up = userPasswordsDao.findByUserGuid(user.guid).get
    -1 must equal(up.hash.indexOf("password"))
  }

  it "generates unique hashes, even for same password" in {
    PasswordAlgorithm.All.filter { _ != PasswordAlgorithm.Unknown }.foreach { algo =>
      algo.hash("password") != algo.hash("password") must be(true)
    }
  }

}
