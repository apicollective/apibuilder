package util

import cats.data.Validated.{Invalid, Valid}
import db.generators.{GeneratorsDao, ServicesDao}
import db.{Authorization, UsersDao}
import io.apibuilder.api.v0.models.{GeneratorForm, GeneratorService}
import io.apibuilder.generator.v0.interfaces.Client
import io.apibuilder.generator.v0.models.Generator
import lib.{Pager, ValidatedHelpers}
import modules.clients.GeneratorClientFactory
import play.api.Logger

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.util.{Failure, Success, Try}

class GeneratorServiceUtil @Inject() (
  servicesDao: ServicesDao,
  generatorsDao: GeneratorsDao,
  usersDao: UsersDao,
  generatorClientFactory: GeneratorClientFactory
) extends ValidatedHelpers {

  private[this] val log: Logger = Logger(this.getClass)

  def sync(serviceGuid: UUID)(implicit ec: scala.concurrent.ExecutionContext): Unit = {
    servicesDao.findByGuid(Authorization.All, serviceGuid).foreach { sync(_) }
  }

  def syncAll(pageSize: Long = 200)(implicit ec: scala.concurrent.ExecutionContext): Unit = {
    Pager.eachPage { offset =>
      servicesDao.findAll(
        Authorization.All,
        limit = pageSize,
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

  def sync(
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

  private[this] def doSync(
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

  private[this] def storeGenerators(service: GeneratorService, generators: Seq[Generator]): Unit = {
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