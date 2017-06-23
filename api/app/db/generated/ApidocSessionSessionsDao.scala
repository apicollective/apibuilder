package db.generated

import anorm._
import util.Query
import java.sql.Connection
import java.util.UUID
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.db.{Database, DB, NamedDatabase}
import play.api.Play.current

case class Session(
  id: String,
  userGuid: UUID,
  expiresAt: DateTime
) {

  lazy val form: SessionForm = SessionForm(
    id = id,
    userGuid = userGuid,
    expiresAt = expiresAt
  )

}

case class SessionForm(
  id: String,
  userGuid: UUID,
  expiresAt: DateTime
)

@Singleton
class SessionsDao @Inject() () {

  private[this] val BaseQuery = Query("""
      | select sessions.id,
      |        sessions.user_guid,
      |        sessions.expires_at,
      |        sessions.created_at,
      |        sessions.created_by_guid,
      |        sessions.updated_at,
      |        sessions.updated_by_guid
      |   from sessions
  """.stripMargin)

  private[this] val InsertQuery = Query("""
    | insert into sessions
    | (id, user_guid, expires_at, created_by_guid, updated_by_guid)
    | values
    | ({id}, {user_guid}::uuid, {expires_at}::timestamptz, {created_by_guid}::uuid, {updated_by_guid}::uuid)
  """.stripMargin)

  private[this] val UpdateQuery = Query("""
    | update sessions
    |    set user_guid = {user_guid}::uuid,
    |        expires_at = {expires_at}::timestamptz,
    |        updated_by_guid = {updated_by_guid}::uuid
    |  where id = {id}
  """.stripMargin)

  private[this] def bindQuery(query: Query, form: SessionForm): Query = {
    query.
      bind("user_guid", form.userGuid).
      bind("expires_at", form.expiresAt)
  }

  def insert(updatedBy: UUID, form: SessionForm) {
    DB.withConnection { implicit c =>
      insert(c, updatedBy, form)
    }
  }
  
  def insert(implicit c: Connection, updatedBy: UUID, form: SessionForm) {
    bindQuery(InsertQuery, form).
      bind("id", form.id).
      bind("created_by_guid", updatedBy).
      bind("updated_by_guid", updatedBy).
      anormSql.execute()
  }

  def updateIfChangedById(updatedBy: UUID, id: String, form: SessionForm) {
    if (!findById(id).map(_.form).contains(form)) {
      updateById(updatedBy, id, form)
    }
  }
  
  def updateById(updatedBy: UUID, id: String, form: SessionForm) {
    DB.withConnection { implicit c =>
      updateById(c, updatedBy, id, form)
    }
  }
  
  def updateById(implicit c: Connection, updatedBy: UUID, id: String, form: SessionForm) {
    bindQuery(UpdateQuery, form).
      bind("id", id).
      bind("updated_by_guid", updatedBy).
      anormSql.execute()
  }

  def update(updatedBy: UUID, existing: Session, form: SessionForm) {
    DB.withConnection { implicit c =>
      update(c, updatedBy, existing, form)
    }
  }
  
  def update(implicit c: Connection, updatedBy: UUID, existing: Session, form: SessionForm) {
    updateById(c, updatedBy, existing.id, form)
  }

  def delete(deletedBy: UUID, session: Session) {
    delete(deletedBy, session.id)
  }
  
  def deleteById(deletedBy: UUID, id: String) {
    DB.withConnection { implicit c =>
      deleteById(c, deletedBy, id)
    }
  }
  
  def deleteById(c: java.sql.Connection, deletedBy: UUID, id: String) {
    delete(c, deletedBy, id)
  }

  def findById(id: String): Option[Session] = {
    findAll(ids = Some(Seq(id)), limit = 1).headOption
  }

  def findAll(
    ids: Option[Seq[String]] = None,
    userGuid: Option[UUID] = None,
    limit: Long,
    offset: Long = 0,
    orderBy: String = "sessions.id"
  ) (
    implicit customQueryModifier: Query => Query = { q => q }
  ): Seq[Session] = {
    DB.withConnection { implicit c =>
      customQueryModifier(BaseQuery).
        optionalIn("sessions.id", ids).
        equals("sessions.user_guid", userGuid).
        limit(limit).
        offset(offset).
        orderBy(orderBy).
        as(SessionsDao.parser().*)
    }
  }

  private[this] val DeleteQuery = s"""
    delete from sessions where id = {id}
  """

  def delete(deletedBy: UUID, id: String) {
    DB.withConnection { implicit c =>
      delete(c, deletedBy, id)
    }
  }

  def delete(
    implicit c: java.sql.Connection,
    deletedBy: UUID,
    id: String
  ) {
    SQL(DeleteQuery).on(
      'id -> id
    ).execute()
  }
  
}

object SessionsDao {

  def parser(): RowParser[Session] = {
    SqlParser.str("id") ~
    SqlParser.get[UUID]("user_guid") ~
    SqlParser.get[DateTime]("expires_at") map {
      case id ~ userGuid ~ expiresAt => Session(
        id = id,
        userGuid = userGuid,
        expiresAt = expiresAt
      )
    }
  }

}
