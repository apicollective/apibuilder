package models

import db.{Authorization, InternalApplication, InternalOrganizationsDao}
import io.apibuilder.api.v0.models.Application
import io.apibuilder.common.v0.models.{Audit, Reference, ReferenceGuid}
import io.flow.postgresql.Query
import org.joda.time.DateTime
import play.api.db.Database

import java.util.UUID
import javax.inject.Inject

class ApplicationsModel @Inject()(
  db: Database,
  organizationsDao: InternalOrganizationsDao,
) {
  def toModel(application: InternalApplication): Option[Application] = {
    toModels(Seq(application)).headOption
  }

  def toModels(applications: Seq[InternalApplication]): Seq[Application] = {
    val organizations = organizationsDao.findAll(
      Authorization.All,
      guids = Some(applications.map(_.organizationGuid).distinct),
      limit = None
    ).map { o => o.guid -> o }.toMap
    val lastUpdated = lookupLastVersionCreatedAt(applications.map(_.guid)).map { d =>
      d.applicationGuid -> d.versionCreatedAt
    }.toMap

    applications.flatMap { app =>
      organizations.get(app.organizationGuid).map { org =>
        Application(
          guid = app.guid,
          organization = Reference(guid = org.guid, key = org.key),
          name = app.name,
          key = app.key,
          visibility = app.visibility,
          description = app.description,
          lastUpdatedAt = lastUpdated.getOrElse(app.guid, app.db.updatedAt),
          audit = Audit(
            createdAt = org.db.createdAt,
            createdBy = ReferenceGuid(org.db.createdByGuid),
            updatedAt = org.db.updatedAt,
            updatedBy = ReferenceGuid(org.db.updatedByGuid),
          )
        )
      }
    }
  }

  private case class LastVersionCreated(applicationGuid: UUID, versionCreatedAt: DateTime)
  private val LastVersionCreatedQuery = Query("select application_guid::text, max(created_at) from version")
  private def lookupLastVersionCreatedAt(guids: Seq[UUID]): Seq[LastVersionCreated] = {
    db.withConnection { c =>
      LastVersionCreatedQuery.in("application_guid", guids)
        .as(parser.*)(c)
    }
  }

  private val parser: anorm.RowParser[LastVersionCreated] = {
    import anorm.*

    SqlParser.str("application_guid") ~
      SqlParser.get[org.joda.time.DateTime]("created_at") map { case applicationGuid ~ versionCreatedAt =>
      LastVersionCreated(
        applicationGuid = java.util.UUID.fromString(applicationGuid),
        versionCreatedAt = versionCreatedAt
      )
    }
  }
}