package models

import db.{Authorization, InternalApplication, OrganizationsDao}
import io.apibuilder.api.v0.models.Application
import io.apibuilder.common.v0.models.Reference

import javax.inject.Inject

class ApplicationsModel @Inject()(
                                        organizationsDao: OrganizationsDao,
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
          description = app.description,
          lastUpdatedAt = app.lastUpdatedAt,
          audit = app.audit
        )
      }
    }
  }               }