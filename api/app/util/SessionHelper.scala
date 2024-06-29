package util

import javax.inject.Inject

import db.UsersDao
import io.apibuilder.api.v0.models.{Authentication, User}
import org.joda.time.DateTime

class SessionHelper @Inject() (
  sessionsDao: db.generated.SessionsDao
) {

  private val DefaultSessionExpirationHours = 24 * 30

  def createAuthentication(u: User): Authentication = {
    val id = SessionIdGenerator.generate()
    val ts = DateTime.now

    sessionsDao.insert(
      UsersDao.AdminUserGuid,
      _root_.db.generated.SessionForm(
        id = id,
        userGuid = u.guid,
        expiresAt = ts.plusHours(DefaultSessionExpirationHours),
        createdAt = ts,
        createdByGuid = u.guid,
        updatedAt = ts,
        updatedByGuid = u.guid,
        deletedAt = None,
        deletedByGuid = None
      )
    )

    val dbSession = sessionsDao.findById(id).getOrElse {
      sys.error("Failed to create session")
    }

    Conversions.toAuthentication(dbSession, u)
  }
  
}
