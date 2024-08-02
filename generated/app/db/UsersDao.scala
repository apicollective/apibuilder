package db.generated

case class User(
  guid: java.util.UUID,
  email: String,
  nickname: String,
  name: Option[String],
  avatarUrl: Option[String],
  gravatarId: Option[String],
  createdAt: org.joda.time.DateTime,
  createdByGuid: java.util.UUID,
  updatedAt: org.joda.time.DateTime,
  updatedByGuid: java.util.UUID,
  deletedAt: Option[org.joda.time.DateTime],
  deletedByGuid: Option[java.util.UUID]
) {
  def form: UserForm = {
    UserForm(
      email = email,
      nickname = nickname,
      name = name,
      avatarUrl = avatarUrl,
      gravatarId = gravatarId,
    )
  }
}

case class UserForm(
  email: String,
  nickname: String,
  name: Option[String],
  avatarUrl: Option[String],
  gravatarId: Option[String]
)

case object UsersTable {
  val SchemaName: String = "public"

  val TableName: String = "users"

  val QualifiedName: String = "public.users"

  sealed trait Column {
    def name: String
  }

  object Columns {
    case object Guid extends Column {
      override val name: String = "guid"
    }

    case object Email extends Column {
      override val name: String = "email"
    }

    case object Nickname extends Column {
      override val name: String = "nickname"
    }

    case object Name extends Column {
      override val name: String = "name"
    }

    case object AvatarUrl extends Column {
      override val name: String = "avatar_url"
    }

    case object GravatarId extends Column {
      override val name: String = "gravatar_id"
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

    val all: List[Column] = List(Guid, Email, Nickname, Name, AvatarUrl, GravatarId, CreatedAt, CreatedByGuid, UpdatedAt, UpdatedByGuid, DeletedAt, DeletedByGuid)
  }
}

trait BaseUsersDao {
  import anorm.*

  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def db: play.api.db.Database

  private val BaseQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | select guid::text,
     |        email,
     |        nickname,
     |        name,
     |        avatar_url,
     |        gravatar_id,
     |        created_at,
     |        created_by_guid::text,
     |        updated_at,
     |        updated_by_guid::text,
     |        deleted_at,
     |        deleted_by_guid::text
     |   from public.users
     |""".stripMargin.stripTrailing
    )
  }

  def findAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[User] = {
    db.withConnection { c =>
      findAllWithConnection(c, guid, guids, limit, offset, orderBy)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[User] = {
    customQueryModifier(BaseQuery)
      .equals("users.guid", guid)
      .optionalIn("users.guid", guids)
      .optionalLimit(limit)
      .offset(offset)
      .orderBy(orderBy.flatMap(_.sql))
      .as(parser.*)(c)
  }

  def iterateAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    pageSize: Long = 1000
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Iterator[User] = {
    assert(pageSize > 0, "pageSize must be > 0")

    def iterate(lastValue: Option[User]): Iterator[User] = {
      val page: Seq[User] = db.withConnection { c =>
        customQueryModifier(BaseQuery)
          .equals("users.guid", guid)
          .optionalIn("users.guid", guids)
          .greaterThan("users.guid", lastValue.map(_.guid))
          .orderBy("users.guid")
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

  def findByGuid(guid: java.util.UUID): Option[User] = {
    db.withConnection { c =>
      findByGuidWithConnection(c, guid)
    }
  }

  def findByGuidWithConnection(
    c: java.sql.Connection,
    guid: java.util.UUID
  ): Option[User] = {
    findAllWithConnection(
      c = c,
      guid = Some(guid),
      limit = Some(1)
    ).headOption
  }

  private val parser: anorm.RowParser[User] = {
    anorm.SqlParser.str("guid") ~
      anorm.SqlParser.str("email") ~
      anorm.SqlParser.str("nickname") ~
      anorm.SqlParser.str("name").? ~
      anorm.SqlParser.str("avatar_url").? ~
      anorm.SqlParser.str("gravatar_id").? ~
      anorm.SqlParser.get[org.joda.time.DateTime]("created_at") ~
      anorm.SqlParser.str("created_by_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("updated_at") ~
      anorm.SqlParser.str("updated_by_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("deleted_at").? ~
      anorm.SqlParser.str("deleted_by_guid").? map { case guid ~ email ~ nickname ~ name ~ avatarUrl ~ gravatarId ~ createdAt ~ createdByGuid ~ updatedAt ~ updatedByGuid ~ deletedAt ~ deletedByGuid =>
      User(
        guid = java.util.UUID.fromString(guid),
        email = email,
        nickname = nickname,
        name = name,
        avatarUrl = avatarUrl,
        gravatarId = gravatarId,
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

class UsersDao @javax.inject.Inject() (override val db: play.api.db.Database) extends BaseUsersDao {
  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def randomPkey: java.util.UUID = {
    java.util.UUID.randomUUID
  }

  private val InsertQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | insert into public.users
     | (guid, email, nickname, name, avatar_url, gravatar_id, created_at, created_by_guid, updated_at, updated_by_guid)
     | values
     | ({guid}::uuid, {email}, {nickname}, {name}, {avatar_url}, {gravatar_id}, {created_at}::timestamptz, {created_by_guid}::uuid, {updated_at}::timestamptz, {updated_by_guid}::uuid)
    """.stripMargin)
  }

  private val UpdateQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | update public.users
     | set email = {email},
     |     nickname = {nickname},
     |     name = {name},
     |     avatar_url = {avatar_url},
     |     gravatar_id = {gravatar_id},
     |     updated_at = {updated_at}::timestamptz,
     |     updated_by_guid = {updated_by_guid}::uuid
     | where guid = {guid}::uuid
    """.stripMargin)
  }

  private val DeleteQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("update public.users set deleted_at = {deleted_at}::timestamptz, deleted_by_guid = {deleted_by_guid}::uuid")
  }

  def insert(
    modifyingUser: java.util.UUID,
    form: UserForm
  ): java.util.UUID = {
    db.withConnection { c =>
      insert(c, modifyingUser, form)
    }
  }

  def insert(
    c: java.sql.Connection,
    modifyingUser: java.util.UUID,
    form: UserForm
  ): java.util.UUID = {
    val id = randomPkey
    bindQuery(InsertQuery, modifyingUser, form)
      .bind("created_at", org.joda.time.DateTime.now)
      .bind("created_by_guid", modifyingUser)
      .bind("guid", id)
      .execute(c)
    id
  }

  def insertBatch(
    modifyingUser: java.util.UUID,
    forms: Seq[UserForm]
  ): Seq[java.util.UUID] = {
    db.withConnection { c =>
      insertBatch(c, modifyingUser, forms)
    }
  }

  def insertBatch(
    c: java.sql.Connection,
    modifyingUser: java.util.UUID,
    forms: Seq[UserForm]
  ): Seq[java.util.UUID] = {
    forms.map { f =>
      val guid = randomPkey
      (guid, Seq(anorm.NamedParameter("created_at", org.joda.time.DateTime.now)) ++ toNamedParameter(modifyingUser, guid, f))
    }.toList match {
      case Nil => Nil
      case one :: rest => {
        anorm.BatchSql(InsertQuery.sql(), one._2, rest.map(_._2)*).execute()(c)
        Seq(one._1) ++ rest.map(_._1)
      }
    }
  }

  def update(
    modifyingUser: java.util.UUID,
    user: User,
    form: UserForm
  ): Unit = {
    db.withConnection { c =>
      update(c, modifyingUser, user, form)
    }
  }

  def update(
    c: java.sql.Connection,
    modifyingUser: java.util.UUID,
    user: User,
    form: UserForm
  ): Unit = {
    updateByGuid(
      c = c,
      modifyingUser = modifyingUser,
      guid = user.guid,
      form = form
    )
  }

  def updateByGuid(
    modifyingUser: java.util.UUID,
    guid: java.util.UUID,
    form: UserForm
  ): Unit = {
    db.withConnection { c =>
      updateByGuid(c, modifyingUser, guid, form)
    }
  }

  def updateByGuid(
    c: java.sql.Connection,
    modifyingUser: java.util.UUID,
    guid: java.util.UUID,
    form: UserForm
  ): Unit = {
    bindQuery(UpdateQuery, modifyingUser, form)
      .bind("guid", guid)
      .bind("updated_by_guid", modifyingUser)
      .execute(c)
    ()
  }

  def updateBatch(
    modifyingUser: java.util.UUID,
    forms: Seq[(java.util.UUID, UserForm)]
  ): Unit = {
    db.withConnection { c =>
      updateBatch(c, modifyingUser, forms)
    }
  }

  def updateBatch(
    c: java.sql.Connection,
    modifyingUser: java.util.UUID,
    forms: Seq[(java.util.UUID, UserForm)]
  ): Unit = {
    forms.map { case (guid, f) => toNamedParameter(modifyingUser, guid, f) }.toList match {
      case Nil => // no-op
      case first :: rest => anorm.BatchSql(UpdateQuery.sql(), first, rest*).execute()(c)
    }
  }

  def delete(
    modifyingUser: java.util.UUID,
    user: User
  ): Unit = {
    db.withConnection { c =>
      delete(c, modifyingUser, user)
    }
  }

  def delete(
    c: java.sql.Connection,
    modifyingUser: java.util.UUID,
    user: User
  ): Unit = {
    deleteByGuid(
      c = c,
      modifyingUser = modifyingUser,
      guid = user.guid
    )
  }

  def deleteByGuid(
    modifyingUser: java.util.UUID,
    guid: java.util.UUID
  ): Unit = {
    db.withConnection { c =>
      deleteByGuid(c, modifyingUser, guid)
    }
  }

  def deleteByGuid(
    c: java.sql.Connection,
    modifyingUser: java.util.UUID,
    guid: java.util.UUID
  ): Unit = {
    DeleteQuery.equals("guid", guid)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", modifyingUser)
      .execute(c)
  }

  def deleteAllByGuids(
    modifyingUser: java.util.UUID,
    guids: Seq[java.util.UUID]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByGuids(c, modifyingUser, guids)
    }
  }

  def deleteAllByGuids(
    c: java.sql.Connection,
    modifyingUser: java.util.UUID,
    guids: Seq[java.util.UUID]
  ): Unit = {
    DeleteQuery.in("guid", guids)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", modifyingUser)
      .execute(c)
  }

  private def bindQuery(
    query: io.flow.postgresql.Query,
    modifyingUser: java.util.UUID,
    form: UserForm
  ): io.flow.postgresql.Query = {
    query
      .bind("email", form.email)
      .bind("nickname", form.nickname)
      .bind("name", form.name)
      .bind("avatar_url", form.avatarUrl)
      .bind("gravatar_id", form.gravatarId)
      .bind("updated_at", org.joda.time.DateTime.now)
      .bind("updated_by_guid", modifyingUser)
  }

  private def toNamedParameter(
    modifyingUser: java.util.UUID,
    guid: java.util.UUID,
    form: UserForm
  ): Seq[anorm.NamedParameter] = {
    Seq(
      anorm.NamedParameter("guid", guid.toString),
      anorm.NamedParameter("email", form.email),
      anorm.NamedParameter("nickname", form.nickname),
      anorm.NamedParameter("name", form.name),
      anorm.NamedParameter("avatar_url", form.avatarUrl),
      anorm.NamedParameter("gravatar_id", form.gravatarId),
      anorm.NamedParameter("updated_at", org.joda.time.DateTime.now),
      anorm.NamedParameter("updated_by_guid", modifyingUser.toString)
    )
  }
}