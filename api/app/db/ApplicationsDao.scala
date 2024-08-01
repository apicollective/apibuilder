package db

import anorm._
import cats.data.ValidatedNec
import cats.implicits._
import io.apibuilder.api.v0.models.{AppSortBy, ApplicationForm, Error, MoveForm, SortOrder, User, Version, Visibility}
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}
import io.apibuilder.task.v0.models.{EmailDataApplicationCreated, TaskType}
import io.flow.postgresql.Query
import lib.{UrlKey, Validation}
import org.joda.time.DateTime
import play.api.db._
import processor.EmailProcessorQueue
import util.OptionalQueryFilter

import java.util.UUID
import javax.inject.{Inject, Singleton}

case class InternalApplication(
                              guid: UUID,
                              name: String,
                              key: String,
                              description: Option[String],
                              visibility: Visibility,
                              organizationGuid: UUID,
                              lastUpdatedAt: DateTime,
                              audit: Audit
                              )

@Singleton
class ApplicationsDao @Inject() (
                                  @NamedDatabase("default") db: Database,
                                  emailQueue: EmailProcessorQueue,
                                  organizationsDao: InternalOrganizationsDao,
                                  tasksDao: InternalTasksDao,
) {

  private val dbHelpers = DbHelpers(db, "applications")

  private val BaseQuery = Query(
    s"""
    select guid, name, key, description, visibility, organization_guid,
           ${AuditsDao.query("applications")},
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
    """
  )

  private val InsertQuery =
    """
    insert into applications
    (guid, organization_guid, name, description, key, visibility, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {name}, {description}, {key}, {visibility}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private val UpdateQuery =
    """
    update applications
       set name = {name},
           visibility = {visibility},
           description = {description},
           updated_by_guid = {updated_by_guid}::uuid
     where guid = {guid}::uuid
  """

  private val InsertMoveQuery =
    """
    insert into application_moves
    (guid, application_guid, from_organization_guid, to_organization_guid, created_by_guid)
    values
    ({guid}::uuid, {application_guid}::uuid, {from_organization_guid}::uuid, {to_organization_guid}::uuid, {created_by_guid}::uuid)
  """

  private val UpdateOrganizationQuery =
    """
    update applications
       set organization_guid = {org_guid}::uuid,
           updated_by_guid = {updated_by_guid}::uuid
     where guid = {guid}::uuid
  """

  private val UpdateVisibilityQuery =
    """
    update applications
       set visibility = {visibility},
           updated_by_guid = {updated_by_guid}::uuid
     where guid = {guid}::uuid
  """

  private def validateOrganizationByKey(auth: Authorization, key: String): ValidatedNec[Error, InternalOrganization] = {
    organizationsDao.findByKey(auth, key).toValidNec(Validation.singleError(s"Organization[$key] not found"))
  }


  private def validateMove(
    authorization: Authorization,
    app: InternalApplication,
    form: MoveForm
  ): ValidatedNec[Error, InternalOrganization] = {
    validateOrganizationByKey(authorization, form.orgKey).andThen { org =>
      findByOrganizationKeyAndApplicationKey(Authorization.All, org.key, app.key) match {
        case None => org.validNec
        case Some(existing) => {
          if (existing.guid == app.guid) {
            org.validNec
          } else {
            Validation.singleError(s"Organization[${form.orgKey}] already has an application[${app.key}]]").invalidNec
          }
        }
      }
    }
  }

  def validate(
    org: InternalOrganization,
    form: ApplicationForm,
    existing: Option[InternalApplication] = None
  ): Seq[Error] = {
    val nameErrors = findByOrganizationAndName(Authorization.All, org, form.name) match {
      case None => Nil
      case Some(app) => {
        if (existing.map(_.guid).contains(app.guid)) {
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
              case Some(app) => {
                if (existing.map(_.guid).contains(app.guid)) {
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
    app: InternalApplication,
    form: ApplicationForm
  ): InternalApplication = {
    val org = organizationsDao.findByGuid(Authorization.User(updatedBy.guid), app.organizationGuid).getOrElse {
      sys.error(s"User[${updatedBy.guid}] does not have access to org[${app.organizationGuid}]")
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
    app: InternalApplication,
    form: MoveForm
  ): ValidatedNec[Error, InternalApplication] = {
    validateMove(Authorization.User(updatedBy.guid), app, form).map { newOrg =>
      if (newOrg.guid == app.organizationGuid) {
        // No-op
        app

      } else {
        withTasks(app.guid, { implicit c =>
          SQL(InsertMoveQuery).on(
            "guid" -> UUID.randomUUID,
            "application_guid" -> app.guid,
            "from_organization_guid" -> app.organizationGuid,
            "to_organization_guid" -> newOrg.guid,
            "created_by_guid" -> updatedBy.guid
          ).execute()

          SQL(UpdateOrganizationQuery).on(
            "guid" -> app.guid,
            "org_guid" -> newOrg.guid,
            "updated_by_guid" -> updatedBy.guid
          ).execute()
        })

        findByGuid(Authorization.All, app.guid).getOrElse {
          sys.error("Error moving application")
        }
      }
    }
  }

  def setVisibility(
    updatedBy: User,
    app: InternalApplication,
    visibility: Visibility
  ): InternalApplication = {
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
    org: InternalOrganization,
    form: ApplicationForm
  ): InternalApplication = {
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
      emailQueue.queueWithConnection(c, EmailDataApplicationCreated(guid))
    })

    findAll(Authorization.All, orgKey = Some(org.key), key = Some(key), limit = Some(1)).headOption.getOrElse {
      sys.error("Failed to create application")
    }
  }

  def softDelete(deletedBy: User, application: InternalApplication): Unit = {
    withTasks(application.guid, { c =>
      dbHelpers.delete(c, deletedBy.guid, application.guid)
    })
  }

  def canUserUpdate(user: User, app: InternalApplication): Boolean = {
    findAll(Authorization.User(user.guid), key = Some(app.key), limit = Some(1)).nonEmpty
  }

  private[db] def findByOrganizationAndName(authorization: Authorization, org: InternalOrganization, name: String): Option[InternalApplication] = {
    findAll(authorization, orgKey = Some(org.key), name = Some(name), limit = Some(1)).headOption
  }

  def findByOrganizationKeyAndApplicationKey(authorization: Authorization, orgKey: String, applicationKey: String): Option[InternalApplication] = {
    findAll(authorization, orgKey = Some(orgKey), key = Some(applicationKey), limit = Some(1)).headOption
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[InternalApplication] = {
    findAll(authorization, guid = Some(guid), limit = Some(1)).headOption
  }

  def findAll(
    authorization: Authorization,
    orgKey: Option[String] = None,
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    name: Option[String] = None,
    key: Option[String] = None,
    version: Option[Version] = None,
    hasVersion: Option[Boolean] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Option[Long],
    offset: Long = 0,
    sorting: Option[AppSortBy] = None,
    ordering: Option[SortOrder] = None
  ): Seq[InternalApplication] = {
    val filters = List(
      new OptionalQueryFilter(orgKey) {
        override def filter(q: Query, value: String): Query = {
          q.in("organization_guid", Query("select guid from organizations").equals("key", orgKey))
        }
      }
    )

    db.withConnection { implicit c =>
      val appQuery = authorization.applicationFilter(
          filters.foldLeft(BaseQuery) { case (q, f) => f.filter(q) },
          "guid"
        ).
        equals("guid", guid).
        optionalIn("guid", guids).
        and(
          name.map { _ =>
            "lower(trim(name)) = lower(trim({name}))"
          }
        ).bind("name", name).
        and(
          key.map { _ =>
            "key = lower(trim({application_key}))"
          }
        ).bind("application_key", key).
        and(
          version.map { _ =>
            "guid = (select application_guid from versions where deleted_at is null and versions.guid = {version_guid}::uuid)"
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
        optionalLimit(limit).
        offset(offset)
      sorting.fold(appQuery) { sorting =>
        val sort = sorting match {
          case AppSortBy.Visibility => "visibility"
          case AppSortBy.CreatedAt => "created_at"
          case AppSortBy.UpdatedAt => "last_updated_at"
          case _ => "name"
        }
        val ord = ordering.getOrElse(SortOrder.Asc).toString
        appQuery.orderBy(s"$sort $ord")
      }.as(parser.*)
    }
  }

  private val parser: RowParser[InternalApplication] = {
    import org.joda.time.DateTime

    SqlParser.get[UUID]("guid") ~
      SqlParser.get[UUID]("organization_guid") ~
      SqlParser.str("name") ~
      SqlParser.str("key") ~
      SqlParser.str("visibility") ~
      SqlParser.str("description").? ~
      SqlParser.get[DateTime]("last_updated_at") ~
      SqlParser.get[DateTime]("created_at") ~
      SqlParser.get[UUID]("created_by_guid") ~
      SqlParser.get[DateTime]("updated_at") ~
      SqlParser.get[UUID]("updated_by_guid") map { case guid ~ organizationGuid ~ name ~ key ~ visibility ~ description ~ lastUpdatedAt ~ createdAt ~ createdByGuid ~ updatedAt ~ updatedByGuid => {
        InternalApplication(
          guid = guid,
          organizationGuid = organizationGuid,
          name = name,
          key = key,
          visibility = Visibility(visibility),
          description = description,
          lastUpdatedAt = lastUpdatedAt,
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

  private def withTasks(
    guid: UUID,
    f: java.sql.Connection => Unit
  ): Unit = {
    db.withTransaction { implicit c =>
      f(c)
      tasksDao.queueWithConnection(c, TaskType.IndexApplication, guid.toString)
    }
  }

}