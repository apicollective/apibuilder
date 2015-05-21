package db

import lib.Text
import com.gilt.apidoc.api.v0.models.{User, Item, ItemType}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object ItemsDao {

  private val BaseQuery = """
    select items.guid::varchar,
           items.type,
           items.label,
           items.description
      from search.items
     where true
  """

  private val InsertQuery = """
    insert into search.items
    (guid, organization_guid, type, label, description, content)
    values
    ({guid}::uuid, {organization_guid}::uuid, {type}, {label}, {description}, {content})
  """

  private val DeleteQuery = """
    delete from search.items where guid = {guid}::uuid
  """

  def upsert(
    guid: UUID,
    organizationGuid: UUID,
    `type`: ItemType,
    label: String,
    description: Option[String],
    content: String
  ) {
    DB.withTransaction { implicit c =>
      delete(c, guid)

      SQL(InsertQuery).on(
        'guid -> guid,
        'organization_guid -> organizationGuid,
        'type -> `type`.toString,
        'label -> label.trim,
        'description -> description.map(_.trim).map(Text.truncate(_)),
	'content -> content.trim.toLowerCase
      ).execute()
    }
  }

  def delete(guid: UUID) {
    DB.withConnection { implicit c =>
      delete(c, guid)
    }
  }
  
  private def delete(implicit c: java.sql.Connection, guid: UUID) {
    SQL(DeleteQuery).on('guid -> guid).execute()
  }

  def findByGuid(
    authorization: Authorization,
    guid: UUID
  ): Option[Item] = {
    findAll(authorization, guid = Some(guid)).headOption
  }

  def findAll(
    authorization: Authorization = Authorization.All, // TODO
    guid: Option[UUID] = None,
    orgKey: Option[String] = None,
    `type`: Option[ItemType] = None,
    q: Option[String] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Item] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and items.guid = {guid}::uuid" },
      orgKey.map { v => "and items.organization_guid = (select guid from organizations where deleted_at is null and key = lower(trim({org_key})))" },
      `type`.map { v => "and items.type = {type}" },
      q.map { v => "and items.content like '%' || lower(trim({q})) || '%' " },
      Some(s"order by lower(items.label) limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      orgKey.map('org_key -> _),
      `type`.map('type -> _.toString),
      q.map('q -> _)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ): Item = {
    Item(
      guid = row[UUID]("guid"),
      `type` = ItemType(row[String]("type")),
      label = row[String]("label"),
      description = row[Option[String]]("description")
    )
  }

}
