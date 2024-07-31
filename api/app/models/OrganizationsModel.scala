package models

import db.{Authorization, InternalOrganization, OrganizationDomainsDao}
import io.apibuilder.api.v0.models.Organization
import io.apibuilder.common.v0.models.{Audit, Reference, ReferenceGuid}

import javax.inject.Inject

class OrganizationsModel @Inject()(
                               domainsDao: OrganizationDomainsDao,
                                        ) {
  def toModel(v: InternalOrganization): Option[Organization] = {
    toModels(Seq(v)).headOption
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
        domains = domains.getOrElse(org.guid, Nil)ø,
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