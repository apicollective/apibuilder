package db.generated

import anorm._
import io.flow.postgresql.{OrderBy, Query}
import io.flow.postgresql.play.db.DbHelpers
import java.sql.Connection
import java.util.UUID
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.db.{Database, NamedDatabase}

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
class SessionsDao @Inject() (
  @NamedDatabase("default") db: Database
) {

  private[this] val dbHelpers = DbHelpers(db, "sessions")

  private[this] val BaseQuery = Query("""
      | select sessions.id,
      |        sessions.user_guid,
      |        sessions.expires_at,
      |        sessions.created_at,
      |        sessions.updated_at,
      |        sessions.updated_by_user_id,
      |        sessions.hash_code
      |   from sessions
  """.stripMargin)

  private[this] val InsertQuery = Query("""
    | insert into sessions
    | (id, user_guid, expires_at, updated_by_user_id, hash_code)
    | values
    | ({id}, {user_guid}::uuid, {expires_at}::timestamptz, {updated_by_user_id}, {hash_code}::bigint)
  """.stripMargin)

  private[this] val UpdateQuery = Query("""
    | update sessions
    |    set user_guid = {user_guid}::uuid,
    |        expires_at = {expires_at}::timestamptz,
    |        updated_by_user_id = {updated_by_user_id},
    |        hash_code = {hash_code}::bigint
    |  where id = {id}
    |    and (sessions.hash_code is null or sessions.hash_code != {hash_code}::bigint)
  """.stripMargin)

  private[this] def bindQuery(query: Query, form: SessionForm): Query = {
    query.
      bind("user_guid", form.userGuid).
      bind("expires_at", form.expiresAt).
      bind("hash_code", form.hashCode())
  }

  def insert(updatedBy: UUID, form: SessionForm) {
    db.withConnection { implicit c =>
      insert(c, updatedBy, form)
    }
  }
  
  def insert(implicit c: Connection, updatedBy: UUID, form: SessionForm) {
    bindQuery(InsertQuery, form).
      bind("id", form.id).
      bind("updated_by_user_id", updatedBy).
      anormSql.execute()
  }

  def updateIfChangedById(updatedBy: UUID, id: String, form: SessionForm) {
    if (!findById(id).map(_.form).contains(form)) {
      updateById(updatedBy, id, form)
    }
  }
  
  def updateById(updatedBy: UUID, id: String, form: SessionForm) {
    db.withConnection { implicit c =>
      updateById(c, updatedBy, id, form)
    }
  }
  
  def updateById(implicit c: Connection, updatedBy: UUID, id: String, form: SessionForm) {
    bindQuery(UpdateQuery, form).
      bind("id", id).
      bind("updated_by_user_id", updatedBy).
      anormSql.execute()
  }

  def update(updatedBy: UUID, existing: Session, form: SessionForm) {
    db.withConnection { implicit c =>
      update(c, updatedBy, existing, form)
    }
  }
  
  def update(implicit c: Connection, updatedBy: UUID, existing: Session, form: SessionForm) {
    updateById(c, updatedBy, existing.id, form)
  }

  def delete(deletedBy: UUID, session: Session) {
    dbHelpers.delete(deletedBy, session.id)
  }
  
  def deleteById(deletedBy: UUID, id: String) {
    db.withConnection { implicit c =>
      deleteById(c, deletedBy, id)
    }
  }
  
  def deleteById(c: java.sql.Connection, deletedBy: UUID, id: String) {
    dbHelpers.delete(c, deletedBy, id)
  }

  def findById(id: String): Option[Session] = {
    findAll(ids = Some(Seq(id)), limit = 1).headOption
  }

  def findAll(
    ids: Option[Seq[String]] = None,
    userGuid: Option[UUID] = None,
    limit: Long,
    offset: Long = 0,
    orderBy: OrderBy = OrderBy("sessions.id")
  ) (
    implicit customQueryModifier: Query => Query = { q => q }
  ): Seq[Session] = {
    db.withConnection { implicit c =>
      customQueryModifier(BaseQuery).
        optionalIn("sessions.id", ids).
        equals("sessions.user_guid", userGuid).
        limit(limit).
        offset(offset).
        orderBy(orderBy.sql).
        as(SessionsDao.parser().*)
    }
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