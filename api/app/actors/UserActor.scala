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

}

@javax.inject.Singleton
class UserActor @javax.inject.Inject() (
  system: ActorSystem,
  usersDao: UsersDao
) extends Actor with ActorLogging with ErrorHandler {

  private[this] implicit val ec = system.dispatchers.lookup("user-actor-context")

  def receive = {

    case m @ UserActor.Messages.UserCreated(guid) => withVerboseErrorHandler(m) {
      usersDao.processUserCreated(guid)
    }

    case m: Any => logUnhandledMessage(m)

  }

}
