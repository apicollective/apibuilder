package db

import io.apibuilder.api.v0.models.Organization
import io.flow.postgresql.Query
import anorm._
import javax.inject.{Inject, Singleton}
import play.api.db._
import java.util.UUID

case class OrganizationLog(guid: UUID, organizationGuid: UUID, message: String)

/**
 * Journal of changes to organizations
 */
@Singleton
class OrganizationLogsDao @Inject() (
  @NamedDatabase("default") db: Database
) {

  private val BaseQuery = Query("""
    select guid::text, organization_guid, message
      from organization_logs
  """)

  private val InsertQuery = """
    insert into organization_logs
    (guid, organization_guid, message, created_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {message}, {created_by_guid}::uuid)
  """

  def create(createdBy: UUID, org: OrganizationReference, message: String): OrganizationLog = {
    db.withConnection { implicit c =>
      create(c, createdBy, org, message)
    }
  }

  private[db] def create(c: java.sql.Connection, createdBy: UUID, org: OrganizationReference, message: String): OrganizationLog = {
    val log = OrganizationLog(
      guid = UUID.randomUUID,
      organizationGuid = org.guid,
      message = message
    )

    SQL(InsertQuery).on(
      "guid" -> log.guid,
      "organization_guid" -> log.organizationGuid,
      "message" -> log.message,
      "created_by_guid" -> createdBy
    ).execute()(using c)

    log
  }

  def findAll(
    authorization: Authorization,
    organization: Option[OrganizationReference],
    limit: Long = 25,
    offset: Long = 0
  ): Seq[OrganizationLog] = {
    db.withConnection { implicit c =>
      authorization.organizationFilter(BaseQuery, "organization_guid").
        equals("organization_guid", organization.map(_.guid)).
        orderBy("created_at desc").
        limit(limit).
        offset(offset).
        anormSql().as(parser().*)
    }
  }

  private def parser(): RowParser[OrganizationLog] = {
    SqlParser.get[UUID]("guid") ~
      SqlParser.get[UUID]("organization_guid") ~
      SqlParser.str("message") map {
      case guid ~ organizationGuid ~ message => {
        OrganizationLog(
          guid = guid,
          organizationGuid = organizationGuid,
          message = message
        )
      }
    }
  }

}
