package actors

import akka.actor.{Actor, ActorLogging, ActorSystem}
import lib.Pager
import com.bryzek.apidoc.api.v0.models.{GeneratorForm, GeneratorService}
import com.bryzek.apidoc.generator.v0.Client
import com.bryzek.apidoc.generator.v0.models.Generator
import com.bryzek.apidoc.internal.v0.models.TaskDataSyncService
import db.{Authorization, TasksDao, UsersDao}
import db.generators.{GeneratorsDao, ServicesDao}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._

object GeneratorServiceActor {

  object Messages {
    case class GeneratorServiceCreated(guid: UUID)
    case object Sync
  }

  // TODO: Refactor and inject directly
  private[this] def servicesDao = play.api.Play.current.injector.instanceOf[ServicesDao]
  private[this] def generatorsDao = play.api.Play.current.injector.instanceOf[GeneratorsDao]
  private[this] def usersDao = play.api.Play.current.injector.instanceOf[UsersDao]

  def sync(serviceGuid: UUID)(implicit ec: scala.concurrent.ExecutionContext) {
    servicesDao.findByGuid(Authorization.All, serviceGuid).map { sync(_) }
  }

  def sync(
    service: GeneratorService
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ) {
    val client = new Client(service.uri)

    var iteration = 0
    val limit = 100
    var num = limit
    var offset = 0

    while (num >= limit) {
      val generators = Await.result(
        client.generators.get(limit = limit, offset = offset),
        5000.millis
      )
      num = generators.size
      offset += limit
      iteration += 1
      if (iteration > 25) {
        sys.error(s"Likely infinite loop fetching generators for service at URI[${service.uri}]")
      }

      generators.foreach { gen =>
        generatorsDao.upsert(
          usersDao.AdminUser,
          GeneratorForm(
            serviceGuid = service.guid,
            generator = gen
          )
        ) match {
          case Left(errors) => Logger.error("Error fetching generators for service[${service.guid}] uri[${service.uri}]: " + errors.mkString(", "))
          case Right(_) => {}
        }
      }
    }
  }

}

@javax.inject.Singleton
class GeneratorServiceActor @javax.inject.Inject() (
  system: ActorSystem,
  usersDao: UsersDao,
  servicesDao: ServicesDao,
  tasksDao: TasksDao
) extends Actor with ActorLogging with ErrorHandler {

  implicit val ec = system.dispatchers.lookup("generator-service-actor-context")

  def receive = {

    case m @ GeneratorServiceActor.Messages.GeneratorServiceCreated(guid) => withVerboseErrorHandler(m) {
      createSyncTask(guid)
    }

    case m @ GeneratorServiceActor.Messages.Sync => withVerboseErrorHandler(m) {
      Pager.eachPage { offset =>
        servicesDao.findAll(
          Authorization.All,
          offset = offset
        )
      } { service =>
        createSyncTask(service.guid)
      }
    }

    case m: Any => logUnhandledMessage(m)      
  }

  def createSyncTask(serviceGuid: UUID) {
    tasksDao.create(usersDao.AdminUser, TaskDataSyncService(serviceGuid))
  }

}

