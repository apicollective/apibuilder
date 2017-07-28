package db

import io.apibuilder.api.v0.models.{Application, ApplicationForm, Error, MoveForm, Organization, User, Version, Visibility}
import io.apibuilder.common.v0.models.Reference
import io.apibuilder.internal.v0.models.TaskDataIndexApplication
import io.flow.postgresql.Query
import javax.inject.{Inject, Named, Singleton}
import lib.{UrlKey, Validation}
import anorm._
import play.api.db._
import play.api.libs.json._
import play.api.Play.current
import java.util.UUID

@Singleton
class ApplicationsDao @Inject() (
  @Named("main-actor") mainActor: akka.actor.ActorRef,
  organizationsDao: OrganizationsDao,
  tasksDao: TasksDao
) {

  private[this] val BaseQuery = Query(
    s"""
    select applications.guid, applications.name, applications.key, applications.description, applications.visibility,
           ${AuditsDao.query("applications")},
           organizations.guid as organization_guid,
           organizations.key as organization_key,
           ${AuditsParserDao.queryWithAlias("organizations", "organization")}
      from applications
      join organizations on organizations.guid = applications.organization_guid and organizations.deleted_at is null
    """
  )

  private[this] val InsertQuery = """
    insert into applications
    (guid, organization_guid, name, description, key, visibility, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {name}, {description}, {key}, {visibility}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[this] val UpdateQuery = """
    update applications
       set name = {name},
           visibility = {visibility},
           description = {description},
           updated_by_guid = {updated_by_guid}::uuid
     where guid = {guid}::uuid
  """

  private[this] val InsertMoveQuery = """
    insert into application_moves
    (guid, application_guid, from_organization_guid, to_organization_guid, created_by_guid)
    values
    ({guid}::uuid, {application_guid}::uuid, {from_organization_guid}::uuid, {to_organization_guid}::uuid, {created_by_guid}::uuid)
  """

  private[this] val UpdateOrganizationQuery = """
    update applications
       set organization_guid = {org_guid}::uuid,
           updated_by_guid = {updated_by_guid}::uuid
     where guid = {guid}::uuid
  """

  private[this] val UpdateVisibilityQuery = """
    update applications
       set visibility = {visibility},
           updated_by_guid = {updated_by_guid}::uuid
     where guid = {guid}::uuid
  """

  def validateMove(
    authorization: Authorization,
    app: Application,
    form: MoveForm
  ): Seq[Error] = {
    val orgErrors = organizationsDao.findByKey(authorization, form.orgKey) match {
      case None => Seq(s"Organization[${form.orgKey}] not found")
      case Some(newOrg) => Nil
    }

    val appErrors = findByOrganizationKeyAndApplicationKey(Authorization.All, form.orgKey, app.key) match {
      case None => Nil
      case Some(existing) => {
        if (existing.guid == app.guid) {
          Nil
        } else {
          Seq(s"Organization[${form.orgKey}] already has an application[${app.key}]]")
        }
      }
    }

    Validation.errors(orgErrors ++ appErrors)
  }

  def validate(
    org: Organization,
    form: ApplicationForm,
    existing: Option[Application] = None
  ): Seq[Error] = {
    val nameErrors = findByOrganizationAndName(Authorization.All, org, form.name) match {
      case None => Seq.empty
      case Some(application: Application) => {
        if (existing.map(_.guid) == Some(application.guid)) {
          Seq.empty
        } else {
          Seq("Application with this name already exists")
        }
      }
    }

    val keyErrors = form.key match {
      case None => Seq.empty
      case Some(key) => {
        UrlKey.validate(key) match {
          case Nil => {
            findByOrganizationKeyAndApplicationKey(Authorization.All, org.key, key) match {
              case None => Seq.empty
              case Some(application: Application) => {
                if (existing.map(_.guid) == Some(application.guid)) {
                  Seq.empty
                } else {
                  Seq("Application with this key already exists")
                }
              }
            }
          }
          case errors => errors
        }
      }
    }

    val visibilityErrors = Visibility.fromString(form.visibility.toString) match {
      case Some(_) => Seq.empty
      case None => Seq(s"Visibility[${form.visibility}] not recognized")
    }

    Validation.errors(nameErrors ++ keyErrors ++ visibilityErrors)
  }

  def update(
    updatedBy: User,
    app: Application,
    form: ApplicationForm
  ): Application = {
    val org = organizationsDao.findByGuid(Authorization.User(updatedBy.guid), app.organization.guid).getOrElse {
      sys.error(s"User[${updatedBy.guid}] does not have access to org[${app.organization.guid}]")
    }
    val errors = validate(org, form, Some(app))
    assert(errors.isEmpty, errors.map(_.message).mkString(" "))

    withTasks(updatedBy, app.guid, { implicit c =>
      SQL(UpdateQuery).on(
        'guid -> app.guid,
        'name -> form.name.trim,
        'visibility -> form.visibility.toString,
        'description -> form.description.map(_.trim),
        'updated_by_guid -> updatedBy.guid
      ).execute()
    })

    findByGuid(Authorization.All, app.guid).getOrElse {
      sys.error("Error updating application")
    }
  }

  def move(
    updatedBy: User,
    app: Application,
    form: MoveForm
  ): Application = {
    val errors = validateMove(Authorization.User(updatedBy.guid), app, form)
    assert(errors.isEmpty, errors.map(_.message).mkString(" "))

    if (app.organization.key == form.orgKey) {
      // No-op
      app

    } else {
      organizationsDao.findByKey(Authorization.All, form.orgKey) match {
        case None => sys.error(s"Could not find organization with key[${form.orgKey}]")
        case Some(newOrg) => {
          withTasks(updatedBy, app.guid, { implicit c =>
            SQL(InsertMoveQuery).on(
              'guid -> UUID.randomUUID,
              'application_guid -> app.guid,
              'from_organization_guid -> app.organization.guid,
              'to_organization_guid -> newOrg.guid,
              'created_by_guid -> updatedBy.guid
            ).execute()

            SQL(UpdateOrganizationQuery).on(
              'guid -> app.guid,
              'org_guid -> newOrg.guid,
              'updated_by_guid -> updatedBy.guid
            ).execute()
          })
        }
      }

      findByGuid(Authorization.All, app.guid).getOrElse {
        sys.error("Error updating visibility")
      }
    }
  }

  def setVisibility(
    updatedBy: User,
    app: Application,
    visibility: Visibility
  ): Application = {
    assert(
      canUserUpdate(updatedBy, app),
      "User[${user.guid}] not authorized to update app[${app.key}]"
    )

    withTasks(updatedBy, app.guid, { implicit c =>
      SQL(UpdateVisibilityQuery).on(
        'guid -> app.guid,
        'visibility -> visibility.toString,
        'updated_by_guid -> updatedBy.guid
      ).execute()
    })

    findByGuid(Authorization.All, app.guid).getOrElse {
      sys.error("Error updating visibility")
    }
  }

  def create(
    createdBy: User,
    org: Organization,
    form: ApplicationForm
  ): Application = {
    val errors = validate(org, form)
    assert(errors.isEmpty, errors.map(_.message).mkString(" "))

    val guid = UUID.randomUUID
    val key = form.key.getOrElse(UrlKey.generate(form.name))

    withTasks(createdBy, guid, { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'organization_guid -> org.guid,
        'name -> form.name.trim,
        'description -> form.description.map(_.trim),
        'key -> key,
        'visibility -> form.visibility.toString,
        'created_by_guid -> createdBy.guid,
        'updated_by_guid -> createdBy.guid
      ).execute()
    })

    mainActor ! actors.MainActor.Messages.ApplicationCreated(guid)
    
    findAll(Authorization.All, orgKey = Some(org.key), key = Some(key)).headOption.getOrElse {
      sys.error("Failed to create application")
    }
  }

  def softDelete(deletedBy: User, application: Application) {
    withTasks(deletedBy, application.guid, { c =>
      SoftDelete.delete(c, "applications", deletedBy, application.guid)
    })
  }

  def canUserUpdate(user: User, app: Application): Boolean = {
    findAll(Authorization.User(user.guid), key = Some(app.key)).headOption match {
      case None => false
      case Some(a) => true
    }
  }

  private[db] def findByOrganizationAndName(authorization: Authorization, org: Organization, name: String): Option[Application] = {
    findAll(authorization, orgKey = Some(org.key), name = Some(name), limit = 1).headOption
  }

  def findByOrganizationKeyAndApplicationKey(authorization: Authorization, orgKey: String, applicationKey: String): Option[Application] = {
    findAll(authorization, orgKey = Some(orgKey), key = Some(applicationKey), limit = 1).headOption
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[Application] = {
    findAll(authorization, guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    authorization: Authorization,
    orgKey: Option[String] = None,
    guid: Option[UUID] = None,
    name: Option[String] = None,
    key: Option[String] = None,
    version: Option[Version] = None,
    hasVersion: Option[Boolean] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Application] = {
    DB.withConnection { implicit c =>
      BaseQuery.
        and(authorization.applicationFilter()).
        equals("applications.guid::uuid", guid).
        equals("organizations.key", orgKey).
        and(
          name.map { _ =>
            "lower(trim(applications.name)) = lower(trim({name}))"
          }
        ).bind("name", name).
        and(
          key.map { _ =>
            "applications.key = lower(trim({key}))"
          }
        ).bind("key", key).
        and(
          version.map { _ =>
            "applications.guid = (select application_guid from versions where deleted_at is null and versions.guid = {version_guid}::uuid)"
          }
        ).bind("version_guid", "version_guid").
        and(
          hasVersion.map { v =>
            val clause = "select 1 from versions where versions.deleted_at is null and versions.application_guid = applications.guid"
            v match {
              case true => { s"exists ($clause)" }
              case false => { s"not exists ($clause)" }
            }
          }
        ).
        and(isDeleted.map(Filters2.isDeleted("applications", _))).
        limit(limit).
        offset(offset).
        anormSql().as(
          io.apibuilder.api.v0.anorm.parsers.Application.parser().*
        )
    }
  }

  private[this] def withTasks(
    user: User,
    guid: UUID,
    f: java.sql.Connection => Unit
  ) {
    val taskGuid = DB.withTransaction { implicit c =>
      f(c)
      tasksDao.insert(c, user, TaskDataIndexApplication(guid))
    }
    mainActor ! actors.MainActor.Messages.TaskCreated(taskGuid)
  }

}

/*
object ApplicationsDao {

  def parser(prefix: Option[String] = None): RowParser[Application] = {
    val p = prefix.map( _ + "_").getOrElse("")
    SqlParser.get[UUID](s"${p}guid") ~
    SqlParser.get[UUID](s"organization_guid") ~
    SqlParser.str(s"organization_key") ~
    SqlParser.str(s"${p}name") ~
    SqlParser.str(s"${p}key") ~
    SqlParser.str(s"${p}visibility") ~
    SqlParser.str(s"${p}description") ~
    AuditsDao.parser(prefix) map {
      case guid ~ organizationGuid ~ organizationKey ~ name ~ key ~ visibility ~ description ~ audit => {
        Application(
        Password(
          id = id,
          user = UserReference(id = userId),
          algorithm = algorithm,
          hash = hash
        )
      }
    }
  }  

    prefix: Option[String] = None
  ): Application = {
    val p = prefix.map( _ + "_").getOrElse("")
    Application(
      guid = row[UUID](s"${p}guid"),
      organization = Reference(
        guid = row[UUID]("organization_guid"),
        key = row[String]("organization_key")
      ),
      name = row[String](s"${p}name"),
      key = row[String](s"${p}key"),
      visibility = Visibility(row[String](s"${p}visibility")),
      description = row[Option[String]](s"${p}description"),
      audit = AuditsDao.fromRow(row, prefix)
    )
  }
 */
