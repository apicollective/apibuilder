package db

import io.apibuilder.api.v0.models.{Original, User}
import anorm._
import javax.inject.{Inject, Singleton}
import play.api.db._
import play.api.libs.json.JsObject
import java.util.UUID

@Singleton
class OriginalsDao @Inject() () {

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
    user: User,
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
    user: User,
    guid: UUID
  ): Unit = {
    SQL(SoftDeleteByVersionGuidQuery).on(
      "version_guid" -> guid,
      "deleted_by_guid" -> user.guid
    ).execute()
  }
}
