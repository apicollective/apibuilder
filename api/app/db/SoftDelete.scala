package db

import com.gilt.apidoc.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import java.util.UUID

private[db] object SoftDelete {

  def delete(tableName: String, deletedBy: User, guid: String) {
    delete(tableName, deletedBy, UUID.fromString(guid))
  }

  def delete(tableName: String, deletedBy: User, guid: UUID): Unit = {
    delete(tableName, deletedBy, ("guid", Some("::uuid"), guid.toString))
  }

  def delete(tableName: String, deletedBy: User, field: (String, Option[String], String)) {
    val (name, tpe, value) = field
    val SoftDeleteQuery = s"""
      update %s set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now() where ${name} = {${name}}${tpe.getOrElse("")} and deleted_at is null
                          """
    DB.withConnection { implicit c =>
      SQL(SoftDeleteQuery.format(tableName)).on('deleted_by_guid -> deletedBy.guid, name -> value).execute()
    }
  }

}
