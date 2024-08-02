package db

import anorm._
import builder.OriginalValidator
import cats.data.ValidatedNec
import cats.implicits._
import core.{ServiceFetcher, VersionMigration}
import io.apibuilder.api.v0.models._
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}
import io.apibuilder.spec.v0.models.Service
import io.apibuilder.spec.v0.models.json._
import io.apibuilder.task.v0.models._
import io.apibuilder.task.v0.models.json._
import io.flow.postgresql.Query
import lib.{ServiceConfiguration, ServiceUri, ValidatedHelpers, VersionTag}
import models.VersionsModel
import org.joda.time.DateTime
import play.api.Logger
import play.api.db._
import play.api.libs.json._
import processor.MigrateVersion

import java.util.UUID
import javax.inject.Inject

case class InternalVersion(
  guid: UUID,
  applicationGuid: UUID,
  version: String,
  original: Option[Original],
  serviceJson: Option[String],
  audit: Audit
)

class VersionsDao @Inject() (
                              @NamedDatabase("default") db: Database,
                              applicationsDao: InternalApplicationsDao,
                              originalsDao: OriginalsDao,
                              tasksDao: InternalTasksDao,
                              usersDao: UsersDao,
                              organizationsDao: InternalOrganizationsDao,
                              versionsModel: VersionsModel
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

  private val BaseQuery = Query(s"""
    select versions.guid, versions.version,
           ${AuditsDao.queryCreationDefaultingUpdatedAt("versions")},
           originals.type as original_type,
           originals.data as original_data,
           versions.application_guid,
           (select services.json::text
              from cache.services
             where services.deleted_at is null
               and services.version_guid = versions.guid
             order by services.created_at desc
              limit 1) as service_json
     from versions
     left join originals on originals.version_guid = versions.guid and originals.deleted_at is null
  """)

  private val InsertQuery = """
    insert into versions
    (guid, application_guid, version, version_sort_key, created_by_guid)
    values
    ({guid}::uuid, {application_guid}::uuid, {version}, {version_sort_key}, {created_by_guid}::uuid)
  """

  private val DeleteQuery = """
    update versions
       set deleted_at = now(),
           deleted_by_guid = {deleted_by_guid}::uuid
     where deleted_at is null
       and guid = {guid}::uuid
  """

  private val InsertServiceQuery = """
    insert into cache.services
    (guid, version_guid, version, json, created_by_guid)
    values
    ({guid}::uuid, {version_guid}::uuid, {version}, {json}::json, {user_guid}::uuid)
  """

  private val SoftDeleteServiceByVersionGuidAndVersionNumberQuery = """
    update cache.services
       set deleted_at = now(),
           deleted_by_guid = {user_guid}::uuid
     where deleted_at is null
       and version_guid = {version_guid}::uuid
       and version = {version}
  """

  def create(user: User, application: InternalApplication, version: String, original: Original, service: Service): InternalVersion = {
    val latestVersion: Option[InternalVersion] = findAll(
      Authorization.User(user.guid),
      applicationGuid = Some(application.guid),
      limit = Some(1)
    ).headOption

    val guid = db.withTransaction { implicit c =>
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
    user: User,
    application: InternalApplication,
    version: String,
    original: Original,
    service: Service
  ): UUID = {
    val guid = UUID.randomUUID

    SQL(InsertQuery).on(
      "guid" -> guid,
      "application_guid" -> application.guid,
      "version" -> version.trim,
      "version_sort_key" -> VersionTag(version.trim).sortKey,
      "created_by_guid" -> user.guid
    ).execute()

    originalsDao.create(c, user, guid, original)
    softDeleteService(c, user, guid)
    insertService(c, user, guid, service)

    guid
  }

  def softDelete(deletedBy: User, version: InternalVersion): Unit =  {
    db.withTransaction { implicit c =>
      softDeleteService(c, deletedBy, version.guid)
      originalsDao.softDeleteByVersionGuid(c, deletedBy, version.guid)

      SQL(DeleteQuery).on(
        "guid" -> version.guid,
        "deleted_by_guid" -> deletedBy.guid
      ).execute()
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

  def replace(user: User, version: InternalVersion, application: InternalApplication, original: Original, service: Service): InternalVersion = {
    val versionGuid = db.withTransaction { implicit c =>
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
    db.withConnection { implicit c =>
      authorization.applicationFilter(BaseQuery, "application_guid").
        and(HasServiceJsonClause).
        equals("versions.guid", guid).
        optionalIn("versions.guid", guids).
        equals("versions.application_guid", applicationGuid).
        equals("versions.version", version).
        and(versionConstraint.map(vc => s"versions.version like '${vc}%'")).
        and(isDeleted.map(Filters.isDeleted("versions", _))).
        orderBy("versions.version_sort_key desc, versions.created_at desc").
        optionalLimit(limit).
        offset(offset).
        as(parser.*)
    }
  }

  def findAllVersions(
    authorization: Authorization,
    applicationGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[ApplicationMetadataVersion] = {
    db.withConnection { implicit c =>
      authorization.applicationFilter(BaseQuery, "application_guid").
        and(HasServiceJsonClause).
        equals("versions.application_guid", applicationGuid).
        and(isDeleted.map(Filters.isDeleted("versions", _))).
        orderBy("versions.version_sort_key desc, versions.created_at desc").
        limit(limit).
        offset(offset).
        as(applicationMetadataParser().*
      )
    }
  }  

  private def applicationMetadataParser(): RowParser[ApplicationMetadataVersion] = {
    SqlParser.str("version") map { version =>
      ApplicationMetadataVersion(
        version = version
      )
    }
  }

  private val parser: RowParser[InternalVersion] = {
    SqlParser.get[UUID]("guid") ~
    SqlParser.get[UUID]("application_guid") ~
    SqlParser.str("version") ~
    SqlParser.str("original_type").? ~
    SqlParser.str("original_data").? ~
    SqlParser.str("service_json").? ~
    SqlParser.get[DateTime]("created_at") ~
    SqlParser.get[UUID]("created_by_guid") ~
    SqlParser.get[DateTime]("updated_at") ~
    SqlParser.get[UUID]("updated_by_guid") map {
      case guid ~ applicationGuid ~ version ~ originalType ~ originalData ~ serviceJson ~ createdAt ~ createdByGuid ~ updatedAt ~ updatedByGuid => {
        InternalVersion(
          guid = guid,
          applicationGuid = applicationGuid,
          version = version,
          original = (originalType.map(OriginalType.apply), originalData).mapN(Original(_, _)),
          serviceJson = serviceJson,
          audit = Audit(
            createdAt = createdAt,
            createdBy = ReferenceGuid(createdByGuid),
            updatedAt = updatedAt,
            updatedBy = ReferenceGuid(updatedByGuid),
          )
        )
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
    db.withConnection { implicit c =>
      BaseQuery.
        equals("versions.guid", guid).
        as(parser.*)
    }.headOption
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
          version.original.toValidNec("Missing original"),
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
            db.withConnection { c =>
              insertService(c, usersDao.AdminUser, versionGuid, service)
            }
          }
        }
      }
    }
  }

  private def softDeleteService(
    implicit c: java.sql.Connection,
    user: User,
    versionGuid: UUID
  ): Unit =  {
    SQL(SoftDeleteServiceByVersionGuidAndVersionNumberQuery).on(
      "version_guid" -> versionGuid,
      "version" -> MigrateVersion.ServiceVersionNumber,
      "user_guid" -> user.guid
    ).execute()
  }

  private def insertService(
    implicit c: java.sql.Connection,
    user: User,
    versionGuid: UUID,
    service: Service
  ): Unit =  {
    SQL(InsertServiceQuery).on(
      "guid" -> UUID.randomUUID,
      "version_guid" -> versionGuid,
      "version" -> MigrateVersion.ServiceVersionNumber,
      "json" -> Json.toJson(service).as[JsObject].toString.trim,
      "user_guid" -> user.guid
    ).execute()
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
