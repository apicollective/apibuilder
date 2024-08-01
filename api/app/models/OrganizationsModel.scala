package models

import db.{Authorization, InternalOrganization, InternalOrganizationsDao, OrganizationDomainsDao}
import io.apibuilder.api.v0.models.Organization
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}

import java.util.UUID
import javax.inject.Inject

class OrganizationsModel @Inject()(
  orgDao: InternalOrganizationsDao,
  domainsDao: OrganizationDomainsDao,
) {
  def toModelByGuids(auth: Authorization, guids: Seq[UUID]): Seq[Organization] = {
    toModels(orgDao.findAll(
      auth,
      guids = Some(guids),
      limit = None
    ))
  }

  def toModel(v: InternalOrganization): Organization = {
    toModels(Seq(v)).head
  }

  def toModels(orgs: Seq[InternalOrganization]): Seq[Organization] = {
    val domains = domainsDao.findAll(
      organizationGuids = Some(orgs.map(_.guid)),
    ).groupBy(_.organizationGuid)

    orgs.map { org =>
      Organization(
        guid = org.guid,
        name = org.name,
        key = org.key,
        namespace = org.db.namespace,
        visibility = org.visibility,
        domains = domains.getOrElse(org.guid, Nil).map(_.domain),
        audit = Audit(
          createdAt = org.db.createdAt,
          createdBy = ReferenceGuid(org.db.createdByGuid),
          updatedAt = org.db.updatedAt,
          updatedBy = ReferenceGuid(org.db.updatedByGuid),
        )
      )
    }
  }
}