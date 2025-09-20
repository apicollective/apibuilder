package db

import cats.data.ValidatedNec
import cats.implicits.*
import db.generated.{ApplicationMoveForm, ApplicationMovesDao, ApplicationsDao}
import io.apibuilder.api.v0.models.{AppSortBy, ApplicationForm, Error, MoveForm, SortOrder, Version, Visibility}
import io.apibuilder.task.v0.models.{EmailDataApplicationCreated, TaskType}
import io.flow.postgresql.{OrderBy, Query}
import lib.{UrlKey, Validation}
import processor.EmailProcessorQueue
import util.OptionalQueryFilter

import java.sql.Connection
import java.util.UUID
import javax.inject.Inject

case class InternalApplication(db: generated.Application) {
  val guid: UUID = db.guid
  val organizationGuid: UUID = db.organizationGuid
  val name: String = db.name
  val key: String = db.key
  val description: Option[String] = db.description
  val visibility: Visibility = Visibility(db.visibility)
}

class InternalApplicationsDao @Inject()(
  dao: ApplicationsDao,
  emailQueue: EmailProcessorQueue,
  organizationsDao: InternalOrganizationsDao,
  tasksDao: InternalTasksDao,
  movesDao: ApplicationMovesDao,
) {

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
    updatedBy: InternalUser,
    app: InternalApplication,
    form: ApplicationForm
  ): InternalApplication = {
    val org = organizationsDao.findByGuid(Authorization.User(updatedBy.guid), app.organizationGuid).getOrElse {
      sys.error(s"User[${updatedBy.guid}] does not have access to org[${app.organizationGuid}]")
    }
    val errors = validate(org, form, Some(app))
    assert(errors.isEmpty, errors.map(_.message).mkString(" "))

    withTasks(app.guid, { implicit c =>
      dao.update(c, updatedBy.guid, app.db, app.db.form.copy(
        name = form.name.trim,
        visibility = form.visibility.toString,
        description = form.description.map(_.trim).filterNot(_.isEmpty)
      ))
    })

    findByGuid(Authorization.All, app.guid).getOrElse {
      sys.error("Error updating application")
    }
  }

  def move(
    updatedBy: InternalUser,
    app: InternalApplication,
    form: MoveForm
  ): ValidatedNec[Error, InternalApplication] = {
    validateMove(Authorization.User(updatedBy.guid), app, form).map { newOrg =>
      if (newOrg.guid == app.organizationGuid) {
        // No-op
        app

      } else {
        withTasks(app.guid, { implicit c =>
          movesDao.insert(c, updatedBy.guid, ApplicationMoveForm(
            applicationGuid = app.guid,
            fromOrganizationGuid = app.organizationGuid,
            toOrganizationGuid = newOrg.guid
          ))

          dao.update(updatedBy.guid, app.db, app.db.form.copy(
            organizationGuid = newOrg.guid
          ))
        })

        findByGuid(Authorization.All, app.guid).getOrElse {
          sys.error("Error moving application")
        }
      }
    }
  }

  def setVisibility(
    updatedBy: InternalUser,
    app: InternalApplication,
    visibility: Visibility
  ): InternalApplication = {
    assert(
      canUserUpdate(updatedBy, app),
      s"User[${updatedBy.guid}] not authorized to update app[${app.key}]"
    )

    withTasks(app.guid, { implicit c =>
      dao.update(c, updatedBy.guid, app.db, app.db.form.copy(
        visibility = visibility.toString
      ))
    })

    findByGuid(Authorization.All, app.guid).getOrElse {
      sys.error("Error updating visibility")
    }
  }

  def create(
    createdBy: InternalUser,
    org: InternalOrganization,
    form: ApplicationForm
  ): InternalApplication = {
    val errors = validate(org, form)
    assert(errors.isEmpty, errors.map(_.message).mkString(" "))

    val key = form.key.getOrElse(UrlKey.generate(form.name))

    val guid = dao.db.withTransaction { c =>
      val guid = dao.insert(c, createdBy.guid, db.generated.ApplicationForm(
        organizationGuid = org.guid,
        name = form.name.trim,
        key = key,
        visibility = form.visibility.toString,
        description = form.description.map(_.trim).filterNot(_.isEmpty)
      ))
      emailQueue.queueWithConnection(c, EmailDataApplicationCreated(guid))
      queueTasks(c, guid)
      guid
    }

    findByGuid(Authorization.All, guid).getOrElse {
      sys.error("Failed to create application")
    }
  }

  def softDelete(deletedBy: InternalUser, app: InternalApplication): Unit = {
    withTasks(app.guid, { c =>
      dao.delete(c, deletedBy.guid, app.db)
    })
  }

  def canUserUpdate(user: InternalUser, app: InternalApplication): Boolean = {
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

  def findAllByGuids(authorization: Authorization, guids: Seq[UUID]): Seq[InternalApplication] = {
    findAll(authorization, guids = Some(guids), limit = None)
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
      },
      new OptionalQueryFilter(isDeleted) {
        override def filter(q: Query, value: Boolean): Query = {
          if (value) {
            q.isNotNull("deleted_at")
          } else {
            q.isNull("deleted_at")
          }
        }
      },
      new OptionalQueryFilter(version) {
        override def filter(q: Query, v: Version): Query = {
          q.in(
            "guid",
            Query("select application_guid from versions")
              .isNull("deleted_at")
              .equals("guid", v.guid)
          )
        }
      },
      new OptionalQueryFilter(hasVersion) {
        override def filter(q: Query, value: Boolean): Query = {
          val clause = "select 1 from versions where versions.deleted_at is null and versions.application_guid = applications.guid"
          if (value) {
            q.and(s"exists ($clause)")
          } else {
            q.and(s"not exists ($clause)")
          }
        }
      }
    )

    dao.findAll(
      guid = guid,
      guids = guids,
      limit = limit,
      offset = offset,
      orderBy = Some(toOrderBy(sorting, ordering)),
    )( using (q: Query) => {
      authorization.applicationFilter(
        filters.foldLeft(q) { case (q, f) => f.filter(q) },
        "guid"
      )
      .equals("lower(name)", name.map(_.toLowerCase().trim))
      .equals("lower(key)", key.map(_.toLowerCase().trim))
    }).map(InternalApplication(_))
  }

  private def toOrderBy(
    sorting: Option[AppSortBy] = None,
    ordering: Option[SortOrder] = None
  ): OrderBy = {
    val sort = sorting.getOrElse(AppSortBy.Name) match {
      case AppSortBy.Visibility => "visibility"
      case AppSortBy.CreatedAt => "created_at"
      case AppSortBy.UpdatedAt => "updated_at"
      case AppSortBy.Name | AppSortBy.UNDEFINED(_) => "name"
    }

    val ord = ordering.getOrElse(SortOrder.Asc) match {
      case SortOrder.Desc => "-"
      case SortOrder.Asc | SortOrder.UNDEFINED(_) => ""
    }

    OrderBy(s"$ord$sort")
  }

  private def withTasks(
    guid: UUID,
    f: java.sql.Connection => Unit
  ): Unit = {
    dao.db.withTransaction { implicit c =>
      f(c)
      queueTasks(c, guid)
    }
  }

  private def queueTasks(c: Connection, guid: UUID): Unit = {
    tasksDao.queueWithConnection(c, TaskType.IndexApplication, guid.toString)
  }

}
