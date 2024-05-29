package actors

import akka.actor._
import db.InternalMigrationsDao
import lib.Role
import play.api.Mode

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, MINUTES, SECONDS}

object MainActor {

  object Messages {
    case class EmailVerificationCreated(guid: UUID)
    case class MembershipRequestCreated(guid: UUID)
    case class MembershipRequestAccepted(organizationGuid: UUID, userGuid: UUID, role: Role)
    case class MembershipRequestDeclined(organizationGuid: UUID, userGuid: UUID, role: Role)
    case class MembershipCreated(guid: UUID)

    case class PasswordResetRequestCreated(guid: UUID)
    case class ApplicationCreated(guid: UUID)
    case class UserCreated(guid: UUID)

    case class TaskCreated(guid: UUID)
    case class GeneratorServiceCreated(guid: UUID)
  }
}

@javax.inject.Singleton
class MainActor @javax.inject.Inject() (
  app: play.api.Application,
  system: ActorSystem,
  internalMigrationsDao: InternalMigrationsDao,
  @javax.inject.Named("email-actor") emailActor: akka.actor.ActorRef,
  @javax.inject.Named("generator-service-actor") generatorServiceActor: akka.actor.ActorRef,
  @javax.inject.Named("task-actor") taskActor: akka.actor.ActorRef,
  @javax.inject.Named("user-actor") userActor: akka.actor.ActorRef
) extends Actor with ActorLogging with ErrorHandler {

  private[this] implicit val ec: ExecutionContext = system.dispatchers.lookup("main-actor-context")

  private[this] case object QueueVersionsToMigrate
  private[this] case object MigrateVersions

  private[this] def scheduleOnce(msg: Any)(implicit delay: FiniteDuration = FiniteDuration(10, SECONDS)): Unit = {
    system.scheduler.scheduleOnce(delay) {
      self ! msg
    }
  }

  scheduleOnce(QueueVersionsToMigrate)
  scheduleOnce(MigrateVersions)

  def receive = akka.event.LoggingReceive {

    case m @ MainActor.Messages.MembershipRequestCreated(guid) => withVerboseErrorHandler(m) {
      emailActor ! EmailActor.Messages.MembershipRequestCreated(guid)
    }

    case m @ MainActor.Messages.MembershipRequestAccepted(organizationGuid, userGuid, role) => withVerboseErrorHandler(m) {
      emailActor ! EmailActor.Messages.MembershipRequestAccepted(organizationGuid, userGuid, role)
    }

    case m @ MainActor.Messages.MembershipRequestDeclined(organizationGuid, userGuid, role) => withVerboseErrorHandler(m) {
      emailActor ! EmailActor.Messages.MembershipRequestDeclined(organizationGuid, userGuid, role)
    }

    case m @ MainActor.Messages.MembershipCreated(guid) => withVerboseErrorHandler(m) {
      emailActor ! EmailActor.Messages.MembershipCreated(guid)
    }

    case m @ MainActor.Messages.ApplicationCreated(guid) => withVerboseErrorHandler(m) {
      emailActor ! EmailActor.Messages.ApplicationCreated(guid)
    }

    case m @ MainActor.Messages.TaskCreated(guid) => withVerboseErrorHandler(m) {
      taskActor ! TaskActor.Messages.Created(guid)
    }

    case m @ MainActor.Messages.GeneratorServiceCreated(guid) => withVerboseErrorHandler(m) {
      generatorServiceActor ! GeneratorServiceActorMessage.GeneratorServiceCreated(guid)
    }

    case m @ MainActor.Messages.EmailVerificationCreated(guid) => withVerboseErrorHandler(m) {
      emailActor ! EmailActor.Messages.EmailVerificationCreated(guid)
    }

    case m @ MainActor.Messages.PasswordResetRequestCreated(guid) => withVerboseErrorHandler(m) {
      emailActor ! EmailActor.Messages.PasswordResetRequestCreated(guid)
    }

    case m @ MainActor.Messages.UserCreated(guid) => withVerboseErrorHandler(m) {
      userActor ! UserActor.Messages.UserCreated(guid)
    }

    case m @ QueueVersionsToMigrate => withVerboseErrorHandler(m) {
      app.mode match {
        case Mode.Test => // No-op
        case Mode.Prod | Mode.Dev => internalMigrationsDao.queueVersions()
      }
    }

    case m @ MigrateVersions => withVerboseErrorHandler(m) {
      app.mode match {
        case Mode.Test => // No-op
        case Mode.Prod | Mode.Dev => {
          if (internalMigrationsDao.migrateBatch(50)) {
            scheduleOnce(MigrateVersions)(FiniteDuration(1, MINUTES))
          }
        }
      }
    }

    case m: Any => logUnhandledMessage(m)
  }
}
