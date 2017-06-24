package db

import io.apibuilder.apidoc.api.v0.models.{Organization, User}
import anorm._
import javax.inject.{Inject, Singleton}
import play.api.db._
import play.api.Play.current
import java.util.UUID

case class OrganizationLog(guid: String, organization_guid: UUID, message: String)

/**
 * Journal of changes to organizations
 */
@Singleton
class OrganizationLogsDao @Inject() () {

  private[this] val BaseQuery = """
    select guid::varchar, organization_guid, message
      from organization_logs
     where true
  """

  private[this] val InsertQuery = """
    insert into organization_logs
    (guid, organization_guid, message, created_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {message}, {created_by_guid}::uuid)
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
  ): Seq[OrganizationLog] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      authorization.organizationFilter().map(v => "and " + v),
      organization.map(v => "and organization_logs.organization_guid = {organization_guid}::uuid"),
      Some(s"order by organization_logs.created_at desc limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      organization.map('organization_guid -> _.guid)
    ).flatten ++ authorization.bindVariables

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        OrganizationLog(
          guid = row[String]("guid"),
          organization_guid = row[UUID]("organization_guid"),
          message = row[String]("message")
        )
      }.toSeq
    }
  }

}
