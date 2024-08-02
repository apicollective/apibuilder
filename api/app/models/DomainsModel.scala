package models

import db.*
import io.apibuilder.api.v0.models.Domain
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}

import java.util.UUID
import javax.inject.Inject

class DomainsModel @Inject()(
  domainsDao: InternalOrganizationDomainsDao,
) {
  def toModel(v: InternalOrganizationDomain): Domain = {
    toModels(Seq(v)).head
  }

  def toModels(domains: Seq[InternalOrganizationDomain]): Seq[Domain] = {
    domains.map { dom =>
      Domain(
        name = dom.db.domain,
      )
    }
  }
}