package actors

import akka.actor.{Actor, ActorLogging, ActorSystem}
import util.GeneratorServiceUtil

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

sealed trait GeneratorServiceActorMessage
object GeneratorServiceActorMessage {

  case class GeneratorServiceCreated(guid: UUID) extends GeneratorServiceActorMessage
  case object SyncAll extends GeneratorServiceActorMessage

}

@javax.inject.Singleton
class GeneratorServiceActor @javax.inject.Inject() (
  system: ActorSystem,
  processor: GeneratorServiceActorProcessor,
) extends Actor with ActorLogging with ErrorHandler {

  private[this] implicit val ec: ExecutionContext = system.dispatchers.lookup("generator-service-actor-context")

  system.scheduler.scheduleAtFixedRate(1.hour, 1.hour, self, GeneratorServiceActorMessage.SyncAll)

  def receive: Receive = {
    case m: GeneratorServiceActorMessage => processor.processMessage(m)
    case other => logUnhandledMessage(other)
  }

}

class GeneratorServiceActorProcessor @Inject() (
  util: GeneratorServiceUtil
) {
  def processMessage(msg: GeneratorServiceActorMessage)(implicit ec: ExecutionContext): Unit = {
    import GeneratorServiceActorMessage._
    msg match {
      case GeneratorServiceCreated(guid) => util.sync(guid)
      case SyncAll => util.syncAll()
    }
  }
}

