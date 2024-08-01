package helpers

import db.InternalOrganization
import io.apibuilder.api.v0.models.{BatchDownloadApplicationForm, BatchDownloadApplicationsForm, Organization}
import models.OrganizationsModel
import org.scalatestplus.play.PlaySpec

trait OrganizationHelpers extends db.Helpers {

  private def orgModel: OrganizationsModel = injector.instanceOf[OrganizationsModel]
  def toModel(org: InternalOrganization): Organization = {
    orgModel.toModel(org)
  }

}
