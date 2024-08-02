package util

import javax.inject.Inject
import db.{InternalUser, InternalUsersDao}
import io.apibuilder.api.v0.models.{Authentication, User}
import org.joda.time.DateTime
import lib.Constants

class SessionHelper @Inject() (
  sessionsDao: db.generated.SessionsDao,
  conversions: Conversions
) {

  private val DefaultSessionExpirationHours = 24 * 30

  def createAuthentication(u: InternalUser): Authentication = {
    val id = SessionIdGenerator.generate()
    val ts = DateTime.now

    sessionsDao.insert(
      Constants.AdminUserGuid,
      _root_.db.generated.SessionForm(
        id = id,
        userGuid = u.guid,
        expiresAt = ts.plusHours(DefaultSessionExpirationHours)
      )
    )

    val dbSession = sessionsDao.findById(id).getOrElse {
      sys.error("Failed to create session")
    }

    conversions.toAuthentication(dbSession, u)
  }
  
}
