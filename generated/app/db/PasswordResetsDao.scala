package db.generated

case class PasswordReset(
  guid: java.util.UUID,
  userGuid: java.util.UUID,
  token: String,
  expiresAt: org.joda.time.DateTime,
  createdAt: org.joda.time.DateTime,
  createdByGuid: java.util.UUID,
  updatedAt: org.joda.time.DateTime,
  deletedAt: Option[org.joda.time.DateTime],
  deletedByGuid: Option[java.util.UUID]
) {
  def form: PasswordResetForm = {
    PasswordResetForm(
      userGuid = userGuid,
      token = token,
      expiresAt = expiresAt,
    )
  }
}

case class PasswordResetForm(
  userGuid: java.util.UUID,
  token: String,
  expiresAt: org.joda.time.DateTime
)

case object PasswordResetsTable {
  val SchemaName: String = "public"

  val TableName: String = "password_resets"

  val QualifiedName: String = "public.password_resets"

  sealed trait Column {
    def name: String
  }

  object Columns {
    case object Guid extends Column {
      override val name: String = "guid"
    }

    case object UserGuid extends Column {
      override val name: String = "user_guid"
    }

    case object Token extends Column {
      override val name: String = "token"
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

    case object DeletedAt extends Column {
      override val name: String = "deleted_at"
    }

    case object DeletedByGuid extends Column {
      override val name: String = "deleted_by_guid"
    }

    val all: List[Column] = List(Guid, UserGuid, Token, ExpiresAt, CreatedAt, CreatedByGuid, UpdatedAt, DeletedAt, DeletedByGuid)
  }
}

trait BasePasswordResetsDao {
  import anorm.*

  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def db: play.api.db.Database

  private val BaseQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | select guid::text,
     |        user_guid::text,
     |        token,
     |        expires_at,
     |        created_at,
     |        created_by_guid::text,
     |        updated_at,
     |        deleted_at,
     |        deleted_by_guid::text
     |   from public.password_resets
     |""".stripMargin.stripTrailing
    )
  }

  def findAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    token: Option[String] = None,
    tokens: Option[Seq[String]] = None,
    userGuid: Option[java.util.UUID] = None,
    userGuids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[PasswordReset] = {
    db.withConnection { c =>
      findAllWithConnection(c, guid, guids, token, tokens, userGuid, userGuids, limit, offset, orderBy)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    token: Option[String] = None,
    tokens: Option[Seq[String]] = None,
    userGuid: Option[java.util.UUID] = None,
    userGuids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[PasswordReset] = {
    customQueryModifier(BaseQuery)
      .equals("password_resets.guid", guid)
      .optionalIn("password_resets.guid", guids)
      .equals("password_resets.token", token)
      .optionalIn("password_resets.token", tokens)
      .equals("password_resets.user_guid", userGuid)
      .optionalIn("password_resets.user_guid", userGuids)
      .optionalLimit(limit)
      .offset(offset)
      .orderBy(orderBy.flatMap(_.sql))
      .as(parser.*)(c)
  }

  def iterateAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    token: Option[String] = None,
    tokens: Option[Seq[String]] = None,
    userGuid: Option[java.util.UUID] = None,
    userGuids: Option[Seq[java.util.UUID]] = None,
    pageSize: Long = 1000
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Iterator[PasswordReset] = {
    assert(pageSize > 0, "pageSize must be > 0")

    def iterate(lastValue: Option[PasswordReset]): Iterator[PasswordReset] = {
      val page: Seq[PasswordReset] = db.withConnection { c =>
        customQueryModifier(BaseQuery)
          .equals("password_resets.guid", guid)
          .optionalIn("password_resets.guid", guids)
          .equals("password_resets.token", token)
          .optionalIn("password_resets.token", tokens)
          .equals("password_resets.user_guid", userGuid)
          .optionalIn("password_resets.user_guid", userGuids)
          .greaterThan("password_resets.guid", lastValue.map(_.guid))
          .orderBy("password_resets.guid")
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

  def findByGuid(guid: java.util.UUID): Option[PasswordReset] = {
    db.withConnection { c =>
      findByGuidWithConnection(c, guid)
    }
  }

  def findByGuidWithConnection(
    c: java.sql.Connection,
    guid: java.util.UUID
  ): Option[PasswordReset] = {
    findAllWithConnection(
      c = c,
      guid = Some(guid),
      limit = Some(1)
    ).headOption
  }

  def findByToken(token: String): Option[PasswordReset] = {
    db.withConnection { c =>
      findByTokenWithConnection(c, token)
    }
  }

  def findByTokenWithConnection(
    c: java.sql.Connection,
    token: String
  ): Option[PasswordReset] = {
    findAllWithConnection(
      c = c,
      token = Some(token),
      limit = Some(1)
    ).headOption
  }

  def findAllByUserGuid(userGuid: java.util.UUID): Seq[PasswordReset] = {
    db.withConnection { c =>
      findAllByUserGuidWithConnection(c, userGuid)
    }
  }

  def findAllByUserGuidWithConnection(
    c: java.sql.Connection,
    userGuid: java.util.UUID
  ): Seq[PasswordReset] = {
    findAllWithConnection(
      c = c,
      userGuid = Some(userGuid),
      limit = None
    )
  }

  private val parser: anorm.RowParser[PasswordReset] = {
    anorm.SqlParser.str("guid") ~
      anorm.SqlParser.str("user_guid") ~
      anorm.SqlParser.str("token") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("expires_at") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("created_at") ~
      anorm.SqlParser.str("created_by_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("updated_at") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("deleted_at").? ~
      anorm.SqlParser.str("deleted_by_guid").? map { case guid ~ userGuid ~ token ~ expiresAt ~ createdAt ~ createdByGuid ~ updatedAt ~ deletedAt ~ deletedByGuid =>
      PasswordReset(
        guid = java.util.UUID.fromString(guid),
        userGuid = java.util.UUID.fromString(userGuid),
        token = token,
        expiresAt = expiresAt,
        createdAt = createdAt,
        createdByGuid = java.util.UUID.fromString(createdByGuid),
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        deletedByGuid = deletedByGuid.map { v => java.util.UUID.fromString(v) }
      )
    }
  }
}

class PasswordResetsDao @javax.inject.Inject() (override val db: play.api.db.Database) extends BasePasswordResetsDao {
  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def randomPkey: java.util.UUID = {
    java.util.UUID.randomUUID
  }

  private val InsertQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | insert into public.password_resets
     | (guid, user_guid, token, expires_at, created_at, created_by_guid, updated_at)
     | values
     | ({guid}::uuid, {user_guid}::uuid, {token}, {expires_at}::timestamptz, {created_at}::timestamptz, {created_by_guid}::uuid, {updated_at}::timestamptz)
    """.stripMargin)
  }

  private val UpdateQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | update public.password_resets
     | set user_guid = {user_guid}::uuid,
     |     token = {token},
     |     expires_at = {expires_at}::timestamptz,
     |     updated_at = {updated_at}::timestamptz
     | where guid = {guid}::uuid
    """.stripMargin)
  }

  private val DeleteQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("update public.password_resets set deleted_at = {deleted_at}::timestamptz, deleted_by_guid = {deleted_by_guid}::uuid").isNull("deleted_at")
  }

  def insert(
    user: java.util.UUID,
    form: PasswordResetForm
  ): java.util.UUID = {
    db.withConnection { c =>
      insert(c, user, form)
    }
  }

  def insert(
    c: java.sql.Connection,
    user: java.util.UUID,
    form: PasswordResetForm
  ): java.util.UUID = {
    val id = randomPkey
    bindQuery(InsertQuery, user, form)
      .bind("created_at", org.joda.time.DateTime.now)
      .bind("created_by_guid", user)
      .bind("guid", id)
      .execute(c)
    id
  }

  def insertBatch(
    user: java.util.UUID,
    forms: Seq[PasswordResetForm]
  ): Seq[java.util.UUID] = {
    db.withConnection { c =>
      insertBatch(c, user, forms)
    }
  }

  def insertBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[PasswordResetForm]
  ): Seq[java.util.UUID] = {
    forms.map { f =>
      val guid = randomPkey
      (guid, Seq(anorm.NamedParameter("created_at", org.joda.time.DateTime.now)) ++ toNamedParameter(user, guid, f))
    }.toList match {
      case Nil => Nil
      case one :: rest => {
        anorm.BatchSql(InsertQuery.sql(), one._2, rest.map(_._2)*).execute()(c)
        Seq(one._1) ++ rest.map(_._1)
      }
    }
  }

  def update(
    user: java.util.UUID,
    passwordReset: PasswordReset,
    form: PasswordResetForm
  ): Unit = {
    db.withConnection { c =>
      update(c, user, passwordReset, form)
    }
  }

  def update(
    c: java.sql.Connection,
    user: java.util.UUID,
    passwordReset: PasswordReset,
    form: PasswordResetForm
  ): Unit = {
    updateByGuid(
      c = c,
      user = user,
      guid = passwordReset.guid,
      form = form
    )
  }

  def updateByGuid(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: PasswordResetForm
  ): Unit = {
    db.withConnection { c =>
      updateByGuid(c, user, guid, form)
    }
  }

  def updateByGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    guid: java.util.UUID,
    form: PasswordResetForm
  ): Unit = {
    bindQuery(UpdateQuery, user, form)
      .bind("guid", guid)
      .execute(c)
    ()
  }

  def updateBatch(
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, PasswordResetForm)]
  ): Unit = {
    db.withConnection { c =>
      updateBatch(c, user, forms)
    }
  }

  def updateBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, PasswordResetForm)]
  ): Unit = {
    forms.map { case (guid, f) => toNamedParameter(user, guid, f) }.toList match {
      case Nil => // no-op
      case first :: rest => anorm.BatchSql(UpdateQuery.sql(), first, rest*).execute()(c)
    }
  }

  def delete(
    user: java.util.UUID,
    passwordReset: PasswordReset
  ): Unit = {
    db.withConnection { c =>
      delete(c, user, passwordReset)
    }
  }

  def delete(
    c: java.sql.Connection,
    user: java.util.UUID,
    passwordReset: PasswordReset
  ): Unit = {
    deleteByGuid(
      c = c,
      user = user,
      guid = passwordReset.guid
    )
  }

  def deleteByGuid(
    user: java.util.UUID,
    guid: java.util.UUID
  ): Unit = {
    db.withConnection { c =>
      deleteByGuid(c, user, guid)
    }
  }

  def deleteByGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    guid: java.util.UUID
  ): Unit = {
    DeleteQuery.equals("guid", guid)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByGuids(
    user: java.util.UUID,
    guids: Seq[java.util.UUID]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByGuids(c, user, guids)
    }
  }

  def deleteAllByGuids(
    c: java.sql.Connection,
    user: java.util.UUID,
    guids: Seq[java.util.UUID]
  ): Unit = {
    DeleteQuery.in("guid", guids)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteByToken(
    user: java.util.UUID,
    token: String
  ): Unit = {
    db.withConnection { c =>
      deleteByToken(c, user, token)
    }
  }

  def deleteByToken(
    c: java.sql.Connection,
    user: java.util.UUID,
    token: String
  ): Unit = {
    DeleteQuery.equals("token", token)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByTokens(
    user: java.util.UUID,
    tokens: Seq[String]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByTokens(c, user, tokens)
    }
  }

  def deleteAllByTokens(
    c: java.sql.Connection,
    user: java.util.UUID,
    tokens: Seq[String]
  ): Unit = {
    DeleteQuery.in("token", tokens)
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
    form: PasswordResetForm
  ): io.flow.postgresql.Query = {
    query
      .bind("user_guid", form.userGuid.toString)
      .bind("token", form.token)
      .bind("expires_at", form.expiresAt)
      .bind("updated_at", org.joda.time.DateTime.now)
  }

  private def toNamedParameter(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: PasswordResetForm
  ): Seq[anorm.NamedParameter] = {
    Seq(
      anorm.NamedParameter("guid", guid.toString),
      anorm.NamedParameter("user_guid", form.userGuid.toString),
      anorm.NamedParameter("token", form.token),
      anorm.NamedParameter("expires_at", form.expiresAt),
      anorm.NamedParameter("updated_at", org.joda.time.DateTime.now)
    )
  }
}