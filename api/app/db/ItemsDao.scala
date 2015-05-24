package db

import lib.Text
import lib.query.{Query, QueryParser}
import com.gilt.apidoc.api.v0.models.{ApplicationSummary, Item, ItemDetail, User}
import com.gilt.apidoc.api.v0.models.json._
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object ItemsDao {

  private val BaseQuery = """
    select items.guid::varchar,
           items.detail::varchar,
           items.label,
           items.description
      from search.items
     where true
  """

  private val InsertQuery = """
    insert into search.items
    (guid, organization_guid, detail, label, description, content)
    values
    ({guid}::uuid, {organization_guid}::uuid, {detail}::json, {label}, {description}, {content})
  """

  private val DeleteQuery = """
    delete from search.items where guid = {guid}::uuid
  """

  def upsert(
    guid: UUID,
    organizationGuid: UUID,
    detail: ItemDetail,
    label: String,
    description: Option[String],
    content: String
  ) {
    DB.withTransaction { implicit c =>
      delete(c, guid)

      SQL(InsertQuery).on(
        'guid -> guid,
        'organization_guid -> organizationGuid,
        'detail -> Json.toJson(detail).toString,
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
    q: Option[String] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Item] = {
    val (keywords, orgKey) = q.flatMap(QueryParser(_)) match {
      case None => (None, None)
      case Some(query) => parseQuery(query)
    }

    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and items.guid = {guid}::uuid" },
      keywords.map { v => "and items.content like '%' || lower(trim({keywords})) || '%' " },
      orgKey.map { v => "and items.organization_guid = (select guid from organizations where deleted_at is null and key = lower(trim({org_key})))" },
      Some(s"order by lower(items.label) limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      keywords.map('keywords -> _),
      orgKey.map('org_key -> _)
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
      detail = Json.parse(row[String]("detail")).as[ItemDetail],
      label = row[String]("label"),
      description = row[Option[String]]("description")
    )
  }

  private def parseQuery(query: Query): (Option[String], Option[String]) = {
    val keywords = query.words match {
      case Nil => None
      case words => Some(words.mkString(" "))
    }
    val orgKey = query.orgKeys match {
      case Nil => None
      case multiple => {
        // TODO: Decide if we want to support this use case of
        // specifying multiple org keys. For now we only use the first
        // org key.
        multiple.headOption
      }
    }
    (keywords, orgKey)
  }

}
