package actors

import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import akka.actor._
import play.api.Logger
import play.api.Play.current
import java.util.UUID

object MainActor {
  def props() = Props(new MainActor("main"))

  object Messages {
    case class MembershipRequestCreated(guid: UUID)
    case class MembershipCreated(guid: UUID)
    case class ServiceCreated(guid: UUID)
    case class VersionCreated(guid: UUID)
  }
}


class MainActor(name: String) extends Actor with ActorLogging {
  import scala.concurrent.duration._

  val emailActor = Akka.system.actorOf(Props[EmailActor], name = s"$name:emailActor")

  def receive = akka.event.LoggingReceive {

    case MainActor.Messages.MembershipRequestCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.MembershipRequestCreated($guid)", {
        emailActor ! EmailActor.Messages.MembershipRequestCreated(guid)
      }
    )

    case MainActor.Messages.MembershipCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.MembershipCreated($guid)", {
        emailActor ! EmailActor.Messages.MembershipCreated(guid)
      }
    )

    case MainActor.Messages.ServiceCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.ServiceCreated($guid)", {
        emailActor ! EmailActor.Messages.ServiceCreated(guid)
      }
    )

    case MainActor.Messages.VersionCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.VersionCreated($guid)", {
        emailActor ! EmailActor.Messages.VersionCreated(guid)
      }
    )

    case m: Any => {
      Logger.error("Main actor got an unhandled message: " + m)
    }

  }
}
