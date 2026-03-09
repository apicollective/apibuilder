package services

import cats.data.ValidatedNec
import cats.implicits.*
import db.InternalVersionsDao
import io.apibuilder.api.v0.models.{BatchVersionLatest, BatchVersionsLatest, BatchVersionsLatestForm}

import javax.inject.Inject

class BatchVersionsLatestService @Inject() (
  versionsDao: InternalVersionsDao,
) {

  private val MaxApplicationKeys = 500

  def process(
    orgKey: String,
    form: BatchVersionsLatestForm,
  ): ValidatedNec[String, BatchVersionsLatest] = {
    if (form.applicationKeys.length > MaxApplicationKeys) {
      s"Maximum of $MaxApplicationKeys application keys allowed, but ${form.applicationKeys.length} were provided".invalidNec
    } else {
      val latestVersions = versionsDao.findLatestVersions(orgKey, form.applicationKeys)

      BatchVersionsLatest(
        applications = form.applicationKeys.map { key =>
          BatchVersionLatest(
            applicationKey = key,
            latestVersion = latestVersions.get(key),
          )
        },
      ).validNec
    }
  }
}
