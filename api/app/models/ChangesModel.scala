package models

import db.*
import io.apibuilder.api.v0.models.{Change, ChangeVersion, Organization, UserSummary}
import io.apibuilder.common.v0.models.{Audit, Reference, ReferenceGuid}

import java.util.UUID
import javax.inject.Inject

class ChangesModel @Inject()(
  orgDao: InternalOrganizationsDao,
  appDao: InternalApplicationsDao
) {
  def toModel(v: InternalChange): Change = {
    toModels(Seq(v)).head
  }

  def toModels(changes: Seq[InternalChange]): Seq[Change] = {
    val apps = appDao.findAllByGuids(
      Authorization.All,
      changes.map(_.db.applicationGuid).distinct
    )
    val appsByGuid = apps.map { a => a.guid -> a }.toMap
    val orgsByGuid = orgDao.findAllByGuids(
      Authorization.All,
      apps.map(_.db.organizationGuid).distinct
    ).map { o => o.guid -> o }.toMap

    changes.flatMap { change =>
      for {
        app <- appsByGuid.get(change.db.applicationGuid)
        org <- orgsByGuid.get(app.db.organizationGuid)
      } yield {
        Change(
          guid = change.guid,
          organization = Reference(guid = org.guid, key = org.key),
          application = Reference(guid = app.guid, key = app.key),
          fromVersion = ChangeVersion(),
          toVersion = ChangeVersion(),
          diff = change.diff,
          changedAt = change.db.changedAt,
          changedBy = UserSummary(),
          audit = Audit(
            createdAt = change.db.createdAt,
            createdBy = ReferenceGuid(change.db.createdByGuid),
            updatedAt = change.db.updatedAt,
            updatedBy = ReferenceGuid(change.db.createdByGuid),
          )
        )
      }
    }
  }
}