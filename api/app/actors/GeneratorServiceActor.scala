package actors

import lib.Pager
import com.gilt.apidoc.api.v0.models.GeneratorService
import com.gilt.apidoc.generator.v0.Client
import com.gilt.apidoc.generator.v0.models.Generator
import com.gilt.apidoc.internal.v0.models.TaskDataSyncService
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

  def sync(serviceGuid: UUID) {
    ServicesDao.findByGuid(Authorization.All, serviceGuid).map { sync(_) }
  }

  def sync(service: GeneratorService) {
    import scala.concurrent.ExecutionContext.Implicits.global

    val client = new Client(service.uri)

    Pager.eachPage[Generator] { offset =>
      println(s"syncService(${service.uri}) - offset[$offset]")
      val results = Await.result(
        client.generators.get(limit = 100, offset = offset),
        5000.millis
      )
      assert(offset < 25, s"Likely infinite loop fetching generators for service at URI[${service.uri}]")
      results
    } { gen =>
      GeneratorsDao.upsert(UsersDao.AdminUser, service, gen)
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

