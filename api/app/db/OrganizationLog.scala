package db

import com.gilt.apidoc.models.{Organization, User}
import anorm._
import play.api.db._
import play.api.Play.current
import java.util.UUID

case class OrganizationLog(guid: String, organization_guid: UUID, message: String)

/**
 * Journal of changes to organizations
 */
object OrganizationLog {

  private val BaseQuery = """
    select guid::varchar, organization_guid, message
      from organization_logs
     where true
  """

  def create(createdBy: User, organization: Organization, message: String): OrganizationLog = {
    DB.withConnection { implicit c =>
      create(c, createdBy, organization, message)
    }
  }

  private[db] def create(implicit c: java.sql.Connection, createdBy: User, organization: Organization, message: String): OrganizationLog = {
    val log = OrganizationLog(
      guid = UUID.randomUUID.toString,
      organization_guid = organization.guid,
      message = message
    )

    SQL("""
      insert into organization_logs
      (guid, organization_guid, message, created_by_guid)
      values
      ({guid}::uuid, {organization_guid}::uuid, {message}, {created_by_guid}::uuid)
      """
    ).on(
      'guid -> log.guid,
      'organization_guid -> log.organization_guid,
      'message -> log.message,
      'created_by_guid -> createdBy.guid
    ).execute()

    log
  }

  def findAllForOrganization(org: Organization): Seq[OrganizationLog] = {
    findAll(organizationGuid = org.guid)
  }


  def findAll(organizationGuid: UUID,
              limit: Long = 25,
              offset: Long = 0): Seq[OrganizationLog] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      Some("and organization_guid = {organization_guid}::uuid"),
      Some(s"order by created_at desc limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[NamedParameter]('organization_guid -> organizationGuid.toString)

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        OrganizationLog(guid = row[String]("guid"),
                        organization_guid = row[UUID]("organization_guid"),
                        message = row[String]("message"))
      }.toSeq
    }
  }

}
