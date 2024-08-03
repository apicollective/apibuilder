package db.generated

case class Session(
  id: String,
  userGuid: java.util.UUID,
  expiresAt: org.joda.time.DateTime,
  createdAt: org.joda.time.DateTime,
  createdByGuid: java.util.UUID,
  updatedAt: org.joda.time.DateTime,
  updatedByGuid: java.util.UUID,
  deletedAt: Option[org.joda.time.DateTime],
  deletedByGuid: Option[java.util.UUID]
) {
  def form: SessionForm = {
    SessionForm(
      id = id,
      userGuid = userGuid,
      expiresAt = expiresAt,
    )
  }
}

case class SessionForm(
  id: String,
  userGuid: java.util.UUID,
  expiresAt: org.joda.time.DateTime
)

case object SessionsTable {
  val SchemaName: String = "public"

  val TableName: String = "sessions"

  val QualifiedName: String = "public.sessions"

  sealed trait Column {
    def name: String
  }

  object Columns {
    case object Id extends Column {
      override val name: String = "id"
    }

    case object UserGuid extends Column {
      override val name: String = "user_guid"
    }

    case object ExpiresAt extends Column {
      override val name: String = "expires_at"
    }

    case object CreatedAt extends Column {
      override val name: String = "created_at"
    }

    case object CreatedByGuid extends Column {
      override val name: String = "created_by_guid"
    }

    case object UpdatedAt extends Column {
      override val name: String = "updated_at"
    }

    case object UpdatedByGuid extends Column {
      override val name: String = "updated_by_guid"
    }

    case object DeletedAt extends Column {
      override val name: String = "deleted_at"
    }

    case object DeletedByGuid extends Column {
      override val name: String = "deleted_by_guid"
    }

    val all: List[Column] = List(Id, UserGuid, ExpiresAt, CreatedAt, CreatedByGuid, UpdatedAt, UpdatedByGuid, DeletedAt, DeletedByGuid)
  }
}

trait BaseSessionsDao {
  import anorm.*

  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def db: play.api.db.Database

