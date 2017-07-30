package db

import io.flow.postgresql.Query
import java.util.UUID
import play.api.db.Database

/**
  * Common utilities for a table given its name (e.g. helpers to delete records)
  */
case class DbHelpers(
  db: Database,
  tableName: String
) {

  private[this] val softDeleteQueryById = Query(s"""
      update $tableName
         set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now()
       where id = {id}
         and deleted_at is null
  """).withDebugging()

  private[this] val softDeleteQueryByGuid = Query(s"""
      update $tableName
         set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now()
       where guid = {guid}::uuid
         and deleted_at is null
  """).withDebugging()

  def delete(deletedBy: UUID, guid: UUID) {
    db.withConnection { implicit c =>
      delete(c, deletedBy, guid)
    }
  }

  def delete(
    implicit c: java.sql.Connection,
    deletedBy: UUID,
    guid: UUID
  ) {
    softDeleteQueryByGuid.
      bind("deleted_by_guid", deletedBy).
      bind("guid", guid).
      anormSql().execute()
  }

  def delete(deletedBy: UUID, id: String) {
    db.withConnection { implicit c =>
      delete(c, deletedBy, id)
    }
  }

  def delete(
    implicit c: java.sql.Connection,
    deletedBy: UUID,
    id: String
  ) {
    softDeleteQueryById.
      bind("deleted_by_guid", deletedBy).
      bind("id", id).
      anormSql().execute()
  }

}
