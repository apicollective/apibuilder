package db

import com.mbryzek.cipher.Ciphers
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class InternalUserPasswordsDaoSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers {

  private lazy val user = upsertUser("michael@mailinator.com")

  "findByUserGuid" in {
    userPasswordsDao.create(user, user.guid, "password")

    val up = userPasswordsDao.findByUserGuid(user.guid).get
    up.userGuid must equal(user.guid)
    up.algorithmKey must equal(Ciphers().latest.key)
  }

  "validate matching passwords" in {
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

  "hash the password" in {
    userPasswordsDao.create(user, user.guid, "password")
    val up = userPasswordsDao.findByUserGuid(user.guid).get
    -1 must equal(up.base64EncodedHash.indexOf("password"))
  }

}
