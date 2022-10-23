package actors

import akka.actor.{ActorLogging, ActorSystem}
import lib.Pager
import db.Authorization
import db.generators.ServicesDao
import play.api.{Environment, Logger, Mode}
import akka.actor.Actor

import java.util.UUID
import util.GeneratorServiceUtil

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object GeneratorServiceActor {

  object Messages {
    case class GeneratorServiceCreated(guid: UUID)
    case object SyncAll
  }

}

@javax.inject.Singleton
class GeneratorServiceActor @javax.inject.Inject() (
  system: ActorSystem,
  servicesDao: ServicesDao,
  util: GeneratorServiceUtil,
  env: Environment
) extends Actor with ActorLogging with ErrorHandler {

  private[this] implicit val ec = system.dispatchers.lookup("generator-service-actor-context")
  private[this] val inTestEnv = env.mode match {
    case Mode.Test => true
    case Mode.Dev | Mode.Prod => false
  }

  system.scheduler.scheduleAtFixedRate(1.hour, 1.hour, self, GeneratorServiceActor.Messages.SyncAll)

  def receive = {

    case m @ GeneratorServiceActor.Messages.GeneratorServiceCreated(guid) => withVerboseErrorHandler(m) {
      util.sync(guid)
    }

    case m @ GeneratorServiceActor.Messages.SyncAll => withVerboseErrorHandler(m) {
      Pager.eachPage { offset =>
        servicesDao.findAll(
          Authorization.All,
          limit = 200,
          offset = offset
        )
      } { service =>
        Try {
          util.sync(service)
        } match {
          case Success(_) => {
            log.info(s"[GeneratorServiceActor] Service[${service.guid}] at uri[${service.uri}] synced")
          }
          case Failure(ex) => {
            ex match {
              case _: java.net.UnknownHostException if inTestEnv => // ignore
              case _ => log.error(s"[GeneratorServiceActor] Service[${service.guid}] at uri[${service.uri}] failed to sync: ${ex.getMessage}", ex)
            }
          }
        }
      }
    }

    case m: Any => logUnhandledMessage(m)      
  }

}

