package db

import io.apibuilder.api.v0.models.{Domain, Organization, User}
import io.flow.postgresql.Query
import lib.{Role, UrlKey}
import anorm._
import javax.inject.{Inject, Singleton}
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

case class OrganizationDomain(guid: UUID, organizationGuid: UUID, domain: Domain)

@Singleton
class OrganizationDomainsDao @Inject() () {

  private[this] val BaseQuery = Query("""
    select guid, organization_guid, domain
      from organization_domains
  """)

  private[this] val InsertQuery = """
    insert into organization_domains
    (guid, organization_guid, domain, created_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {domain}, {created_by_guid}::uuid)
  """  

  def create(createdBy: User, org: Organization, domainName: String): OrganizationDomain = {
    DB.withConnection { implicit c =>
      create(c, createdBy, org, domainName)
    }
  }

  private[db] def create(implicit c: java.sql.Connection, createdBy: User, org: Organization, domainName: String): OrganizationDomain = {
    val domain = OrganizationDomain(
      guid = UUID.randomUUID,
      organizationGuid = org.guid,
      domain = Domain(domainName.trim)
    )

    SQL(InsertQuery).on(
      'guid -> domain.guid,
      'organization_guid -> domain.organizationGuid,
      'domain -> domain.domain.name,
      'created_by_guid -> createdBy.guid
    ).execute()

    domain
  }

  def softDelete(deletedBy: User, domain: OrganizationDomain) {
    SoftDelete.delete("organization_domains", deletedBy, domain.guid)
  }

  def findAll(
    guid: Option[UUID] = None,
    organizationGuid: Option[UUID] = None,
    domain: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false)
  ): Seq[OrganizationDomain] = {
    DB.withConnection { implicit c =>
      BaseQuery.
        equals("organization_domains.guid", guid).
        equals("organization_domains.organization_guid", organizationGuid).
        and(
          domain.map { _ =>
            "organization_domains.domain = lower(trim({domain}))"
          }
        ).bind("domain", domain).
        and(isDeleted.map(Filters.isDeleted("organization_domains", _))).
        anormSql().as(
          parser().*
        )
    }
  }

  private[this] def parser(): RowParser[OrganizationDomain] = {
    SqlParser.get[UUID]("guid") ~
    SqlParser.get[UUID]("organization_guid") ~
    SqlParser.str("domain") map {
      case guid ~ organizationGuid ~ domain => {
        OrganizationDomain(
          guid = guid,
          organizationGuid = organizationGuid,
          domain = Domain(domain)
        )
      }
    }
  }
  
}
