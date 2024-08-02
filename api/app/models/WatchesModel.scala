package models

import cats.implicits.*
import db.{Authorization, InternalApplicationsDao, InternalOrganizationsDao, InternalUsersDao, InternalWatch}
import io.apibuilder.api.v0.models.Watch
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}

import javax.inject.Inject

class WatchesModel @Inject()(
  organizationsDao: InternalOrganizationsDao,
  applicationsDao: InternalApplicationsDao,
  applicationsModel: ApplicationsModel,
  usersModel: UsersModel,
  organizationsModel: OrganizationsModel,
) {
  def toModel(watch: InternalWatch): Option[Watch] = {
    toModels(Seq(watch)).headOption
  }

  def toModels(watches: Seq[InternalWatch]): Seq[Watch] = {
    val users = usersModel.toModelByGuids(watches.map(_.userGuid)).map { u => u.guid -> u }.toMap

    val applications = applicationsModel.toModels(
      applicationsDao.findAll(
        Authorization.All,
        guids = Some(watches.map(_.applicationGuid).distinct),
        limit = None
      )
    ).map { o => o.guid -> o }.toMap

    val organizations = organizationsModel.toModelByGuids(
      Authorization.All,
      applications.values.map(_.organization.guid).toSeq
    ).map { o => o.guid -> o }.toMap

    watches.flatMap { w =>
      for {
        user <- users.get(w.userGuid)
        app <- applications.get(w.applicationGuid)
        org <- organizations.get(app.organization.guid)
      } yield {
        Watch(
          guid = w.guid,
          user = user,
          organization = org,
          application = app,
          audit = Audit(
            createdAt = w.db.createdAt,
            createdBy = ReferenceGuid(w.db.createdByGuid),
            updatedAt = w.db.updatedAt,
            updatedBy = ReferenceGuid(w.db.createdByGuid),
          )
        )
      }
    }
  }

}