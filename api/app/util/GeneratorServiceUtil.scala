package util

import java.util.UUID
import javax.inject.Inject
import db.{Authorization, UsersDao}
import db.generators.{GeneratorsDao, ServicesDao}
import io.apibuilder.api.v0.models.{GeneratorForm, GeneratorService}
import io.apibuilder.generator.v0.Client
import lib.Pager
import play.api.Logger
import play.api.libs.ws.WSClient

import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.util.{Failure, Success, Try}

class GeneratorServiceUtil @Inject() (
  wSClient: WSClient,
  servicesDao: ServicesDao,
  generatorsDao: GeneratorsDao,
  usersDao: UsersDao
) {

  private[this] val log: Logger = Logger(this.getClass)

  def sync(serviceGuid: UUID)(implicit ec: scala.concurrent.ExecutionContext): Unit = {
    servicesDao.findByGuid(Authorization.All, serviceGuid).foreach { sync }
  }

  def syncAll()(implicit ec: scala.concurrent.ExecutionContext): Unit = {
    Pager.eachPage { offset =>
      servicesDao.findAll(
        Authorization.All,
        limit = 200,
        offset = offset
      )
    } { service =>
      Try {
        sync(service)
      } match {
        case Success(_) => {
          log.info(s"[GeneratorServiceActor] Service[${service.guid}] at uri[${service.uri}] synced")
        }
        case Failure(ex) => {
          log.error(s"[GeneratorServiceActor] Service[${service.guid}] at uri[${service.uri}] failed to sync: ${ex.getMessage}", ex)
        }
      }
    }
  }

  def sync(
    service: GeneratorService
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ): Unit = {
    val client = new Client(wSClient, service.uri)

    var iteration = 0
    val limit = 100
    var num = limit
    var offset = 0

    while (num >= limit) {
      val generators = Await.result(
        client.generators.get(limit = limit, offset = offset),
        FiniteDuration(30, SECONDS)
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
          case Left(errors) => log.error(s"Error fetching generators for service[${service.guid}] uri[${service.uri}]: " + errors.mkString(", "))
          case Right(_) => {}
        }
      }
    }
  }
}