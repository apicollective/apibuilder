package db

import lib.Constants
import anorm._
import play.api.db._
import play.api.Play.current
import java.util.UUID

case class MembershipLog(guid: UUID, organization_guid: UUID, message: String)

/**
 * Journal of changes to organization memberships
 */
object MembershipLog {

  private val BaseQuery = """
    select guid::varchar, organization_guid::varchar, message
      from membership_logs
     where true
  """

  def create(createdBy: User, organization: Organization, message: String): MembershipLog = {
    val guid = UUID.randomUUID
    DB.withConnection { implicit c =>
      SQL("""
          insert into membership_logs
          (guid, organization_guid, message, created_by_guid)
          values
          ({guid}::uuid, {organization_guid}::uuid, {message}, {created_by_guid}::uuid)
          """).on('guid -> guid,
                  'organization_guid -> organization.guid,
                  'message -> message,
                  'created_by_guid -> createdBy.guid).execute()
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create membership_log")
    }
  }

  private def findByGuid(guid: UUID): Option[MembershipLog] = {
    DB.withConnection { implicit c =>
      SQL(BaseQuery + " and guid = {guid}::uuid").on('guid -> guid)().map { r => mapRow(r) }.toSeq.headOption
    }
  }

  def findAllForOrganization(org: Organization): Seq[MembershipLog] = {
    DB.withConnection { implicit c =>
      SQL(BaseQuery + " and organization_guid = {organization_guid}::uuid").on('organization_guid -> org.guid)().map { r => mapRow(r) }.toSeq
    }
  }

  private def mapRow(row: anorm.SqlRow): MembershipLog = {
    MembershipLog(guid = UUID.fromString(row[String]("guid")),
                              organization_guid = UUID.fromString(row[String]("organization_guid")),
                              message = row[String]("message"))
  }

}
