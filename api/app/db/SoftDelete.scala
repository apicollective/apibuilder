package db

import anorm._
import play.api.db._
import play.api.Play.current
import java.util.UUID

private[db] object SoftDelete {

  private val SoftDeleteQuery = """
    update %s set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now() where guid = {guid}::uuid and deleted_at is null
  """

  def delete(tableName: String, deletedBy: User, guid: String) {
    delete(tableName, deletedBy, UUID.fromString(guid))
  }

  def delete(tableName: String, deletedBy: User, guid: UUID) {
    DB.withConnection { implicit c =>
      delete(c, tableName, deletedBy, guid)
    }
  }

  private[db] def delete(implicit c: java.sql.Connection, tableName: String, deletedBy: User, guid: UUID) {
    SQL(SoftDeleteQuery.format(tableName)).on('deleted_by_guid -> deletedBy.guid, 'guid -> guid.toString).execute()
  }

}
