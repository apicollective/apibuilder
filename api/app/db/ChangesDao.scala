package db

import com.gilt.apidoc.api.v0.models.Application
import com.gilt.apidoc.internal.v0.models.{Change, ChangeVersion, Difference, DifferenceBreaking, DifferenceNonBreaking, DifferenceUndefinedType}
import com.gilt.apidoc.internal.v0.models.json._
import com.gilt.apidoc.api.v0.models.{User, Version}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object ChangesDao {

  private val BaseQuery = """
    select changes.guid,
           from_version.guid::varchar as from_guid,
           from_version.version as from_version,
           to_version.guid::varchar as to_guid,
           to_version.version as to_version,
           changes.type,
           changes.description
      from changes
      join versions from_version on versions.guid = changes.from_version_guid
      join versions to_version on versions.guid = changes.to_version_guid
     where true
  """

  private val InsertQuery = """
    insert into changes
    (guid, from_version_guid, to_version_guid, description, created_by_guid)
    values
    ({guid}::uuid, {to_version_guid}::uuid, {to_version_guid}::uuid, {description}, {created_by_guid}::uuid)
  """

  def upsert(createdBy: User, fromVersion: Version, toVersion: Version, differences: Seq[Difference]) {
    assert(
      fromVersion.application.guid == toVersion.application.guid,
      "Versions must belong to same application"
    )

    DB.withTransaction { implicit c =>

      differences.foreach { d =>
        val (differenceType, description) = d match {
          case DifferenceBreaking(desc) => ("breaking", desc)
          case DifferenceNonBreaking(desc) => ("non_breaking", desc)
          case DifferenceUndefinedType(desc) => {
            sys.error(s"Unrecognized difference type: $desc")
          }
        }

        // TODO: Handle unique constraint PSQLException
        SQL(InsertQuery).on(
          'guid -> UUID.randomUUID,
          'from_version_guid -> fromVersion.guid,
          'to_version_guid -> toVersion.guid,
          'type -> differenceType,
          'description -> description,
          'created_by_guid -> createdBy.guid
        ).execute()
      }
    }

  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[Change] = {
    findAll(authorization, guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    application: Option[Application] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Change] = {
    // TODO: Authorization on versions
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and changes.guid = {guid}::uuid" },
      application.map { v => "and changes.from_version_guid in (select guid from versions where deleted_at is null and application_guid = {application_guid}::uuid)" },
      Some(s"order by changes.number_attempts, changes.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      application.map('application_guid -> _.guid.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ): Change = {
    Change(
      guid = row[UUID]("guid"),
      fromVersion = ChangeVersion(row[UUID]("from_guid"), row[String]("from_version")),
      toVersion = ChangeVersion(row[UUID]("to_guid"), row[String]("to_version")),
      difference = row[String]("type") match {
        case "breaking" => DifferenceBreaking(row[String]("description"))
        case "non_breaking" => DifferenceNonBreaking(row[String]("description"))
        case other => DifferenceUndefinedType(other)
      }
    )
  }

}
