package services

import cats.data.ValidatedNec
import cats.implicits._
import db.{ApplicationsDao, Authorization, VersionsDao}
import io.apibuilder.api.v0.models.Version

import javax.inject.Inject

class BatchesService @Inject()(
  applicationsDao: ApplicationsDao,
  versionsDao: VersionsDao,
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

  private[this] def validateForm(
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
        ) match {
          case None => s"Cannot find version '${form.version}' for application with key '${form.applicationKey}'".invalidNec
          case Some(v) => v.validNec
        }
      }
    }
  }
}