  private val BaseQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | select id,
     |        user_guid::text,
     |        expires_at,
     |        created_at,
     |        created_by_guid::text,
     |        updated_at,
     |        updated_by_guid::text,
     |        deleted_at,
     |        deleted_by_guid::text
     |   from public.sessions
     |""".stripMargin.stripTrailing
    )
  }

  def findAll(
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    userGuid: Option[java.util.UUID] = None,
    userGuids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[Session] = {
    db.withConnection { c =>
      findAllWithConnection(c, id, ids, userGuid, userGuids, limit, offset, orderBy)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    userGuid: Option[java.util.UUID] = None,
    userGuids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[Session] = {
    customQueryModifier(BaseQuery)
      .equals("sessions.id", id)
      .optionalIn("sessions.id", ids)
      .equals("sessions.user_guid", userGuid)
      .optionalIn("sessions.user_guid", userGuids)
      .optionalLimit(limit)
      .offset(offset)
      .orderBy(orderBy.flatMap(_.sql))
      .as(parser.*)(c)
  }

  def iterateAll(
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    userGuid: Option[java.util.UUID] = None,
    userGuids: Option[Seq[java.util.UUID]] = None,
    pageSize: Long = 1000
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Iterator[Session] = {
    assert(pageSize > 0, "pageSize must be > 0")

    def iterate(lastValue: Option[Session]): Iterator[Session] = {
      val page: Seq[Session] = db.withConnection { c =>
        customQueryModifier(BaseQuery)
          .equals("sessions.id", id)
          .optionalIn("sessions.id", ids)
          .equals("sessions.user_guid", userGuid)
          .optionalIn("sessions.user_guid", userGuids)
          .greaterThan("sessions.id", lastValue.map(_.id))
          .orderBy("sessions.id")
          .limit(pageSize)
          .as(parser.*)(c)
      }
      if (page.length >= pageSize) {
        page.iterator ++ iterate(page.lastOption)
      } else {
        page.iterator
      }
    }

    iterate(None)
  }

  def findById(id: String): Option[Session] = {
    db.withConnection { c =>
      findByIdWithConnection(c, id)
    }
  }

  def findByIdWithConnection(
    c: java.sql.Connection,
    id: String
  ): Option[Session] = {
    findAllWithConnection(
      c = c,
      id = Some(id),
      limit = Some(1)
    ).headOption
  }

  def findAllByUserGuid(userGuid: java.util.UUID): Seq[Session] = {
    db.withConnection { c =>
      findAllByUserGuidWithConnection(c, userGuid)
    }
  }

  def findAllByUserGuidWithConnection(
    c: java.sql.Connection,
    userGuid: java.util.UUID
  ): Seq[Session] = {
    findAllWithConnection(
      c = c,
      userGuid = Some(userGuid),
      limit = None
    )
  }

  private val parser: anorm.RowParser[Session] = {
    anorm.SqlParser.str("id") ~
      anorm.SqlParser.str("user_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("expires_at") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("created_at") ~
      anorm.SqlParser.str("created_by_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("updated_at") ~
      anorm.SqlParser.str("updated_by_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("deleted_at").? ~
      anorm.SqlParser.str("deleted_by_guid").? map { case id ~ userGuid ~ expiresAt ~ createdAt ~ createdByGuid ~ updatedAt ~ updatedByGuid ~ deletedAt ~ deletedByGuid =>
      Session(
        id = id,
        userGuid = java.util.UUID.fromString(userGuid),
        expiresAt = expiresAt,
        createdAt = createdAt,
        createdByGuid = java.util.UUID.fromString(createdByGuid),
        updatedAt = updatedAt,
        updatedByGuid = java.util.UUID.fromString(updatedByGuid),
        deletedAt = deletedAt,
        deletedByGuid = deletedByGuid.map { v => java.util.UUID.fromString(v) }
      )
    }
  }
}

class SessionsDao @javax.inject.Inject() (override val db: play.api.db.Database) extends BaseSessionsDao {
  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  private val InsertQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | insert into public.sessions
     | (id, user_guid, expires_at, created_at, created_by_guid, updated_at, updated_by_guid)
     | values
     | ({id}, {user_guid}::uuid, {expires_at}::timestamptz, {created_at}::timestamptz, {created_by_guid}::uuid, {updated_at}::timestamptz, {updated_by_guid}::uuid)
    """.stripMargin)
  }

  private val UpdateQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | update public.sessions
     | set user_guid = {user_guid}::uuid,
     |     expires_at = {expires_at}::timestamptz,
     |     updated_at = {updated_at}::timestamptz,
     |     updated_by_guid = {updated_by_guid}::uuid
     | where id = {id}
    """.stripMargin)
  }

  private val DeleteQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("update public.sessions set deleted_at = {deleted_at}::timestamptz, deleted_by_guid = {deleted_by_guid}::uuid").isNull("deleted_at")
  }

  def insert(
    user: java.util.UUID,
    form: SessionForm
  ): Unit = {
    db.withConnection { c =>
      insert(c, user, form)
    }
  }

  def insert(
    c: java.sql.Connection,
    user: java.util.UUID,
    form: SessionForm
  ): Unit = {
    bindQuery(InsertQuery, user, form)
      .bind("created_at", org.joda.time.DateTime.now)
      .bind("created_by_guid", user)
      .execute(c)
  }

  def insertBatch(
    user: java.util.UUID,
    forms: Seq[SessionForm]
  ): Seq[Unit] = {
    db.withConnection { c =>
      insertBatch(c, user, forms)
    }
  }

  def insertBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[SessionForm]
  ): Seq[Unit] = {
    forms.map { f => Seq(anorm.NamedParameter("created_at", org.joda.time.DateTime.now)) ++ toNamedParameter(user, f) }.toList match {
      case Nil => Nil
      case one :: rest => {
        anorm.BatchSql(InsertQuery.sql(), one, rest*).execute()(c)
        (Seq(one) ++ rest).map { _ => () }
      }
    }
  }

  def update(
    user: java.util.UUID,
    session: Session,
    form: SessionForm
  ): Unit = {
    db.withConnection { c =>
      update(c, user, session, form)
    }
  }

  def update(
    c: java.sql.Connection,
    user: java.util.UUID,
    session: Session,
    form: SessionForm
  ): Unit = {
    updateById(
      c = c,
      user = user,
      id = session.id,
      form = form
    )
  }

  def updateById(
    user: java.util.UUID,
    id: String,
    form: SessionForm
  ): Unit = {
    db.withConnection { c =>
      updateById(c, user, id, form)
    }
  }

  def updateById(
    c: java.sql.Connection,
    user: java.util.UUID,
    id: String,
    form: SessionForm
  ): Unit = {
    bindQuery(UpdateQuery, user, form)
      .bind("id", id)
      .bind("updated_by_guid", user)
      .execute(c)
    ()
  }

  def updateBatch(
    user: java.util.UUID,
    forms: Seq[SessionForm]
  ): Unit = {
    db.withConnection { c =>
      updateBatch(c, user, forms)
    }
  }

  def updateBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[SessionForm]
  ): Unit = {
    forms.map { f => toNamedParameter(user, f) }.toList match {
      case Nil => // no-op
      case first :: rest => anorm.BatchSql(UpdateQuery.sql(), first, rest*).execute()(c)
    }
  }

  def delete(
    user: java.util.UUID,
    session: Session
  ): Unit = {
    db.withConnection { c =>
      delete(c, user, session)
    }
  }

  def delete(
    c: java.sql.Connection,
    user: java.util.UUID,
    session: Session
  ): Unit = {
    deleteById(
      c = c,
      user = user,
      id = session.id
    )
  }

  def deleteById(
    user: java.util.UUID,
    id: String
  ): Unit = {
    db.withConnection { c =>
      deleteById(c, user, id)
    }
  }

  def deleteById(
    c: java.sql.Connection,
    user: java.util.UUID,
    id: String
  ): Unit = {
    DeleteQuery.equals("id", id)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByIds(
    user: java.util.UUID,
    ids: Seq[String]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByIds(c, user, ids)
    }
  }

  def deleteAllByIds(
    c: java.sql.Connection,
    user: java.util.UUID,
    ids: Seq[String]
  ): Unit = {
    DeleteQuery.in("id", ids)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByUserGuid(
    user: java.util.UUID,
    userGuid: java.util.UUID
  ): Unit = {
    db.withConnection { c =>
      deleteAllByUserGuid(c, user, userGuid)
    }
  }

  def deleteAllByUserGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    userGuid: java.util.UUID
  ): Unit = {
    DeleteQuery.equals("user_guid", userGuid)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByUserGuids(
    user: java.util.UUID,
    userGuids: Seq[java.util.UUID]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByUserGuids(c, user, userGuids)
    }
  }

  def deleteAllByUserGuids(
    c: java.sql.Connection,
    user: java.util.UUID,
    userGuids: Seq[java.util.UUID]
  ): Unit = {
    DeleteQuery.in("user_guid", userGuids)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  private def bindQuery(
    query: io.flow.postgresql.Query,
    user: java.util.UUID,
    form: SessionForm
  ): io.flow.postgresql.Query = {
    query
      .bind("id", form.id)
      .bind("user_guid", form.userGuid.toString)
      .bind("expires_at", form.expiresAt)
      .bind("updated_at", org.joda.time.DateTime.now)
      .bind("updated_by_guid", user)
  }

  private def toNamedParameter(
    user: java.util.UUID,
    form: SessionForm
  ): Seq[anorm.NamedParameter] = {
    Seq(
      anorm.NamedParameter("id", form.id),
      anorm.NamedParameter("user_guid", form.userGuid.toString),
      anorm.NamedParameter("expires_at", form.expiresAt),
      anorm.NamedParameter("updated_at", org.joda.time.DateTime.now),
      anorm.NamedParameter("updated_by_guid", user.toString)
    )
  }
}