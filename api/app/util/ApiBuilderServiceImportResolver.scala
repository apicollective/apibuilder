package util

import db.{Authorization, InternalVersionsDao}
import io.apibuilder.api.v0.models.Version
import io.apibuilder.spec.v0.models.{Import, Service}
import lib.VersionTag
import logger.ApiBuilderLogger
import models.VersionsModel

import javax.inject.Inject
import scala.annotation.tailrec

class ApiBuilderServiceImportResolver @Inject()(
                                                 versionsDao: InternalVersionsDao,
                                                 logger: ApiBuilderLogger,
                                                 versionModel: VersionsModel
) {

  /**
   * Expands each import into its service definition, returning the list of imported services.
   * If a service is imported more than once, returns the latest version.
   **/
  def resolve(auth: Authorization, service: Service): Seq[Service] = {
    resolve(auth, service.imports.toList, ServiceBuilder()).distinctServicesByLatest
  }

  @tailrec
  private def resolve(auth: Authorization, imports: List[Import], builder: ServiceBuilder): ServiceBuilder = {
    imports match {
      case Nil => builder
      case imp :: rest => {
        if (builder.hasImport(imp)) {
          resolve(auth, rest, builder)
        } else {
          versionsDao.findVersion(auth, imp.organization.key, imp.application.key, imp.version).flatMap(versionModel.toModel) match {
            case None => {
              logger
                .organization(imp.organization.key)
                .withKeyValue("application", imp.application.key)
                .withKeyValue("version", imp.version)
                .info("Could not resolve import")
              resolve(auth, rest, builder)
            }
            case Some(v: Version) => {
              resolve(auth, rest ++ v.service.imports, builder.withService(v.service))
            }
          }
        }
      }
    }
  }
}

private[util] case class ServiceBuilder(services: Seq[Service] = Nil) {

  private val keysWithVersion: Set[String] = services.map(toKeyWithVersion).toSet

  private def toKeyWithVersion(service: Service): String = {
    s"${service.organization.key}/${service.application.key}/${service.version}"
  }
  private def toKeyWithVersion(imp: Import): String = {
    s"${imp.organization.key}/${imp.application.key}/${imp.version}"
  }

  def hasImport(imp: Import): Boolean = {
    keysWithVersion.contains(toKeyWithVersion(imp))
  }

  /**
   * Returns the list of distinct services. Where multiple versions are available,
   * chooses the latest.
   */
  lazy val distinctServicesByLatest: Seq[Service] = {
    services
      .groupBy { s => s"${s.organization.key}/${s.application.key}" }
      .map { case (_, services) =>
        services.maxBy { s => VersionTag(s.version) }
      }.toSeq
  }


  def withService(service: Service): ServiceBuilder = {
    ServiceBuilder(services = services ++ Seq(service))
  }

}
