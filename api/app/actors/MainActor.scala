package actors

import java.util.UUID

import akka.actor._
import db.VersionsDao
import lib.Role
import play.api.Mode

import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.util.{Failure, Success, Try}

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
  versionsDao: VersionsDao,
  @javax.inject.Named("email-actor") emailActor: akka.actor.ActorRef,
  @javax.inject.Named("generator-service-actor") generatorServiceActor: akka.actor.ActorRef,
  @javax.inject.Named("task-actor") taskActor: akka.actor.ActorRef,
  @javax.inject.Named("user-actor") userActor: akka.actor.ActorRef
) extends Actor with ActorLogging with ErrorHandler {

  private[this] implicit val ec = system.dispatchers.lookup("main-actor-context")

  private[this] case object Startup

  system.scheduler.scheduleOnce(FiniteDuration(5, SECONDS)) {
    self ! Startup
  }

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

    case m @ Startup => withVerboseErrorHandler(m) {
      app.mode match {
        case Mode.Test => {
          // No-op
        }
        case Mode.Prod | Mode.Dev => {
          ensureServices()
        }
      }
    }

    case m: Any => logUnhandledMessage(m)
  }

  private[this] def ensureServices(): Unit = {
    log.info("[MainActor] Starting ensureServices()")

    Try {
      versionsDao.migrate()
    } match {
      case Success(result) => log.info("ensureServices() completed: " + result)
      case Failure(ex) => log.error(s"Error migrating versions: ${ex.getMessage}")
    }
  }
}
