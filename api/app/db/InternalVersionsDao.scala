package db

import anorm.*
import builder.OriginalValidator
import cats.data.ValidatedNec
import cats.implicits.*
import core.{ServiceFetcher, VersionMigration}
import db.generated.VersionsDao
import db.generated.cache.ServicesDao
import io.apibuilder.api.v0.models.*
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}
import io.apibuilder.spec.v0.models.Service
import io.apibuilder.spec.v0.models.json.*
import io.apibuilder.task.v0.models.*
import io.apibuilder.task.v0.models.json.*
import io.flow.postgresql.{OrderBy, Query}
import lib.{ServiceConfiguration, ServiceUri, ValidatedHelpers, VersionTag}
import models.VersionsModel
import org.joda.time.DateTime
import play.api.Logger
import play.api.db.*
import play.api.libs.json.*
import processor.MigrateVersion

import java.util.UUID
import javax.inject.Inject

case class InternalVersion(db: generated.Version) {
  val guid: UUID = db.guid
  val applicationGuid: UUID = db.applicationGuid
  val version: String = db.version
}

class InternalVersionsDao @Inject()(
                                     dao: VersionsDao,
                                     applicationsDao: InternalApplicationsDao,
                                     originalsDao: InternalOriginalsDao,
                                     tasksDao: InternalTasksDao,
                                     usersDao: InternalUsersDao,
                                     organizationsDao: InternalOrganizationsDao,
                                     versionsModel: VersionsModel,
                                     servicesDao: ServicesDao
) extends ValidatedHelpers {

  private val logger: Logger = Logger(this.getClass)

  private val LatestVersion = "latest"
  private val LatestVersionFilter = "~"

  private val HasServiceJsonClause: String =
    """
      |exists (
      |  select 1
      |    from cache.services
      |   where services.deleted_at is null
      |     and services.version_guid = versions.guid
      |)
    """.stripMargin

  def create(user: InternalUser, application: InternalApplication, version: String, original: Original, service: Service): InternalVersion = {
    val latestVersion: Option[InternalVersion] = findAll(
      Authorization.User(user.guid),
      applicationGuid = Some(application.guid),
      limit = Some(1)
    ).headOption

    val guid = dao.db.withTransaction { implicit c =>
      val versionGuid = doCreate(c, user, application, version, original, service)
      latestVersion.foreach { v =>
        createDiffTask(v.guid, versionGuid)
      }
      versionGuid
    }

    findByGuid(Authorization.All, guid).getOrElse {
      sys.error("Failed to create version")
    }
  }

  private def doCreate(
    implicit c: java.sql.Connection,
    user: InternalUser,
    application: InternalApplication,
    version: String,
    original: Original,
    service: Service
  ): UUID = {
    val guid = dao.insert(c, user.guid, generated.VersionForm(
      applicationGuid = application.guid,
      version = version.trim,
      versionSortKey = VersionTag(version.trim).sortKey,
      original = None,
      oldJson = None,
    ))

    originalsDao.create(c, user, guid, original)
    softDeleteService(c, user, guid)
    insertService(c, user, guid, service)

    guid
  }

  def softDelete(deletedBy: InternalUser, version: InternalVersion): Unit =  {
    dao.db.withTransaction { implicit c =>
      softDeleteService(c, deletedBy, version.guid)
      originalsDao.softDeleteByVersionGuid(c, deletedBy, version.guid)
      dao.delete(c, deletedBy.guid, version.db)
    }
  }

  private def createDiffTask(
    oldVersionGuid: UUID,
    newVersionGuid: UUID
  ) (
    implicit c: java.sql.Connection
  ): Unit = {
    tasksDao.queueWithConnection(
      c,
      TaskType.DiffVersion,
      newVersionGuid.toString,
      data = Json.toJson(DiffVersionData(oldVersionGuid = oldVersionGuid, newVersionGuid = newVersionGuid))
    )
  }

  def replace(user: InternalUser, version: InternalVersion, application: InternalApplication, original: Original, service: Service): InternalVersion = {
    val versionGuid = dao.db.withTransaction { implicit c =>
      softDelete(user, version)
      val versionGuid = doCreate(c, user, application, version.version, original, service)
      createDiffTask(version.guid, versionGuid)
      tasksDao.queueWithConnection(c, TaskType.IndexApplication, application.guid.toString)
      versionGuid
    }

    findByGuid(Authorization.All, versionGuid).getOrElse {
      sys.error(s"Failed to replace version[${version.guid}]")
    }
  }

  def findVersion(
    authorization: Authorization,
    orgKey: String,
    applicationKey: String,
    version: String
  ): Option[InternalVersion] = {
    applicationsDao.findByOrganizationKeyAndApplicationKey(authorization, orgKey, applicationKey).flatMap { application =>
      if (version == LatestVersion) {
        findAll(authorization, applicationGuid = Some(application.guid), limit = Some(1)).headOption
      } else if (version.startsWith(LatestVersionFilter)) {
        /*
         ~ specifies a minimum version, but allows the last digit specified to go up
         */
        val versionFilter = version.replace(LatestVersionFilter, "")
        findAll(authorization, applicationGuid = Some(application.guid), limit = Some(1)
          , versionConstraint = Some(versionFilter.split("\\.").dropRight(1).mkString(".")) //allows the last digit specified to go up
        )
          .headOption
          .filter(_.version >= versionFilter) //must meet minimum version
      } else {
        findByApplicationAndVersion(authorization, application, version)
      }
    }
  }

  def findByApplicationAndVersion(authorization: Authorization, application: InternalApplication, version: String): Option[InternalVersion] = {
    findAll(
      authorization,
      applicationGuid = Some(application.guid),
      version = Some(version),
      limit = Some(1)
    ).headOption
  }

  def findByGuid(
    authorization: Authorization,
    guid: UUID,
    isDeleted: Option[Boolean] = Some(false)
  ): Option[InternalVersion] = {
    findAll(authorization, guid = Some(guid), isDeleted = isDeleted, limit = Some(1)).headOption
  }

  def findAllByGuids(authorization: Authorization, guids: Seq[UUID]): Seq[InternalVersion] = {
    findAll(authorization, guids = Some(guids), limit = None)
  }

  def findAll(
    authorization: Authorization,
    applicationGuid: Option[UUID] = None,
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    version: Option[String] = None,
    versionConstraint: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Option[Long],
    offset: Long = 0
  ): Seq[InternalVersion] = {
    dao.findAll(
      guid = guid,
      guids = guids,
      applicationGuid = applicationGuid,
      limit = limit,
      offset = offset,
      orderBy = Some(OrderBy("-version_sort_key, -created_at"))
    ) { q =>
      authorization.applicationFilter(q, "application_guid")
      .and(HasServiceJsonClause)
      .and(versionConstraint.map(vc => s"version like '${vc}%'"))
      .and(isDeleted.map(Filters.isDeleted("versions", _)))
      .equals("version", version)
    }.map(InternalVersion(_))
  }

  // Efficient query to fetch all versions of a given application
  def findAllVersions(
    authorization: Authorization,
    applicationGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Option[Long],
    offset: Long = 0
  ): Seq[ApplicationMetadataVersion] = {
    dao.db.withConnection { c =>
      authorization.applicationFilter(
          Query("select version from versions").withDebugging,
          "application_guid"
        ).equals("application_guid", applicationGuid)
        .and(isDeleted.map(Filters.isDeleted("versions", _)))
        .optionalLimit(limit)
        .offset(offset)
        .orderBy("version_sort_key desc, created_at desc")
        .as(SqlParser.str(1).*)(c)
        .map { version =>
          ApplicationMetadataVersion(version = version)
        }
    }
  }

  private def validateApplication(guid: UUID): ValidatedNec[String, InternalApplication] = {
    applicationsDao.findByGuid(Authorization.All, guid).toValidNec(s"Cannot find application where guid = '$guid'")
  }

  private def validateOrg(guid: UUID): ValidatedNec[String, InternalOrganization] = {
    organizationsDao.findByGuid(Authorization.All, guid).toValidNec(s"Cannot find organization where guid = '$guid'")
  }

  private def lookupVersionToMigrate(guid: UUID): Option[InternalVersion] = {
    dao.findByGuid(guid).map(InternalVersion(_))
  }

  private def validateOriginal(version: InternalVersion): ValidatedNec[String, InternalOriginal] = {
    originalsDao.findByVersionGuid(version.guid).toValidNec("Cannot find original")
  }

  def migrateVersionGuid(guid: UUID): ValidatedNec[String, Unit] = {
    lookupVersionToMigrate(guid) match {
      case None => {
        "Could not find version to migrate".invalidNec
      }

      case Some(version) => {
        val versionName = version.version
        val versionGuid = version.guid

        (
          validateApplication(version.applicationGuid).andThen { app =>
            validateOrg(app.organizationGuid).map { o => (o, app) }
          },
          validateOriginal(version)
        ).mapN { case ((org, app), original) =>
          val serviceConfig = ServiceConfiguration(
            orgKey = org.key,
            orgNamespace = org.db.namespace,
            version = versionName
          )
          logger.info(s"Migrating ${org.key}/${app.key}/$versionName versionGuid[$versionGuid] to latest API Builder spec version[${MigrateVersion.ServiceVersionNumber}] (with serviceConfig=$serviceConfig)")

          val validator = OriginalValidator(
            config = serviceConfig,
            `type` = original.`type`,
            fetcher = databaseServiceFetcher(Authorization.All),
            migration = VersionMigration(internal = true)
          )
          validator.validate(original.data).map { service =>
            dao.db.withConnection { c =>
              insertService(c, usersDao.AdminUser, versionGuid, service)
            }
          }
        }
      }
    }
  }

  private def softDeleteService(
    implicit c: java.sql.Connection,
    user: InternalUser,
    versionGuid: UUID
  ): Unit =  {
    servicesDao.findAll(
      versionGuid = Some(versionGuid),
      limit = None
    ) { q =>
      q.equals("version", MigrateVersion.ServiceVersionNumber)
        .isNull("deleted_at")
    }.foreach { s =>
      servicesDao.delete(user.guid, s)
    }
  }

  private def insertService(
    implicit c: java.sql.Connection,
    user: InternalUser,
    versionGuid: UUID,
    service: Service
  ): Unit =  {
    servicesDao.insert(c, user.guid, generated.cache.ServiceForm(
      versionGuid = versionGuid,
      version = MigrateVersion.ServiceVersionNumber,
      json = Json.toJson(service)
    ))
  }

  private def databaseServiceFetcher(auth: Authorization) = {
    new ServiceFetcher {
      override def fetch(uri: String): Service = {
        val serviceUri = ServiceUri.parse(uri).getOrElse {
          sys.error(s"could not parse URI[$uri]")
        }

        findVersion(auth, serviceUri.org, serviceUri.app, serviceUri.version)
          .flatMap(versionsModel.toModel)
          .map(_.service).getOrElse {
          sys.error(s"Error while fetching service for URI[$serviceUri] - could not find [${serviceUri.org}/${serviceUri.app}:${serviceUri.version}]")
        }
      }
    }
  }
}
