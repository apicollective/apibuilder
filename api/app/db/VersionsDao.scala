package db

import anorm._
import builder.OriginalValidator
import builder.api_json.upgrades.ServiceParser
import cats.data.ValidatedNec
import cats.implicits._
import core.{ServiceFetcher, VersionMigration}
import io.apibuilder.api.v0.models._
import io.apibuilder.common.v0.models.{Audit, Reference}
import io.apibuilder.spec.v0.models.Service
import io.apibuilder.spec.v0.models.json._
import io.apibuilder.task.v0.models._
import io.apibuilder.task.v0.models.json._
import io.flow.postgresql.Query
import lib.{ServiceConfiguration, ServiceUri, ValidatedHelpers, VersionTag}
import play.api.Logger
import play.api.db._
import play.api.libs.json._
import processor.TaskType

import java.util.UUID
import javax.inject.{Inject, Named}

class VersionsDao @Inject() (
  @NamedDatabase("default") db: Database,
  @Named("main-actor") mainActor: akka.actor.ActorRef,
  applicationsDao: ApplicationsDao,
  originalsDao: OriginalsDao,
  tasksDao: InternalTasksDao,
  usersDao: UsersDao,
  organizationsDao: OrganizationsDao,
  serviceParser: ServiceParser
) extends ValidatedHelpers {

  private[this] val logger: Logger = Logger(this.getClass)

  private[this] val LatestVersion = "latest"
  private[this] val LatestVersionFilter = "~"

  private[this] val HasServiceJsonClause: String =
    """
      |exists (
      |  select 1
      |    from cache.services
      |   where services.deleted_at is null
      |     and services.version_guid = versions.guid
      |)
    """.stripMargin

  private[this] val BaseQuery = Query(s"""
    select versions.guid, versions.version,
           ${AuditsDao.queryCreationDefaultingUpdatedAt("versions")},
           originals.type as original_type,
           originals.data as original_data,
           organizations.guid as organization_guid,
           organizations.key as organization_key,
           organizations.namespace as organization_namespace,
           applications.guid as application_guid,
           applications.key as application_key,
           (select services.json::text
              from cache.services
             where services.deleted_at is null
               and services.version_guid = versions.guid
             order by services.created_at desc
              limit 1) as service_json
     from versions
     left join originals on originals.version_guid = versions.guid and originals.deleted_at is null
     join applications on applications.deleted_at is null and applications.guid = versions.application_guid
     join organizations on organizations.deleted_at is null and organizations.guid = applications.organization_guid
  """)

  private[this] val InsertQuery = """
    insert into versions
    (guid, application_guid, version, version_sort_key, created_by_guid)
    values
    ({guid}::uuid, {application_guid}::uuid, {version}, {version_sort_key}, {created_by_guid}::uuid)
  """

  private[this] val DeleteQuery = """
    update versions
       set deleted_at = now(),
           deleted_by_guid = {deleted_by_guid}::uuid
     where deleted_at is null
       and guid = {guid}::uuid
  """

  private[this] val InsertServiceQuery = """
    insert into cache.services
    (guid, version_guid, version, json, created_by_guid)
    values
    ({guid}::uuid, {version_guid}::uuid, {version}, {json}::json, {user_guid}::uuid)
  """

  private[this] val SoftDeleteServiceByVersionGuidAndVersionNumberQuery = """
    update cache.services
       set deleted_at = now(),
           deleted_by_guid = {user_guid}::uuid
     where deleted_at is null
       and version_guid = {version_guid}::uuid
       and version = {version}
  """

  def create(user: User, application: Application, version: String, original: Original, service: Service): Version = {
    val latestVersion: Option[Version] = findAll(
      Authorization.User(user.guid),
      applicationGuid = Some(application.guid),
      limit = 1
    ).headOption

    val (guid, taskGuid) = db.withTransaction { implicit c =>
      val versionGuid = doCreate(c, user, application, version, original, service)
      val taskGuid = latestVersion.map { v =>
        createDiffTask(user, v.guid, versionGuid)
      }
      (versionGuid, taskGuid)
    }

    taskGuid.foreach { guid =>
      mainActor ! actors.MainActor.Messages.TaskCreated(guid)
    }

    findAll(Authorization.All, guid = Some(guid), limit = 1).headOption.getOrElse {
      sys.error("Failed to create version")
    }
  }

  private[this] def doCreate(
    implicit c: java.sql.Connection,
    user: User,
    application: Application,
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

  def softDelete(deletedBy: User, version: Version): Unit =  {
    db.withTransaction { implicit c =>
      softDeleteService(c, deletedBy, version.guid)
      originalsDao.softDeleteByVersionGuid(c, deletedBy, version.guid)

      SQL(DeleteQuery).on(
        "guid" -> version.guid,
        "deleted_by_guid" -> deletedBy.guid
      ).execute()
    }
  }

  private[this] def createDiffTask(
    user: User, oldVersionGuid: UUID, newVersionGuid: UUID
  ) (
    implicit c: java.sql.Connection
  ): UUID = {
    tasksDao.queueWithConnection(c, TaskType.DiffVersion, data = Json.toJson(DiffVersionData(oldVersionGuid = oldVersionGuid, newVersionGuid = newVersionGuid)))
  }

  def replace(user: User, version: Version, application: Application, original: Original, service: Service): Version = {
    val (versionGuid, taskGuids) = db.withTransaction { implicit c =>
      softDelete(user, version)
      val versionGuid = doCreate(c, user, application, version.version, original, service)
      val diffTaskGuid = createDiffTask(user, version.guid, versionGuid)
      val indexTaskGuid = tasksDao.insert(c, user, TaskDataIndexApplication(application.guid))
      (versionGuid, Seq(diffTaskGuid, indexTaskGuid))
    }

    taskGuids.foreach { taskGuid =>
      mainActor ! actors.MainActor.Messages.TaskCreated(taskGuid)
    }

    findAll(Authorization.All, guid = Some(versionGuid), limit = 1).headOption.getOrElse {
      sys.error(s"Failed to replace version[${version.guid}]")
    }
  }

  def findVersion(
    authorization: Authorization,
    orgKey: String,
    applicationKey: String,
    version: String
  ): Option[Version] = {
    applicationsDao.findByOrganizationKeyAndApplicationKey(authorization, orgKey, applicationKey).flatMap { application =>
      if (version == LatestVersion) {
        findAll(authorization, applicationGuid = Some(application.guid), limit = 1).headOption
      } else if (version.startsWith(LatestVersionFilter)) {
        /*
         ~ specifies a minimum version, but allows the last digit specified to go up
         */
        val versionFilter = version.replace(LatestVersionFilter, "")
        findAll(authorization, applicationGuid = Some(application.guid), limit = 1
          , versionConstraint = Some(versionFilter.split("\\.").dropRight(1).mkString(".")) //allows the last digit specified to go up
        )
          .headOption
          .filter(_.version >= versionFilter) //must meet minimum version
      } else {
        findByApplicationAndVersion(authorization, application, version)
      }
    }
  }

  def findByApplicationAndVersion(authorization: Authorization, application: Application, version: String): Option[Version] = {
    findAll(
      authorization,
      applicationGuid = Some(application.guid),
      version = Some(version),
      limit = 1
    ).headOption
  }

  def findByGuid(
    authorization: Authorization,
    guid: UUID,
    isDeleted: Option[Boolean] = Some(false)
  ): Option[Version] = {
    findAll(authorization, guid = Some(guid), isDeleted = isDeleted, limit = 1).headOption
  }

  def findAll(
    authorization: Authorization,
    applicationGuid: Option[UUID] = None,
    guid: Option[UUID] = None,
    version: Option[String] = None,
    versionConstraint: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Version] = {
    db.withConnection { implicit c =>
      authorization.applicationFilter(BaseQuery).
        and(HasServiceJsonClause).
        equals("versions.guid", guid).
        equals("versions.application_guid", applicationGuid).
        equals("versions.version", version).
        and(versionConstraint.map(vc => s"versions.version like '${vc}%'")).
        and(isDeleted.map(Filters.isDeleted("versions", _))).
        orderBy("versions.version_sort_key desc, versions.created_at desc").
        limit(limit).
        offset(offset).
        as(parser.*)
    }.flatMap { v =>
      v.toVersion.toOption
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
      authorization.applicationFilter(BaseQuery).
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

  private[this] def applicationMetadataParser(): RowParser[ApplicationMetadataVersion] = {
    SqlParser.str("version") map { version =>
      ApplicationMetadataVersion(
        version = version
      )
    }
  }

  private[this] case class TmpVersion(
                                       guid: UUID,
                                       organization: Reference,
                                       application: Reference,
                                       version: String,
                                       original: Option[Original],
                                       serviceJson: Option[String],
                                       audit: Audit
                                     ) {
    def toVersion: ValidatedNec[String, Version] = {
      service.map { svc =>
        Version(
          guid = guid,
          organization = organization,
          application = application,
          version = version,
          original = original,
          service = svc,
          audit = audit
        )
      }

    }

    private[this] def service: ValidatedNec[String, Service] = {
      serviceJson match {
        case None => "Version does not have service json".invalidNec
        case Some(json) => serviceParser.fromString(json)
      }
    }
  }

  private[this] val parser: RowParser[TmpVersion] = {
    SqlParser.get[_root_.java.util.UUID]("guid") ~
      io.apibuilder.common.v0.anorm.parsers.Reference.parserWithPrefix("organization") ~
      io.apibuilder.common.v0.anorm.parsers.Reference.parserWithPrefix("application") ~
      SqlParser.str("version") ~
      io.apibuilder.api.v0.anorm.parsers.Original.parserWithPrefix("original").? ~
      SqlParser.str("service_json").? ~
      io.apibuilder.common.v0.anorm.parsers.Audit.parserWithPrefix("audit") map {
      case guid ~ organization ~ application ~ version ~ original ~ serviceJson ~ audit => {
        TmpVersion(
          guid = guid,
          organization = organization,
          application = application,
          version = version,
          original = original,
          serviceJson = serviceJson,
          audit = audit
        )
      }
    }
  }

  private[this] def validateOrg(org: Reference): ValidatedNec[String, Organization] = {
    organizationsDao.findByGuid(Authorization.All, org.guid).toValidNec(s"Cannot find organization where guid = '${org.guid}'")
  }

  private[this] def lookupVersionToMigrate(guid: UUID): Option[TmpVersion] = {
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
        val orgKey = version.organization.key
        val applicationKey = version.application.key
        val versionName = version.version
        val versionGuid = version.guid

        (
          validateOrg(version.organization),
          version.original.toValidNec("Missing original"),
        ).mapN { case (org, original) =>
          val serviceConfig = ServiceConfiguration(
            orgKey = orgKey,
            orgNamespace = org.namespace,
            version = versionName
          )
          logger.info(s"Migrating $orgKey/$applicationKey/$versionName versionGuid[$versionGuid] to latest API Builder spec version[${Migration.ServiceVersionNumber}] (with serviceConfig=$serviceConfig)")

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

  private[this] def softDeleteService(
    implicit c: java.sql.Connection,
    user: User,
    versionGuid: UUID
  ): Unit =  {
    SQL(SoftDeleteServiceByVersionGuidAndVersionNumberQuery).on(
      "version_guid" -> versionGuid,
      "version" -> Migration.ServiceVersionNumber,
      "user_guid" -> user.guid
    ).execute()
  }

  private[this] def insertService(
    implicit c: java.sql.Connection,
    user: User,
    versionGuid: UUID,
    service: Service
  ): Unit =  {
    SQL(InsertServiceQuery).on(
      "guid" -> UUID.randomUUID,
      "version_guid" -> versionGuid,
      "version" -> Migration.ServiceVersionNumber,
      "json" -> Json.toJson(service).as[JsObject].toString.trim,
      "user_guid" -> user.guid
    ).execute()
  }

  private[this] def databaseServiceFetcher(auth: Authorization) = {
    new ServiceFetcher {
      override def fetch(uri: String): Service = {
        val serviceUri = ServiceUri.parse(uri).getOrElse {
          sys.error(s"could not parse URI[$uri]")
        }

        findVersion(auth, serviceUri.org, serviceUri.app, serviceUri.version).map(_.service).getOrElse {
          sys.error(s"Error while fetching service for URI[$serviceUri] - could not find [${serviceUri.org}/${serviceUri.app}:${serviceUri.version}]")
        }
      }
    }
  }
}
