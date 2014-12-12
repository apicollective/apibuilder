package db

import com.gilt.apidoc.models.{Organization, User}
import anorm._
import play.api.db._
import play.api.Play.current
import java.util.UUID

case class OrganizationLogsDao(guid: String, organization_guid: UUID, message: String)

/**
 * Journal of changes to organizations
 */
object OrganizationLogsDao {

  private val BaseQuery = """
    select guid::varchar, organization_guid, message
      from organization_logs
     where true
  """

  private val InsertQuery = """
    insert into organization_logs
    (guid, organization_guid, message, created_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {message}, {created_by_guid}::uuid)
  """

  def create(createdBy: User, organization: Organization, message: String): OrganizationLogsDao = {
    DB.withConnection { implicit c =>
      create(c, createdBy, organization, message)
    }
  }

  private[db] def create(implicit c: java.sql.Connection, createdBy: User, organization: Organization, message: String): OrganizationLogsDao = {
    val log = OrganizationLogsDao(
      guid = UUID.randomUUID.toString,
      organization_guid = organization.guid,
      message = message
    )

    SQL(InsertQuery).on(
      'guid -> log.guid,
      'organization_guid -> log.organization_guid,
      'message -> log.message,
      'created_by_guid -> createdBy.guid
    ).execute()

    log
  }

  def findAll(
    authorization: Authorization,
    organization: Option[Organization],
    limit: Long = 25,
    offset: Long = 0
  ): Seq[OrganizationLogsDao] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      authorization.organizationFilter("organization_guid").map(v => "and " + v),
      organization.map(v => "and organization_guid = {organization_guid}::uuid"),
      Some(s"order by created_at desc limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      organization.map('organization_guid -> _.guid)
    ).flatten ++ authorization.bindVariables

    println(sql)

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        OrganizationLogsDao(guid = row[String]("guid"),
                        organization_guid = row[UUID]("organization_guid"),
                        message = row[String]("message"))
      }.toSeq
    }
  }

}
