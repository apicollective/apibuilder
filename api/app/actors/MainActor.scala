package actors

import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import akka.actor._
import play.api.Logger
import play.api.Play.current

object MainActor {
  def props() = Props(new MainActor("main"))

  object Messages {
    case class MembershipRequestCreated(id: Long)
  }
}


class MainActor(name: String) extends Actor with ActorLogging {
  import scala.concurrent.duration._

  def receive = akka.event.LoggingReceive {

    case MainActor.Messages.MembershipRequestCreated(id) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.MembershipRequestCreated(id)", {
        Logger.info("TODO")
      }
    )

    case m: Any => {
      Logger.error("Main actor got an unhandled message: " + m)
    }

  }
}
