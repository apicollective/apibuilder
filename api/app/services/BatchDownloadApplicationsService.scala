package services

import cats.data.ValidatedNec
import db.{InternalApplicationsDao, Authorization, InternalVersionsDao}
import cats.implicits._
import io.apibuilder.api.v0.models.{BatchDownloadApplicationForm, BatchDownloadApplications, BatchDownloadApplicationsForm, Version}
import models.VersionsModel

import javax.inject.Inject

class BatchDownloadApplicationsService @Inject() (
                                                   applicationsDao: InternalApplicationsDao,
                                                   versionsDao: InternalVersionsDao,
                                                   versionsModel: VersionsModel,
) {

  def process(
    auth: Authorization,
    orgKey: String,
    form: BatchDownloadApplicationsForm,
  ): ValidatedNec[String, BatchDownloadApplications] = {
    form.applications.map { f =>
      validateForm(auth, orgKey, f)
    }.toList.traverse(identity).map { versions =>
      BatchDownloadApplications(
        applications = versions,
      )
    }
  }

  private def validateForm(
    auth: Authorization,
    orgKey: String,
    form: BatchDownloadApplicationForm,
  ): ValidatedNec[String, Version] = {
    applicationsDao.findByOrganizationKeyAndApplicationKey(auth, orgKey, form.applicationKey) match {
      case None => s"Cannot find application with key '${form.applicationKey}'".invalidNec
      case Some(a) => {
        versionsDao.findVersion(
          auth,
          orgKey = orgKey,
          applicationKey = a.key,
          version = form.version,
        ).flatMap(versionsModel.toModel) match {
          case None => s"Cannot find version '${form.version}' for application with key '${form.applicationKey}'".invalidNec
          case Some(v) => v.validNec
        }
      }
    }
  }
}
