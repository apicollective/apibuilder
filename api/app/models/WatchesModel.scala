package models

import cats.implicits._
import db.{InternalApplicationsDao, Authorization, InternalWatch, InternalOrganizationsDao, InternalUsersDao}
import io.apibuilder.api.v0.models.Watch

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

    val organizations = organizationsModel.toModels(organizationsDao.findAll(
      Authorization.All,
      guids = Some(applications.values.map(_.organization.guid).toSeq.distinct),
      limit = None
    )).map { o => o.guid -> o }.toMap

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
          audit = w.audit
        )
      }
    }
  }

}