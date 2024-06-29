package db

import io.flow.postgresql.Query
import java.util.UUID
import io.apibuilder.api.v0.models.User
import play.api.db.Database

/**
  * Common utilities for a table given its name (e.g. helpers to delete records)
  */
case class DbHelpers(
  db: Database,
  tableName: String
) {

  private val softDeleteQueryById = Query(s"""
      update $tableName
         set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now()
       where id = {id}
         and deleted_at is null
  """)

  private val softDeleteQueryByGuid = Query(s"""
      update $tableName
         set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now()
       where guid = {guid}::uuid
         and deleted_at is null
  """)

  def delete(user: User, guid: UUID): Unit = {
    delete(user.guid, guid)
  }

  def delete(deletedBy: UUID, guid: UUID): Unit = {
    db.withConnection { implicit c =>
      delete(c, deletedBy, guid)
    }
  }

  def delete(
    implicit c: java.sql.Connection,
    deletedBy: UUID,
    guid: UUID
  ): Unit = {
    softDeleteQueryByGuid.
      bind("deleted_by_guid", deletedBy).
      bind("guid", guid).
      anormSql().execute()
  }

  def delete(user: User, id: String): Unit = {
    delete(user.guid, id)
  }

  def delete(deletedBy: UUID, id: String): Unit = {
    db.withConnection { implicit c =>
      delete(c, deletedBy, id)
    }
  }

  def delete(
    implicit c: java.sql.Connection,
    deletedBy: UUID,
    id: String
  ): Unit = {
    softDeleteQueryById.
      bind("deleted_by_guid", deletedBy).
      bind("id", id).
      anormSql().execute()
  }
}
