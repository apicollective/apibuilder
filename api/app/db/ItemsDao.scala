package db

import lib.Text
import lib.query.{Query, QueryParser}
import io.apibuilder.api.v0.models._
import io.apibuilder.api.v0.models.json._
import anorm._
import javax.inject.{Inject, Singleton}

import play.api.db._
import play.api.libs.json._
import java.util.UUID

@Singleton
class ItemsDao @Inject() (
  @NamedDatabase("default") db: Database
) {

  private val BaseQuery = io.flow.postgresql.Query("""
    select guid, items.detail::text, label, description
      from search.items
  """).withDebugging()

  private val UpsertQuery = """
    insert into search.items
    (guid, organization_guid, application_guid, detail, label, description, content)
    values
    ({guid}::uuid, {organization_guid}::uuid, {application_guid}::uuid, {detail}::json, {label}, {description}, {content})
    on conflict(guid)
    do update
          set organization_guid = {organization_guid}::uuid,
              application_guid = {application_guid}::uuid,
              detail = {detail}::json,
              label = {label},
              description = {description},
              content = {content}
  """

  private val DeleteQuery = """
    delete from search.items where guid = {guid}::uuid
  """

  def upsert(
    guid: UUID,
    detail: ItemDetail,
    label: String,
    description: Option[String],
    content: String
  ): Unit = {
    val organizationGuid = detail match {
      case ApplicationSummary(_, org, _) => Some(org.guid)
      case ItemDetailUndefinedType(_) => sys.error(s"Could not determine organization guid from detail: $detail")
    }

    val applicationGuid = detail match {
      case ApplicationSummary(guid, org, key) => Some(guid)
      case ItemDetailUndefinedType(desc) => None
    }

    db.withTransaction { implicit c =>
      delete(c, guid)

      SQL(UpsertQuery).on(
        "guid" -> guid,
        "organization_guid" -> organizationGuid,
        "application_guid" -> applicationGuid,
        "detail" -> Json.toJson(detail).toString,
        "label" -> label.trim,
        "description" -> description.map(_.trim).map(Text.truncate(_)),
	      "content" -> content.trim.toLowerCase
      ).execute()
    }
  }

  def delete(guid: UUID): Unit = {
    db.withConnection { implicit c =>
      delete(c, guid)
    }
  }
  
  private def delete(implicit c: java.sql.Connection, guid: UUID): Unit = {
    SQL(DeleteQuery).on("guid" -> guid).execute()
  }

  def findByGuid(
    authorization: Authorization,
    guid: UUID
  ): Option[Item] = {
    findAll(authorization, guid = Some(guid)).headOption
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    q: Option[String] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Item] = {
    val (keywords, orgKey) = q.flatMap(QueryParser(_)) match {
      case None => (None, None)
      case Some(query) => parseQuery(query)
    }

    db.withConnection { implicit c =>
      authorization.applicationFilter(BaseQuery, "application_guid").
        equals("items.guid", guid).
        and(
          keywords.map { _ =>
            "items.content like '%' || lower(trim({keywords})) || '%' "
          }
        ).bind("keywords", keywords).
        and(
          orgKey.map { _ =>
            "items.organization_guid = (select guid from organizations where deleted_at is null and key = lower(trim({org_key})))"
          }
        ).bind("org_key", orgKey).
        limit(limit).
        offset(offset).
        orderBy("lower(items.label)").
        as(parser().*)
    }
  }

  private def parser(): RowParser[Item] = {
    SqlParser.get[UUID]("guid") ~
      SqlParser.str("detail") ~
      SqlParser.str("label") ~
      SqlParser.str("description").?  map {
      case guid ~ detail ~ label ~ description => {
        Item(
          guid = guid,
          detail = Json.parse(detail).as[ItemDetail],
          label = label,
          description = description
        )
      }
    }
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
