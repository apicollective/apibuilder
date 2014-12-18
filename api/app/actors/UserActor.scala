package actors

import lib.Role
import db.{EmailVerificationsDao, MembershipRequestsDao, OrganizationsDao, UsersDao}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object UserActor {

  object Messages {
    case class UserCreated(guid: UUID)
  }

  def userCreated(guid: UUID) {
    UsersDao.findByGuid(guid).map { user =>
      OrganizationsDao.findByEmailDomain(user.email).foreach { org =>
        MembershipRequestsDao.upsert(user, org, user, Role.Member)
      }

      EmailVerificationsDao.create(user, user, user.email)
    }
  }

}

class UserActor extends Actor {

  def receive = {

    case UserActor.Messages.UserCreated(guid) => Util.withVerboseErrorHandler(
      s"UserActor.Messages.UserCreated($guid)", {
        UserActor.userCreated(guid)
      }
    )

    case m: Any => {
      Logger.error("Email actor got an unhandled message: " + m)
    }

  }
}

