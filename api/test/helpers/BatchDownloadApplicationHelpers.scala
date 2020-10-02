package helpers

import io.apibuilder.api.v0.models.{BatchDownloadApplicationForm, BatchDownloadApplicationsForm}

trait BatchDownloadApplicationHelpers extends RandomHelpers {

  def makeBatchDownloadApplicationsForm(
    applications: Seq[io.apibuilder.api.v0.models.BatchDownloadApplicationForm],
  ): BatchDownloadApplicationsForm = {
    BatchDownloadApplicationsForm(
      applications = applications,
    )
  }

  def makeBatchDownloadApplicationForm(
    applicationKey: String = randomString(),
    version: String = "latest",
  ): BatchDownloadApplicationForm = {
    BatchDownloadApplicationForm(
      applicationKey = applicationKey,
      version = version,
    )
  }

}
