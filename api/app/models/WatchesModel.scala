package models

import cats.implicits._
import db.{ApplicationsDao, Authorization, InternalWatch, InternalOrganizationsDao, UsersDao}
import io.apibuilder.api.v0.models.Watch

import javax.inject.Inject

class WatchesModel @Inject()(
                              organizationsDao: InternalOrganizationsDao,
                              applicationsDao: ApplicationsDao,
                              applicationsModel: ApplicationsModel,
                              usersDao: UsersDao
                                        ) {
  def toModel(watch: InternalWatch): Option[Watch] = {
    toModels(Seq(watch)).headOption
  }

  def toModels(watches: Seq[InternalWatch]): Seq[Watch] = {
    val users = usersDao.findAll(
      guids = Some(watches.map(_.userGuid))
    ).map { u => u.guid -> u }.toMap

    val applications = applicationsModel.toModels (
      applicationsDao.findAll(
        Authorization.All,
        guids = Some(watches.map(_.applicationGuid).distinct),
        limit = None
      )
    ).map { o => o.guid -> o }.toMap

    val organizations = organizationsDao.findAll(
      Authorization.All,
      guids = Some(applications.values.map(_.organization.guid).toSeq.distinct),
      limit = None
    ).map { o => o.guid -> o }.toMap

    watches.flatMap { w =>
      (users.get(w.userGuid),
        applications.get(w.applicationGuid).flatMap { a =>
          organizations.get(a.organization.guid).map { o => (o, a) }
        },
      ).mapN { case (user, (org, app)) =>
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