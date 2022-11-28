package util

import db.generators.{GeneratorsDao, ServicesDao}
import db.{Authorization, UsersDao}
import io.apibuilder.api.v0.models.{GeneratorForm, GeneratorService}
import lib.Pager
import modules.clients.GeneratorClientFactory
import play.api.{Environment, Logger, Mode}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.util.{Failure, Success, Try}

class GeneratorServiceUtil @Inject() (
  servicesDao: ServicesDao,
  generatorsDao: GeneratorsDao,
  usersDao: UsersDao,
  env: Environment,
  generatorClientFactory: GeneratorClientFactory
) {

  private[this] val log: Logger = Logger(this.getClass)
  private[this] val inTestEnv = env.mode match {
    case Mode.Test => true
    case Mode.Dev | Mode.Prod => false
  }

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
          ex match {
            case _: java.net.UnknownHostException if inTestEnv => // ignore
            case _ => log.error(s"[GeneratorServiceActor] Service[${service.guid}] at uri[${service.uri}] failed to sync: ${ex.getMessage}", ex)
          }
        }
      }
    }
  }

  def sync(
    service: GeneratorService
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ): Unit = {
    val client = generatorClientFactory.instance(service.uri)

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