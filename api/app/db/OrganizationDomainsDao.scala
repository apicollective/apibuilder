package db

import com.gilt.apidoc.api.v0.models.{Domain, Organization, User}
import lib.{Role, UrlKey}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

case class OrganizationDomain(guid: String, organization_guid: UUID, domain: String) {

  def toDomain(): Domain = {
    Domain(domain)
  }

}

object OrganizationDomainsDao {

  private val BaseQuery = """
    select guid::varchar, organization_guid, domain
      from organization_domains
     where true
  """

  def create(createdBy: User, org: Organization, domainName: String): OrganizationDomain = {
    DB.withConnection { implicit c =>
      create(c, createdBy, org, domainName)
    }
  }

  private[db] def create(implicit c: java.sql.Connection, createdBy: User, org: Organization, domainName: String): OrganizationDomain = {
    val domain = OrganizationDomain(
      guid = UUID.randomUUID.toString,
      organization_guid = org.guid,
      domain = domainName
    )

    SQL("""
      insert into organization_domains
      (guid, organization_guid, domain, created_by_guid)
      values
      ({guid}::uuid, {organization_guid}::uuid, {domain}, {created_by_guid}::uuid)
    """).on(
      'guid -> domain.guid,
      'organization_guid -> domain.organization_guid,
      'domain -> domain.domain,
      'created_by_guid -> createdBy.guid
    ).execute()

    domain
  }

  def softDelete(deletedBy: User, domain: OrganizationDomain) {
    SoftDelete.delete("organization_domains", deletedBy, domain.guid)
  }

  def findAll(
    guid: Option[String] = None,
    organizationGuid: Option[UUID] = None,
    domain: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false)
  ): Seq[OrganizationDomain] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and organization_domains.guid = {guid}::uuid" },
      organizationGuid.map { v => "and organization_domains.organization_guid = {organization_guid}::uuid" },
      domain.map { v => "and organization_domains.domain = lower(trim({domain}))" },
      isDeleted.map(Filters.isDeleted("organization_domains", _))
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _),
      organizationGuid.map('organization_guid -> _.toString),
      domain.map('domain -> _)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        OrganizationDomain(
          guid = row[String]("guid"),
          organization_guid = row[UUID]("organization_guid"),
          domain = row[String]("domain")
        )
      }.toSeq
    }
  }

}
