package db

import core.{ Role, UrlKey }
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

case class OrganizationDomain(guid: String, organization_guid: String, domain: String)

object OrganizationDomainDao {

  private val BaseQuery = """
    select guid::varchar, organization_guid::varchar, domain
      from organization_domains
     where deleted_at is null
  """

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
    organizationGuid: Option[String] = None,
    domain: Option[String] = None
  ): Seq[OrganizationDomain] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and guid = {guid}::uuid" },
      organizationGuid.map { v => "and organization_guid = {organization_guid}::uuid" },
      domain.map { v => "and domain = lower(trim({domain}))" }
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _),
      organizationGuid.map('organization_guid -> _),
      domain.map('domain -> _)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        OrganizationDomain(
          guid = row[String]("guid"),
          organization_guid = row[String]("organization_guid"),
          domain = row[String]("domain")
        )
      }.toSeq
    }
  }

}
