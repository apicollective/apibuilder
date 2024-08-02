package db

import io.apibuilder.api.v0.models.{Original, OriginalType, User}
import anorm.*
import db.generated.Version
import io.flow.postgresql.Query

import javax.inject.Inject
import play.api.db.*
import play.api.libs.json.JsObject

import java.util.UUID

case class InternalOriginal(id: BigInt, versionGuid: UUID, `type`: OriginalType, data: String)

class OriginalsDao @Inject() (db: Database) {

  private val InsertQuery = """
    insert into originals
    (version_guid, type, data, created_by_guid)
    values
    ({version_guid}::uuid, {type}, {data}, {created_by_guid}::uuid)
  """

  private val SoftDeleteByVersionGuidQuery = """
    update originals
       set deleted_at = now(),
           deleted_by_guid = {deleted_by_guid}::uuid
     where deleted_at is null
       and version_guid = {version_guid}::uuid
  """

  def create(
    implicit c: java.sql.Connection,
    user: InternalUser,
    versionGuid: UUID,
    original: Original
  ): Unit = {
    SQL(InsertQuery).on(
      "version_guid" -> versionGuid,
      "type" -> original.`type`.toString,
      "data" -> original.data,
      "created_by_guid" -> user.guid
    ).execute()
  }

  def softDeleteByVersionGuid(
    implicit c: java.sql.Connection,
    user: InternalUser,
    guid: UUID
  ): Unit = {
    SQL(SoftDeleteByVersionGuidQuery).on(
      "version_guid" -> guid,
      "deleted_by_guid" -> user.guid
    ).execute()
  }

  def findAllByVersionGuids(guids: Seq[UUID]): Seq[InternalOriginal] = {
    db.withConnection { c =>
      Query("select id, version_guid::text, type, data from originals")
        .in("version_guid", guids)
        .isNull("deleted_at")
        .as(parser.*)(c)
    }
  }

  def findByVersionGuid(guid: UUID): Option[InternalOriginal] = {
    findAllByVersionGuids(Seq(guid)).headOption
  }

  private val parser: anorm.RowParser[InternalOriginal] = {
    anorm.SqlParser.long("id") ~
      anorm.SqlParser.str("version_guid") ~
      anorm.SqlParser.str("type") ~
      anorm.SqlParser.str("data") map { case id ~ versionGuid ~ typ ~ data =>
      InternalOriginal(
        id = id,
        versionGuid = java.util.UUID.fromString(versionGuid),
        `type` = OriginalType(typ),
        data = data
      )
    }
  }
}
