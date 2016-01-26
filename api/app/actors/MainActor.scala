package actors

import lib.{Role}
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import akka.actor._
import play.api.Logger
import play.api.Play.current
import java.util.UUID

object MainActor {
  def props() = Props(new MainActor("main"))

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


class MainActor(name: String) extends Actor with ActorLogging {
  import scala.concurrent.duration._

  private[this] val emailActor = Akka.system.actorOf(Props[EmailActor], name = s"$name:emailActor")
  private[this] val generatorServiceActor = Akka.system.actorOf(Props[GeneratorServiceActor], name = s"$name:generatorServiceActor")
  private[this] val taskActor = Akka.system.actorOf(Props[TaskActor], name = s"$name:taskActor")
  private[this] val userActor = Akka.system.actorOf(Props[UserActor], name = s"$name:userActor")

  Akka.system.scheduler.schedule(5.seconds, 1.minute, taskActor, TaskActor.Messages.RestartDroppedTasks)
  Akka.system.scheduler.schedule(1.hour, 1.hour, taskActor, TaskActor.Messages.PurgeOldTasks)
  Akka.system.scheduler.schedule(12.hours, 1.day, taskActor, TaskActor.Messages.NotifyFailed)
  Akka.system.scheduler.schedule(1.hour, 1.hour, generatorServiceActor, GeneratorServiceActor.Messages.Sync)

  def receive = akka.event.LoggingReceive {

    case MainActor.Messages.MembershipRequestCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.MembershipRequestCreated($guid)", {
        emailActor ! EmailActor.Messages.MembershipRequestCreated(guid)
      }
    )

    case MainActor.Messages.MembershipRequestAccepted(organizationGuid, userGuid, role) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.MembershipRequestAccepted($organizationGuid, $userGuid, $role)", {
        emailActor ! EmailActor.Messages.MembershipRequestAccepted(organizationGuid, userGuid, role)
      }
    )

    case MainActor.Messages.MembershipRequestDeclined(organizationGuid, userGuid, role) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.MembershipRequestDeclined($organizationGuid, $userGuid, $role)", {
        emailActor ! EmailActor.Messages.MembershipRequestDeclined(organizationGuid, userGuid, role)
      }
    )

    case MainActor.Messages.MembershipCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.MembershipCreated($guid)", {
        emailActor ! EmailActor.Messages.MembershipCreated(guid)
      }
    )

    case MainActor.Messages.ApplicationCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.ApplicationCreated($guid)", {
        emailActor ! EmailActor.Messages.ApplicationCreated(guid)
      }
    )

    case MainActor.Messages.TaskCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.TaskCreated($guid)", {
        taskActor ! TaskActor.Messages.TaskCreated(guid)
      }
    )

    case MainActor.Messages.GeneratorServiceCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.GeneratorServiceCreated($guid)", {
        generatorServiceActor ! GeneratorServiceActor.Messages.GeneratorServiceCreated(guid)
      }
    )

    case MainActor.Messages.EmailVerificationCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.EmailVerificationCreated($guid)", {
        emailActor ! EmailActor.Messages.EmailVerificationCreated(guid)
      }
    )

    case MainActor.Messages.PasswordResetRequestCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.PasswordResetRequestCreated($guid)", {
        emailActor ! EmailActor.Messages.PasswordResetRequestCreated(guid)
      }
    )

    case MainActor.Messages.UserCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.UserCreated($guid)", {
        userActor ! UserActor.Messages.UserCreated(guid)
      }
    )

    case m: Any => {
      Logger.error("Main actor got an unhandled message: " + m)
    }

  }
}
