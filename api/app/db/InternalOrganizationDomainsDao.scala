package db

import db.generated.OrganizationDomainsDao
import io.apibuilder.api.v0.models.Domain
import io.flow.postgresql.{OrderBy, Query}
import play.api.db.*

import java.util.UUID
import javax.inject.Inject

case class InternalOrganizationDomain(db: generated.OrganizationDomain) {
  val guid: UUID = db.guid
  val organizationGuid: UUID = db.organizationGuid
}

class InternalOrganizationDomainsDao @Inject()(
  dao: OrganizationDomainsDao
) {

  def create(createdBy: InternalUser, org: InternalOrganization, domainName: String): InternalOrganizationDomain = {
    dao.db.withConnection { implicit c =>
      create(c, createdBy, org.guid, domainName)
    }
  }

  private[db] def create(implicit c: java.sql.Connection, createdBy: InternalUser, orgGuid: UUID, domainName: String): InternalOrganizationDomain = {
    val guid = dao.insert(c, createdBy.guid, generated.OrganizationDomainForm(
      organizationGuid = orgGuid,
      domain = domainName.trim
    ))
    InternalOrganizationDomain(
      dao.findByGuidWithConnection(c, guid).get
    )
  }

  def softDelete(deletedBy: InternalUser, domain: InternalOrganizationDomain): Unit = {
    dao.delete(deletedBy.guid, domain.db)
  }

  def findAllByDomain(domain: String): Seq[InternalOrganizationDomain] = {
    findAll(domain = Some(domain), limit = None)
  }

  def findAll(
    guid: Option[UUID] = None,
    organizationGuid: Option[UUID] = None,
    organizationGuids: Option[Seq[UUID]] = None,
    domain: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Option[Long],
    orderBy: Option[OrderBy] = None
  ): Seq[InternalOrganizationDomain] = {
    dao.findAll(
      guid = guid,
      organizationGuid = organizationGuid,
      organizationGuids = organizationGuids,
      domain = domain.map(_.trim.toLowerCase()),
      limit = limit,
      orderBy = orderBy,
    ) { q =>
      q.and(isDeleted.map(Filters.isDeleted("organization_domains", _)))
    }.map(InternalOrganizationDomain(_))
  }
  
}
