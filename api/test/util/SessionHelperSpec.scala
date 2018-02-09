package util

import db.Util
import org.joda.time.DateTime
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

class SessionHelperSpec extends PlaySpec with OneAppPerSuite {

  "createAuthentication" in {
    val user = Util.upsertUser("michael@mailinator.com")
    val auth = sessionHelper.createAuthentication(user)
    auth.user.guid must equal(user.guid)
    auth.session.expiresAt.isBefore(DateTime.now.plusWeeks(6)) must be(true)
    auth.session.expiresAt.isAfter(DateTime.now.plusWeeks(3)) must be(true)
  }

  "can delete session" in {
    val user = Util.upsertUser("michael@mailinator.com")
    val auth = sessionHelper.createAuthentication(user)

    sessionsDao.findById(auth.session.id).get.deletedAt.isEmpty must be(true)

    sessionsDao.deleteById(user.guid, auth.session.id)
    sessionsDao.findById(auth.session.id).get.deletedAt.isEmpty must be(false)
  }

}
