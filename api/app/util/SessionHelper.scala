package util

import javax.inject.Inject
import io.apibuilder.apidoc.api.v0.models.{Authentication, User}
import org.joda.time.DateTime

class SessionHelper @Inject() (
  sessionsDao: db.generated.SessionsDao,
  usersDao: db.UsersDao
) {

  private[this] val DefaultSessionExpirationHours = 24 * 30

  def createAuthentication(u: User): Authentication = {
    val id = SessionIdGenerator.generate()

    sessionsDao.insert(
      usersDao.AdminUser.guid,
      _root_.db.generated.SessionForm(
        id = id,
        userGuid = u.guid,
        expiresAt = DateTime.now().plusHours(DefaultSessionExpirationHours)
      )
    )

    val dbSession = sessionsDao.findById(id).getOrElse {
      sys.error("Failed to create session")
    }

    Conversions.toAuthentication(dbSession, u)
  }
  
}
