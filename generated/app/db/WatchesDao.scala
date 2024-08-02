package db.generated

case class Watch(
  guid: java.util.UUID,
  userGuid: java.util.UUID,
  applicationGuid: java.util.UUID,
  createdAt: org.joda.time.DateTime,
  createdByGuid: java.util.UUID,
  updatedAt: org.joda.time.DateTime,
  deletedAt: Option[org.joda.time.DateTime],
  deletedByGuid: Option[java.util.UUID]
) {
  def form: WatchForm = {
    WatchForm(
      userGuid = userGuid,
      applicationGuid = applicationGuid,
    )
  }
}

case class WatchForm(
  userGuid: java.util.UUID,
  applicationGuid: java.util.UUID
)

case object WatchesTable {
  val SchemaName: String = "public"

  val TableName: String = "watches"

  val QualifiedName: String = "public.watches"

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

    case object ApplicationGuid extends Column {
      override val name: String = "application_guid"
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

    val all: List[Column] = List(Guid, UserGuid, ApplicationGuid, CreatedAt, CreatedByGuid, UpdatedAt, DeletedAt, DeletedByGuid)
  }
}

trait BaseWatchesDao {
  import anorm.*

  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def db: play.api.db.Database

  private val BaseQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | select guid::text,
     |        user_guid::text,
     |        application_guid::text,
     |        created_at,
     |        created_by_guid::text,
     |        updated_at,
     |        deleted_at,
     |        deleted_by_guid::text
     |   from public.watches
     |""".stripMargin.stripTrailing
    )
  }

  def findAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    userGuid: Option[java.util.UUID] = None,
    userGuids: Option[Seq[java.util.UUID]] = None,
    applicationGuid: Option[java.util.UUID] = None,
    applicationGuids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[Watch] = {
    db.withConnection { c =>
      findAllWithConnection(c, guid, guids, userGuid, userGuids, applicationGuid, applicationGuids, limit, offset, orderBy)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    userGuid: Option[java.util.UUID] = None,
    userGuids: Option[Seq[java.util.UUID]] = None,
    applicationGuid: Option[java.util.UUID] = None,
    applicationGuids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[Watch] = {
    customQueryModifier(BaseQuery)
      .equals("watches.guid", guid)
      .optionalIn("watches.guid", guids)
      .equals("watches.user_guid", userGuid)
      .optionalIn("watches.user_guid", userGuids)
      .equals("watches.application_guid", applicationGuid)
      .optionalIn("watches.application_guid", applicationGuids)
      .optionalLimit(limit)
      .offset(offset)
      .orderBy(orderBy.flatMap(_.sql))
      .as(parser.*)(c)
  }

  def iterateAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    userGuid: Option[java.util.UUID] = None,
    userGuids: Option[Seq[java.util.UUID]] = None,
    applicationGuid: Option[java.util.UUID] = None,
    applicationGuids: Option[Seq[java.util.UUID]] = None,
    pageSize: Long = 1000
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Iterator[Watch] = {
    assert(pageSize > 0, "pageSize must be > 0")

    def iterate(lastValue: Option[Watch]): Iterator[Watch] = {
      val page: Seq[Watch] = db.withConnection { c =>
        customQueryModifier(BaseQuery)
          .equals("watches.guid", guid)
          .optionalIn("watches.guid", guids)
          .equals("watches.user_guid", userGuid)
          .optionalIn("watches.user_guid", userGuids)
          .equals("watches.application_guid", applicationGuid)
          .optionalIn("watches.application_guid", applicationGuids)
          .greaterThan("watches.guid", lastValue.map(_.guid))
          .orderBy("watches.guid")
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

  def findByGuid(guid: java.util.UUID): Option[Watch] = {
    db.withConnection { c =>
      findByGuidWithConnection(c, guid)
    }
  }

  def findByGuidWithConnection(
    c: java.sql.Connection,
    guid: java.util.UUID
  ): Option[Watch] = {
    findAllWithConnection(
      c = c,
      guid = Some(guid),
      limit = Some(1)
    ).headOption
  }

  def findAllByUserGuid(userGuid: java.util.UUID): Seq[Watch] = {
    db.withConnection { c =>
      findAllByUserGuidWithConnection(c, userGuid)
    }
  }

  def findAllByUserGuidWithConnection(
    c: java.sql.Connection,
    userGuid: java.util.UUID
  ): Seq[Watch] = {
    findAllWithConnection(
      c = c,
      userGuid = Some(userGuid),
      limit = None
    )
  }

  def findAllByApplicationGuid(applicationGuid: java.util.UUID): Seq[Watch] = {
    db.withConnection { c =>
      findAllByApplicationGuidWithConnection(c, applicationGuid)
    }
  }

  def findAllByApplicationGuidWithConnection(
    c: java.sql.Connection,
    applicationGuid: java.util.UUID
  ): Seq[Watch] = {
    findAllWithConnection(
      c = c,
      applicationGuid = Some(applicationGuid),
      limit = None
    )
  }

  private val parser: anorm.RowParser[Watch] = {
    anorm.SqlParser.str("guid") ~
      anorm.SqlParser.str("user_guid") ~
      anorm.SqlParser.str("application_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("created_at") ~
      anorm.SqlParser.str("created_by_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("updated_at") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("deleted_at").? ~
      anorm.SqlParser.str("deleted_by_guid").? map { case guid ~ userGuid ~ applicationGuid ~ createdAt ~ createdByGuid ~ updatedAt ~ deletedAt ~ deletedByGuid =>
      Watch(
        guid = java.util.UUID.fromString(guid),
        userGuid = java.util.UUID.fromString(userGuid),
        applicationGuid = java.util.UUID.fromString(applicationGuid),
        createdAt = createdAt,
        createdByGuid = java.util.UUID.fromString(createdByGuid),
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        deletedByGuid = deletedByGuid.map { v => java.util.UUID.fromString(v) }
      )
    }
  }
}

class WatchesDao @javax.inject.Inject() (override val db: play.api.db.Database) extends BaseWatchesDao {
  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def randomPkey: java.util.UUID = {
    java.util.UUID.randomUUID
  }

  private val InsertQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | insert into public.watches
     | (guid, user_guid, application_guid, created_at, created_by_guid, updated_at)
     | values
     | ({guid}::uuid, {user_guid}::uuid, {application_guid}::uuid, {created_at}::timestamptz, {created_by_guid}::uuid, {updated_at}::timestamptz)
    """.stripMargin)
  }

  private val UpdateQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | update public.watches
     | set user_guid = {user_guid}::uuid,
     |     application_guid = {application_guid}::uuid,
     |     updated_at = {updated_at}::timestamptz
     | where guid = {guid}::uuid
    """.stripMargin)
  }

  private val DeleteQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("update public.watches set deleted_at = {deleted_at}::timestamptz, deleted_by_guid = {deleted_by_guid}::uuid").isNull("deleted_at")
  }

  def insert(
    user: java.util.UUID,
    form: WatchForm
  ): java.util.UUID = {
    db.withConnection { c =>
      insert(c, user, form)
    }
  }

  def insert(
    c: java.sql.Connection,
    user: java.util.UUID,
    form: WatchForm
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
    forms: Seq[WatchForm]
  ): Seq[java.util.UUID] = {
    db.withConnection { c =>
      insertBatch(c, user, forms)
    }
  }

  def insertBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[WatchForm]
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
    watch: Watch,
    form: WatchForm
  ): Unit = {
    db.withConnection { c =>
      update(c, user, watch, form)
    }
  }

  def update(
    c: java.sql.Connection,
    user: java.util.UUID,
    watch: Watch,
    form: WatchForm
  ): Unit = {
    updateByGuid(
      c = c,
      user = user,
      guid = watch.guid,
      form = form
    )
  }

  def updateByGuid(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: WatchForm
  ): Unit = {
    db.withConnection { c =>
      updateByGuid(c, user, guid, form)
    }
  }

  def updateByGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    guid: java.util.UUID,
    form: WatchForm
  ): Unit = {
    bindQuery(UpdateQuery, user, form)
      .bind("guid", guid)
      .execute(c)
    ()
  }

  def updateBatch(
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, WatchForm)]
  ): Unit = {
    db.withConnection { c =>
      updateBatch(c, user, forms)
    }
  }

  def updateBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, WatchForm)]
  ): Unit = {
    forms.map { case (guid, f) => toNamedParameter(user, guid, f) }.toList match {
      case Nil => // no-op
      case first :: rest => anorm.BatchSql(UpdateQuery.sql(), first, rest*).execute()(c)
    }
  }

  def delete(
    user: java.util.UUID,
    watch: Watch
  ): Unit = {
    db.withConnection { c =>
      delete(c, user, watch)
    }
  }

  def delete(
    c: java.sql.Connection,
    user: java.util.UUID,
    watch: Watch
  ): Unit = {
    deleteByGuid(
      c = c,
      user = user,
      guid = watch.guid
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

  def deleteAllByApplicationGuid(
    user: java.util.UUID,
    applicationGuid: java.util.UUID
  ): Unit = {
    db.withConnection { c =>
      deleteAllByApplicationGuid(c, user, applicationGuid)
    }
  }

  def deleteAllByApplicationGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    applicationGuid: java.util.UUID
  ): Unit = {
    DeleteQuery.equals("application_guid", applicationGuid)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByApplicationGuids(
    user: java.util.UUID,
    applicationGuids: Seq[java.util.UUID]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByApplicationGuids(c, user, applicationGuids)
    }
  }

  def deleteAllByApplicationGuids(
    c: java.sql.Connection,
    user: java.util.UUID,
    applicationGuids: Seq[java.util.UUID]
  ): Unit = {
    DeleteQuery.in("application_guid", applicationGuids)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  private def bindQuery(
    query: io.flow.postgresql.Query,
    user: java.util.UUID,
    form: WatchForm
  ): io.flow.postgresql.Query = {
    query
      .bind("user_guid", form.userGuid.toString)
      .bind("application_guid", form.applicationGuid.toString)
      .bind("updated_at", org.joda.time.DateTime.now)
  }

  private def toNamedParameter(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: WatchForm
  ): Seq[anorm.NamedParameter] = {
    Seq(
      anorm.NamedParameter("guid", guid.toString),
      anorm.NamedParameter("user_guid", form.userGuid.toString),
      anorm.NamedParameter("application_guid", form.applicationGuid.toString),
      anorm.NamedParameter("updated_at", org.joda.time.DateTime.now)
    )
  }
}