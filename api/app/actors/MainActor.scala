package actors

import lib.{Role}
import play.api.libs.concurrent.Akka
import akka.actor._
import play.api.Logger
import play.api.Play.current
import java.util.UUID
import scala.concurrent.duration._

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
  @javax.inject.Named("email-actor") emailActor: akka.actor.ActorRef,
  @javax.inject.Named("generator-service-actor") generatorServiceActor: akka.actor.ActorRef,
  @javax.inject.Named("task-actor") taskActor: akka.actor.ActorRef,
  @javax.inject.Named("user-actor") userActor: akka.actor.ActorRef
) extends Actor with ActorLogging with ErrorHandler {

  implicit val ec = system.dispatchers.lookup("main-actor-context")

  system.scheduler.schedule(5.seconds, 1.minute, taskActor, TaskActor.Messages.RestartDroppedTasks)
  system.scheduler.schedule(1.hour, 1.hour, taskActor, TaskActor.Messages.PurgeOldTasks)
  system.scheduler.schedule(12.hours, 1.day, taskActor, TaskActor.Messages.NotifyFailed)
  system.scheduler.schedule(1.hour, 1.hour, generatorServiceActor, GeneratorServiceActor.Messages.Sync)

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
      taskActor ! TaskActor.Messages.TaskCreated(guid)
    }

    case m @ MainActor.Messages.GeneratorServiceCreated(guid) => withVerboseErrorHandler(m) {
      generatorServiceActor ! GeneratorServiceActor.Messages.GeneratorServiceCreated(guid)
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

    case m: Any => logUnhandledMessage(m)
  }
}
