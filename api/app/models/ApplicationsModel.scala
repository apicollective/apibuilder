package models

import db.{Authorization, InternalApplication, InternalOrganizationsDao}
import io.apibuilder.api.v0.models.Application
import io.apibuilder.common.v0.models.{Audit, Reference, ReferenceGuid}

import javax.inject.Inject

class ApplicationsModel @Inject()(
                                   organizationsDao: InternalOrganizationsDao,
                                        ) {
  def toModel(application: InternalApplication): Option[Application] = {
    toModels(Seq(application)).headOption
  }

  def toModels(applications: Seq[InternalApplication]): Seq[Application] = {
    val organizations = organizationsDao.findAll(
      Authorization.All,
      guids = Some(applications.map(_.organizationGuid).toSeq.distinct),
      limit = None
    ).map { o => o.guid -> o }.toMap

    applications.flatMap { app =>
      organizations.get(app.organizationGuid).map { org =>
        Application(
          guid = app.guid,
          organization = Reference(guid = org.guid, key = org.key),
          name = app.name,
          key = app.key,
          visibility = app.visibility,
          description = app.db.description,
          lastUpdatedAt = app.db.updatedAt,
          audit = Audit(
            createdAt = org.db.createdAt,
            createdBy = ReferenceGuid(org.db.createdByGuid),
            updatedAt = org.db.updatedAt,
            updatedBy = ReferenceGuid(org.db.updatedByGuid),
          )
        )
      }
    }
  }               }