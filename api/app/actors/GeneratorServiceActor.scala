package actors

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

  def sync(serviceGuid: UUID)(implicit ec: scala.concurrent.ExecutionContext) {
    ServicesDao.findByGuid(Authorization.All, serviceGuid).map { sync(_) }
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
        GeneratorsDao.upsert(
          UsersDao.AdminUser,
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

class GeneratorServiceActor extends Actor {

  def receive = {

    case GeneratorServiceActor.Messages.GeneratorServiceCreated(guid) => Util.withVerboseErrorHandler(
      s"GeneratorServiceActor.Messages.GeneratorServiceCreated($guid)", {
        createSyncTask(guid)
      }
    )

    case GeneratorServiceActor.Messages.Sync => Util.withVerboseErrorHandler(
      s"GeneratorServiceActor.Messages.Sync", {
        Pager.eachPage { offset =>
          ServicesDao.findAll(
            Authorization.All,
            offset = offset
          )
        } { service =>
          createSyncTask(service.guid)
        }
      }
    )

  }

  def createSyncTask(serviceGuid: UUID) {
    TasksDao.create(UsersDao.AdminUser, TaskDataSyncService(serviceGuid))
  }

}

