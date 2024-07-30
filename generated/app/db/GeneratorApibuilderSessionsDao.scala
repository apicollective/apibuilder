package db.generated

import anorm._
import io.flow.postgresql.{OrderBy, Query}
import java.sql.Connection
import java.util.UUID
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.db.{Database, NamedDatabase}

case class Session(
  id: String,
  userGuid: UUID,
  expiresAt: DateTime,
  createdAt: DateTime,
  createdByGuid: UUID,
  updatedAt: DateTime,
  updatedByGuid: UUID,
  deletedAt: Option[DateTime],
  deletedByGuid: Option[UUID]
) {

  lazy val form: SessionForm = SessionForm(
    id = id,
    userGuid = userGuid,
    expiresAt = expiresAt,
    createdAt = createdAt,
    createdByGuid = createdByGuid,
    updatedAt = updatedAt,
    updatedByGuid = updatedByGuid,
    deletedAt = deletedAt,
    deletedByGuid = deletedByGuid
  )

}

case class SessionForm(
  id: String,
  userGuid: UUID,
  expiresAt: DateTime,
  createdAt: DateTime,
  createdByGuid: UUID,
  updatedAt: DateTime,
  updatedByGuid: UUID,
  deletedAt: Option[DateTime],
  deletedByGuid: Option[UUID]
)

@Singleton
class SessionsDao @Inject() (
  @NamedDatabase("default") db: Database
) {

  private val BaseQuery = Query("""
      | select sessions.id,
      |        sessions.user_guid,
      |        sessions.expires_at,
      |        sessions.created_at,
      |        sessions.created_by_guid,
      |        sessions.updated_at,
      |        sessions.updated_by_guid,
      |        sessions.deleted_at,
      |        sessions.deleted_by_guid,
      |        sessions.hash_code
      |   from sessions
  """.stripMargin)

  private val InsertQuery = Query("""
    | insert into sessions
    | (id, user_guid, expires_at, created_at, created_by_guid, updated_at, updated_by_guid, deleted_at, deleted_by_guid, hash_code)
    | values
    | ({id}, {user_guid}::uuid, {expires_at}::timestamptz, {created_at}::timestamptz, {created_by_guid}::uuid, {updated_at}::timestamptz, {updated_by_guid}::uuid, {deleted_at}::timestamptz, {deleted_by_guid}::uuid, {hash_code}::bigint)
  """.stripMargin)

  private val UpdateQuery = Query("""
    | update sessions
    |    set user_guid = {user_guid}::uuid,
    |        expires_at = {expires_at}::timestamptz,
    |        created_at = {created_at}::timestamptz,
    |        created_by_guid = {created_by_guid}::uuid,
    |        updated_at = {updated_at}::timestamptz,
    |        updated_by_guid = {updated_by_guid}::uuid,
    |        deleted_at = {deleted_at}::timestamptz,
    |        deleted_by_guid = {deleted_by_guid}::uuid,
    |        hash_code = {hash_code}::bigint
    |  where id = {id}
    |    and (sessions.hash_code is null or sessions.hash_code != {hash_code}::bigint)
  """.stripMargin)

  private def bindQuery(query: Query, form: SessionForm): Query = {
    query.
      bind("user_guid", form.userGuid).
      bind("expires_at", form.expiresAt).
      bind("created_at", form.createdAt).
      bind("created_by_guid", form.createdByGuid).
      bind("updated_at", form.updatedAt).
      bind("updated_by_guid", form.updatedByGuid).
      bind("deleted_at", form.deletedAt).
      bind("deleted_by_guid", form.deletedByGuid).
      bind("hash_code", form.hashCode())
  }

  def insert(updatedBy: UUID, form: SessionForm): Unit = {
    db.withConnection { implicit c =>
      insert(c, updatedBy, form)
    }
  }

  def insert(implicit c: Connection, updatedBy: UUID, form: SessionForm): Unit = {
    bindQuery(InsertQuery, form).
      bind("id", form.id).
      anormSql().execute()
  }

  def updateIfChangedById(updatedBy: UUID, id: String, form: SessionForm): Unit ={
    if (!findById(id).map(_.form).contains(form)) {
      updateById(updatedBy, id, form)
    }
  }

  def updateById(updatedBy: UUID, id: String, form: SessionForm): Unit = {
    db.withConnection { implicit c =>
      updateById(c, updatedBy, id, form)
    }
  }

  def updateById(implicit c: Connection, updatedBy: UUID, id: String, form: SessionForm): Unit = {
    bindQuery(UpdateQuery, form).
      bind("id", id).
      anormSql().execute()
  }

  def update(updatedBy: UUID, existing: Session, form: SessionForm): Unit = {
    db.withConnection { implicit c =>
      update(c, updatedBy, existing, form)
    }
  }

  def update(implicit c: Connection, updatedBy: UUID, existing: Session, form: SessionForm): Unit = {
    updateById(c, updatedBy, existing.id, form)
  }

  def delete(deletedBy: UUID, session: Session): Unit = ???

  def deleteById(deletedBy: UUID, id: String): Unit = {
    db.withConnection { implicit c =>
      deleteById(c, deletedBy, id)
    }
  }

  def deleteById(c: java.sql.Connection, deletedBy: UUID, id: String): Unit = ???

  def findById(id: String): Option[Session] = {
    db.withConnection { implicit c =>
      findByIdWithConnection(c, id)
    }
  }

  def findByIdWithConnection(c: java.sql.Connection, id: String): Option[Session] = {
    findAllWithConnection(c, ids = Some(Seq(id)), limit = 1).headOption
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
      findAllWithConnection(
        c,
        ids = ids,
        userGuid = userGuid,
        limit = limit,
        offset = offset,
        orderBy = orderBy
      )(customQueryModifier)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    ids: Option[Seq[String]] = None,
    userGuid: Option[UUID] = None,
    limit: Long,
    offset: Long = 0,
    orderBy: OrderBy = OrderBy("sessions.id")
  ) (
    implicit customQueryModifier: Query => Query = { q => q }
  ): Seq[Session] = {
    customQueryModifier(BaseQuery).
      optionalIn("sessions.id", ids).
      equals("sessions.user_guid", userGuid).
      limit(limit).
      offset(offset).
      orderBy(orderBy.sql).
      as(SessionsDao.parser().*)(c)
  }

}

object SessionsDao {

  def parser(): RowParser[Session] = {
    SqlParser.str("id") ~
    SqlParser.get[UUID]("user_guid") ~
    SqlParser.get[DateTime]("expires_at") ~
    SqlParser.get[DateTime]("created_at") ~
    SqlParser.get[UUID]("created_by_guid") ~
    SqlParser.get[DateTime]("updated_at") ~
    SqlParser.get[UUID]("updated_by_guid") ~
    SqlParser.get[DateTime]("deleted_at").? ~
    SqlParser.get[UUID]("deleted_by_guid").? map {
      case id ~ userGuid ~ expiresAt ~ createdAt ~ createdByGuid ~ updatedAt ~ updatedByGuid ~ deletedAt ~ deletedByGuid => Session(
        id = id,
        userGuid = userGuid,
        expiresAt = expiresAt,
        createdAt = createdAt,
        createdByGuid = createdByGuid,
        updatedAt = updatedAt,
        updatedByGuid = updatedByGuid,
        deletedAt = deletedAt,
        deletedByGuid = deletedByGuid
      )
    }
  }

}