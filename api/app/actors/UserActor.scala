package actors

import akka.actor.{Actor, ActorLogging, ActorSystem}
import db.{EmailVerificationsDao, MembershipRequestsDao, OrganizationsDao, UsersDao}
import lib.Role
import play.api.Logger
import java.util.UUID

object UserActor {

  object Messages {
    case class UserCreated(guid: UUID)
  }

  def userCreated(guid: UUID) {
    usersDao.findByGuid(guid).map { user =>
      organizationsDao.findByEmailDomain(user.email).foreach { org =>
        membershipRequestsDao.upsert(user, org, user, Role.Member)
      }

      emailVerificationsDao.create(user, user, user.email)
    }
  }

}

@javax.inject.Singleton
class UserActor @javax.inject.Inject() (
  system: ActorSystem
) extends Actor with ActorLogging with ErrorHandler {

  implicit val ec = system.dispatchers.lookup("user-actor-context")

  def receive = {

    case m @ UserActor.Messages.UserCreated(guid) => withVerboseErrorHandler(m) {
      UserActor.userCreated(guid)
    }

    case m: Any => logUnhandledMessage(m)

  }
}
