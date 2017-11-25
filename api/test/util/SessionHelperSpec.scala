package util

import db.Util
import org.joda.time.DateTime
import org.scalatest.{FunSpec, Matchers}

class SessionHelperSpec extends FunSpec with Matchers with util.TestApplication {

  it("createAuthentication") {
    val user = Util.upsertUser("michael@mailinator.com")
    val auth = sessionHelper.createAuthentication(user)
    auth.user.guid should equal(user.guid)
    auth.session.expiresAt.isBefore(DateTime.now.plusWeeks(6)) should be(true)
    auth.session.expiresAt.isAfter(DateTime.now.plusWeeks(3)) should be(true)
  }

  it("can delete session") {
    val user = Util.upsertUser("michael@mailinator.com")
    val auth = sessionHelper.createAuthentication(user)

    sessionsDao.findById(auth.session.id).get.deletedAt.isEmpty should be(true)

    sessionsDao.deleteById(user.guid, auth.session.id)
    sessionsDao.findById(auth.session.id).get.deletedAt.isEmpty should be(false)
  }

}
