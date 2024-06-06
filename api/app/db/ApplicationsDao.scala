package db

import anorm._
import io.apibuilder.api.v0.models.{AppSortBy, Application, ApplicationForm, Error, MoveForm, Organization, SortOrder, User, Version, Visibility}
import io.apibuilder.task.v0.models.TaskType
import io.flow.postgresql.Query
import lib.{UrlKey, Validation}
import play.api.db._

import java.util.UUID
import javax.inject.{Inject, Named, Singleton}

@Singleton
class ApplicationsDao @Inject() (
  @Named("main-actor") mainActor: akka.actor.ActorRef,
  @NamedDatabase("default") db: Database,
  organizationsDao: OrganizationsDao,
  tasksDao: InternalTasksDao,
) {

  private[this] val dbHelpers = DbHelpers(db, "applications")

  private[this] val BaseQuery = Query(
    s"""
    select applications.guid, applications.name, applications.key, applications.description, applications.visibility,
           ${AuditsDao.query("applications")},
           organizations.guid as organization_guid,
           organizations.key as organization_key,
           ${AuditsDao.queryWithAlias("organizations", "organization")},
           coalesce(
             (select versions.created_at
               from versions
               where versions.application_guid = applications.guid
               and versions.deleted_at is null
               order by versions.version_sort_key desc, versions.created_at desc
               limit 1),
             applications.updated_at
           ) as last_updated_at
      from applications
      join organizations on organizations.guid = applications.organization_guid and organizations.deleted_at is null
    """
  )

  private[this] val InsertQuery =
    """
    insert into applications
    (guid, organization_guid, name, description, key, visibility, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {name}, {description}, {key}, {visibility}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[this] val UpdateQuery =
    """
    update applications
       set name = {name},
           visibility = {visibility},
           description = {description},
           updated_by_guid = {updated_by_guid}::uuid
     where guid = {guid}::uuid
  """

  private[this] val InsertMoveQuery =
    """
    insert into application_moves
    (guid, application_guid, from_organization_guid, to_organization_guid, created_by_guid)
    values
    ({guid}::uuid, {application_guid}::uuid, {from_organization_guid}::uuid, {to_organization_guid}::uuid, {created_by_guid}::uuid)
  """

  private[this] val UpdateOrganizationQuery =
    """
    update applications
       set organization_guid = {org_guid}::uuid,
           updated_by_guid = {updated_by_guid}::uuid
     where guid = {guid}::uuid
  """

  private[this] val UpdateVisibilityQuery =
    """
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
      case Some(_) => Nil
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
      case None => Nil
      case Some(application: Application) => {
        if (existing.map(_.guid).contains(application.guid)) {
          Nil
        } else {
          Seq("Application with this name already exists")
        }
      }
    }

    val keyErrors = form.key match {
      case None => Nil
      case Some(key) => {
        UrlKey.validate(key) match {
          case Nil => {
            findByOrganizationKeyAndApplicationKey(Authorization.All, org.key, key) match {
              case None => Nil
              case Some(application: Application) => {
                if (existing.map(_.guid).contains(application.guid)) {
                  Nil
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
      case Some(_) => Nil
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

    withTasks(app.guid, { implicit c =>
      SQL(UpdateQuery).on(
        "guid" -> app.guid,
        "name" -> form.name.trim,
        "visibility" -> form.visibility.toString,
        "description" -> form.description.map(_.trim),
        "updated_by_guid" -> updatedBy.guid
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
          withTasks(app.guid, { implicit c =>
            SQL(InsertMoveQuery).on(
              "guid" -> UUID.randomUUID,
              "application_guid" -> app.guid,
              "from_organization_guid" -> app.organization.guid,
              "to_organization_guid" -> newOrg.guid,
              "created_by_guid" -> updatedBy.guid
            ).execute()

            SQL(UpdateOrganizationQuery).on(
              "guid" -> app.guid,
              "org_guid" -> newOrg.guid,
              "updated_by_guid" -> updatedBy.guid
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

    withTasks(app.guid, { implicit c =>
      SQL(UpdateVisibilityQuery).on(
        "guid" -> app.guid,
        "visibility" -> visibility.toString,
        "updated_by_guid" -> updatedBy.guid
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

    withTasks(guid, { implicit c =>
      SQL(InsertQuery).on(
        "guid" -> guid,
        "organization_guid" -> org.guid,
        "name" -> form.name.trim,
        "description" -> form.description.map(_.trim),
        "key" -> key,
        "visibility" -> form.visibility.toString,
        "created_by_guid" -> createdBy.guid,
        "updated_by_guid" -> createdBy.guid
      ).execute()
    })

    mainActor ! actors.MainActor.Messages.ApplicationCreated(guid)

    findAll(Authorization.All, orgKey = Some(org.key), key = Some(key)).headOption.getOrElse {
      sys.error("Failed to create application")
    }
  }

  def softDelete(deletedBy: User, application: Application): Unit = {
    withTasks(application.guid, { c =>
      dbHelpers.delete(c, deletedBy.guid, application.guid)
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
    offset: Long = 0,
    sorting: Option[AppSortBy] = None,
    ordering: Option[SortOrder] = None
  ): Seq[Application] = {
    db.withConnection { implicit c =>
      val appQuery = authorization.applicationFilter(BaseQuery).
        equals("applications.guid", guid).
        equals("organizations.key", orgKey).
        and(
          name.map { _ =>
            "lower(trim(applications.name)) = lower(trim({name}))"
          }
        ).bind("name", name).
        and(
          key.map { _ =>
            "applications.key = lower(trim({application_key}))"
          }
        ).bind("application_key", key).
        and(
          version.map { _ =>
            "applications.guid = (select application_guid from versions where deleted_at is null and versions.guid = {version_guid}::uuid)"
          }
        ).bind("version_guid", version.map(_.guid.toString)).
        and(
          hasVersion.map { v =>
            val clause = "select 1 from versions where versions.deleted_at is null and versions.application_guid = applications.guid"
            if (v) {
              s"exists ($clause)"
            } else {
              s"not exists ($clause)"
            }
          }
        ).
        and(isDeleted.map(Filters.isDeleted("applications", _))).
        limit(limit).
        offset(offset)
      sorting.fold(appQuery) { sorting =>
        val sort = sorting match {
          case AppSortBy.Visibility => "applications.visibility"
          case AppSortBy.CreatedAt  => "applications.created_at"
          case AppSortBy.UpdatedAt  => "last_updated_at"
          case _                    => "applications.name"
        }
        val ord = ordering.getOrElse(SortOrder.Asc).toString
        appQuery.orderBy(s"$sort $ord")
      }.anormSql().as {
        io.apibuilder.api.v0.anorm.parsers.Application.parser().*
      }
    }
  }

  private[this] def withTasks(
    guid: UUID,
    f: java.sql.Connection => Unit
  ): Unit = {
    db.withTransaction { implicit c =>
      f(c)
      tasksDao.queueWithConnection(c, TaskType.IndexApplication, guid.toString)
    }
  }

}