package actors

import akka.actor.{Actor, ActorLogging}
import db.UsersDao

import java.util.UUID
import javax.inject.{Inject, Singleton}

object UserActor {

  object Messages {
    case class UserCreated(guid: UUID)
  }

}

@Singleton
class UserActor @Inject() (
  usersDao: UsersDao
) extends Actor with ActorLogging with ErrorHandler {

  def receive = {

    case m @ UserActor.Messages.UserCreated(guid) => withVerboseErrorHandler(m) {
      usersDao.processUserCreated(guid)
    }

    case m: Any => logUnhandledMessage(m)

  }

}
