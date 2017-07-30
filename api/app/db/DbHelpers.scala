package db

import java.util.UUID

import io.flow.postgresql.Query
import play.api.db.Database

/**
  * Common utilities for a table given its name (e.g. helpers to delete records)
  */
case class DbHelpers(
  db: Database,
  tableName: String
) {

  private[this] val softDeleteQuery = Query(s"""
      update $tableName
         set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now()
       where guid = {guid)::uuid
         and deleted_at is null
  """).withDebugging()

  def delete(deletedBy: UUID, guid: String) {
    db.withConnection { implicit c =>
      delete(c, deletedBy, guid)
    }
  }

  def delete(
    implicit c: java.sql.Connection,
    deletedBy: UUID,
    guid: String
  ) {
    softDeleteQuery.
      bind("deleted_by_guid", deletedBy).
      bind("guid", guid).
      anormSql().execute()
  }

}
