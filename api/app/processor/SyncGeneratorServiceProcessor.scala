package processor

import akka.actor.ActorSystem
import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec
import cats.implicits._
import db.generators.{GeneratorsDao, ServicesDao}
import db.{Authorization, UsersDao}
import io.apibuilder.api.v0.models.{GeneratorForm, GeneratorService}
import io.apibuilder.generator.v0.interfaces.Client
import io.apibuilder.generator.v0.models.Generator
import io.apibuilder.task.v0.models.TaskType
import lib.{Pager, ValidatedHelpers}
import modules.clients.GeneratorClientFactory
import play.api.Logger

import java.util.UUID
import javax.inject.Inject
import scala.annotation.tailrec
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}


class SyncGeneratorServiceProcessor @Inject()(
  args: TaskProcessorArgs,
  system: ActorSystem,
  servicesDao: ServicesDao,
  generatorsDao: GeneratorsDao,
  usersDao: UsersDao,
  generatorClientFactory: GeneratorClientFactory
) extends TaskProcessorWithGuid(args, TaskType.SyncGeneratorService) with ValidatedHelpers {

  private val log: Logger = Logger(this.getClass)
  private val ec: ExecutionContext = system.dispatchers.lookup("generator-service-sync-context")

  override def processRecord(guid: UUID): ValidatedNec[String, Unit] = {
    syncAll()(ec).validNec
  }

  private def syncAll(pageSize: Long = 200)(implicit ec: scala.concurrent.ExecutionContext): Unit = {
    Pager.eachPage { offset =>
      servicesDao.findAll(
        Authorization.All,
        limit = Some(pageSize),
        offset = offset
      )
    } { service =>
      Try {
        sync(service, pageSize = pageSize)
      } match {
        case Success(_) => {
          log.info(s"[GeneratorServiceActor] Service[${service.guid}] at uri[${service.uri}] synced")
        }
        case Failure(ex) => {
          ex match {
            case _ => log.error(s"[GeneratorServiceActor] Service[${service.guid}] at uri[${service.uri}] failed to sync: ${ex.getMessage}", ex)
          }
        }
      }
    }
  }

  private def sync(
            service: GeneratorService,
            pageSize: Long = 200
          ) (
            implicit ec: scala.concurrent.ExecutionContext
          ): Unit = {
    doSync(
      client = generatorClientFactory.instance(service.uri),
      service = service,
      pageSize = pageSize,
      offset = 0,
      resolved = Nil
    )
  }

  @tailrec
  private def doSync(
                            client: Client,
                            service: GeneratorService,
                            pageSize: Long,
                            offset: Int,
                            resolved: List[Generator]
                          )(
                            implicit ec: scala.concurrent.ExecutionContext
                          ): Unit = {
    val newGenerators = Await.result(
      client.generators.get(limit = pageSize.toInt, offset = offset),
      FiniteDuration(30, SECONDS)
    ).filterNot { g => resolved.exists(_.key == g.key) }

    if (newGenerators.nonEmpty) {
      storeGenerators(service, newGenerators)
      doSync(client, service, pageSize = pageSize, offset + pageSize.toInt, resolved ++ newGenerators)
    }
  }

  private def storeGenerators(service: GeneratorService, generators: Seq[Generator]): Unit = {
    sequenceUnique {
      generators.map { gen =>
        generatorsDao.upsert(
          usersDao.AdminUser,
          GeneratorForm(
            serviceGuid = service.guid,
            generator = gen
          )
        )
      }
    } match {
      case Invalid(errors) => {
        log.error(s"Error fetching generators for service[${service.guid}] uri[${service.uri}]: " + formatErrors(errors))
      }
      case Valid(_) => // no-op
    }
  }
}