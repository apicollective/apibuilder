package db

import core.{ Organization, User }
import lib.Constants
import anorm._
import play.api.db._
import play.api.Play.current
import java.util.UUID

case class OrganizationLog(guid: String, organization_guid: String, message: String)

case class OrganizationLogQuery(organization_guid: String,
                                limit: Int = 50,
                                offset: Int = 0)

/**
 * Journal of changes to organization memberships
 */
object OrganizationLog {

  private val BaseQuery = """
    select guid::varchar, organization_guid::varchar, message
      from membership_logs
     where true
  """

  def create(createdBy: User, organization: Organization, message: String): OrganizationLog = {
    val log = OrganizationLog(guid = UUID.randomUUID.toString,
                              organization_guid = organization.guid,
                              message = message)

    DB.withConnection { implicit c =>
      SQL("""
          insert into membership_logs
          (guid, organization_guid, message, created_by_guid)
          values
          ({guid}::uuid, {organization_guid}::uuid, {message}, {created_by_guid}::uuid)
          """).on('guid -> log.guid,
                  'organization_guid -> log.organization_guid,
                  'message -> log.message,
                  'created_by_guid -> createdBy.guid).execute()
    }

    log
  }

  def findAllForOrganization(org: Organization): Seq[OrganizationLog] = {
    findAll(OrganizationLogQuery(organization_guid = org.guid))
  }

  def findAll(query: OrganizationLogQuery): Seq[OrganizationLog] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      Some("and organization_guid = {organization_guid}::uuid"),
      Some(s"order by created_at desc limit ${query.limit} offset ${query.offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq(
      Some('organization_guid -> toParameterValue(query.organization_guid))
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        OrganizationLog(guid = row[String]("guid"),
                        organization_guid = row[String]("organization_guid"),
                        message = row[String]("message"))
      }.toSeq
    }
  }

}
