package db.generated

case class Original(
  id: Long,
  versionGuid: java.util.UUID,
  `type`: String,
  data: String,
  createdAt: org.joda.time.DateTime,
  createdByGuid: java.util.UUID,
  updatedAt: org.joda.time.DateTime,
  deletedAt: Option[org.joda.time.DateTime],
  deletedByGuid: Option[java.util.UUID]
) {
  def form: OriginalForm = {
    OriginalForm(
      versionGuid = versionGuid,
      `type` = `type`,
      data = data,
    )
  }
}

case class OriginalForm(
  versionGuid: java.util.UUID,
  `type`: String,
  data: String
)

case object OriginalsTable {
  val SchemaName: String = "public"

  val TableName: String = "originals"

  val QualifiedName: String = "public.originals"

  sealed trait Column {
    def name: String
  }

  object Columns {
    case object Id extends Column {
      override val name: String = "id"
    }

    case object VersionGuid extends Column {
      override val name: String = "version_guid"
    }

    case object Type extends Column {
      override val name: String = "type"
    }

    case object Data extends Column {
      override val name: String = "data"
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

    val all: List[Column] = List(Id, VersionGuid, Type, Data, CreatedAt, CreatedByGuid, UpdatedAt, DeletedAt, DeletedByGuid)
  }
}

trait BaseOriginalsDao {
  import anorm.*

  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def db: play.api.db.Database

  private val BaseQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | select id,
     |        version_guid::text,
     |        type,
     |        data,
     |        created_at,
     |        created_by_guid::text,
     |        updated_at,
     |        deleted_at,
     |        deleted_by_guid::text
     |   from public.originals
     |""".stripMargin.stripTrailing
    )
  }

  def findAll(
    id: Option[Long] = None,
    ids: Option[Seq[Long]] = None,
    versionGuid: Option[java.util.UUID] = None,
    versionGuids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[Original] = {
    db.withConnection { c =>
      findAllWithConnection(c, id, ids, versionGuid, versionGuids, limit, offset, orderBy)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    id: Option[Long] = None,
    ids: Option[Seq[Long]] = None,
    versionGuid: Option[java.util.UUID] = None,
    versionGuids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[Original] = {
    customQueryModifier(BaseQuery)
      .equals("originals.id", id)
      .optionalIn("originals.id", ids)
      .equals("originals.version_guid", versionGuid)
      .optionalIn("originals.version_guid", versionGuids)
      .optionalLimit(limit)
      .offset(offset)
      .orderBy(orderBy.flatMap(_.sql))
      .as(parser.*)(c)
  }

  def iterateAll(
    id: Option[Long] = None,
    ids: Option[Seq[Long]] = None,
    versionGuid: Option[java.util.UUID] = None,
    versionGuids: Option[Seq[java.util.UUID]] = None,
    pageSize: Long = 1000
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Iterator[Original] = {
    assert(pageSize > 0, "pageSize must be > 0")

    def iterate(lastValue: Option[Original]): Iterator[Original] = {
      val page: Seq[Original] = db.withConnection { c =>
        customQueryModifier(BaseQuery)
          .equals("originals.id", id)
          .optionalIn("originals.id", ids)
          .equals("originals.version_guid", versionGuid)
          .optionalIn("originals.version_guid", versionGuids)
          .greaterThan("originals.id", lastValue.map(_.id))
          .orderBy("originals.id")
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

  def findById(id: Long): Option[Original] = {
    db.withConnection { c =>
      findByIdWithConnection(c, id)
    }
  }

  def findByIdWithConnection(
    c: java.sql.Connection,
    id: Long
  ): Option[Original] = {
    findAllWithConnection(
      c = c,
      id = Some(id),
      limit = Some(1)
    ).headOption
  }

  def findAllByVersionGuid(versionGuid: java.util.UUID): Seq[Original] = {
    db.withConnection { c =>
      findAllByVersionGuidWithConnection(c, versionGuid)
    }
  }

  def findAllByVersionGuidWithConnection(
    c: java.sql.Connection,
    versionGuid: java.util.UUID
  ): Seq[Original] = {
    findAllWithConnection(
      c = c,
      versionGuid = Some(versionGuid),
      limit = None
    )
  }

  private val parser: anorm.RowParser[Original] = {
    anorm.SqlParser.long("id") ~
      anorm.SqlParser.str("version_guid") ~
      anorm.SqlParser.str("type") ~
      anorm.SqlParser.str("data") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("created_at") ~
      anorm.SqlParser.str("created_by_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("updated_at") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("deleted_at").? ~
      anorm.SqlParser.str("deleted_by_guid").? map { case id ~ versionGuid ~ type_ ~ data ~ createdAt ~ createdByGuid ~ updatedAt ~ deletedAt ~ deletedByGuid =>
      Original(
        id = id,
        versionGuid = java.util.UUID.fromString(versionGuid),
        `type` = type_,
        data = data,
        createdAt = createdAt,
        createdByGuid = java.util.UUID.fromString(createdByGuid),
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        deletedByGuid = deletedByGuid.map { v => java.util.UUID.fromString(v) }
      )
    }
  }
}

class OriginalsDao @javax.inject.Inject() (override val db: play.api.db.Database) extends BaseOriginalsDao {
  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  private val InsertQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | insert into public.originals
     | (version_guid, type, data, created_at, created_by_guid, updated_at)
     | values
     | ({version_guid}::uuid, {type}, {data}, {created_at}::timestamptz, {created_by_guid}::uuid, {updated_at}::timestamptz)
    """.stripMargin)
  }

  private val UpdateQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | update public.originals
     | set version_guid = {version_guid}::uuid,
     |     type = {type},
     |     data = {data},
     |     updated_at = {updated_at}::timestamptz
     | where id = {id}::bigint
    """.stripMargin)
  }

  private val DeleteQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("update public.originals set deleted_at = {deleted_at}::timestamptz, deleted_by_guid = {deleted_by_guid}::uuid").isNull("deleted_at")
  }

  def insert(
    user: java.util.UUID,
    form: OriginalForm
  ): Unit = {
    db.withConnection { c =>
      insert(c, user, form)
    }
  }

  def insert(
    c: java.sql.Connection,
    user: java.util.UUID,
    form: OriginalForm
  ): Unit = {
    bindQuery(InsertQuery, user, form)
      .bind("created_at", org.joda.time.DateTime.now)
      .bind("created_by_guid", user)
      .execute(c)
  }

  def insertBatch(
    user: java.util.UUID,
    forms: Seq[OriginalForm]
  ): Seq[Unit] = {
    db.withConnection { c =>
      insertBatch(c, user, forms)
    }
  }

  def insertBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[OriginalForm]
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
    original: Original,
    form: OriginalForm
  ): Unit = {
    db.withConnection { c =>
      update(c, user, original, form)
    }
  }

  def update(
    c: java.sql.Connection,
    user: java.util.UUID,
    original: Original,
    form: OriginalForm
  ): Unit = {
    updateById(
      c = c,
      user = user,
      id = original.id,
      form = form
    )
  }

  def updateById(
    user: java.util.UUID,
    id: Long,
    form: OriginalForm
  ): Unit = {
    db.withConnection { c =>
      updateById(c, user, id, form)
    }
  }

  def updateById(
    c: java.sql.Connection,
    user: java.util.UUID,
    id: Long,
    form: OriginalForm
  ): Unit = {
    bindQuery(UpdateQuery, user, form)
      .bind("id", id)
      .execute(c)
    ()
  }

  def updateBatch(
    user: java.util.UUID,
    forms: Seq[OriginalForm]
  ): Unit = {
    db.withConnection { c =>
      updateBatch(c, user, forms)
    }
  }

  def updateBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[OriginalForm]
  ): Unit = {
    forms.map { f => toNamedParameter(user, f) }.toList match {
      case Nil => // no-op
      case first :: rest => anorm.BatchSql(UpdateQuery.sql(), first, rest*).execute()(c)
    }
  }

  def delete(
    user: java.util.UUID,
    original: Original
  ): Unit = {
    db.withConnection { c =>
      delete(c, user, original)
    }
  }

  def delete(
    c: java.sql.Connection,
    user: java.util.UUID,
    original: Original
  ): Unit = {
    deleteById(
      c = c,
      user = user,
      id = original.id
    )
  }

  def deleteById(
    user: java.util.UUID,
    id: Long
  ): Unit = {
    db.withConnection { c =>
      deleteById(c, user, id)
    }
  }

  def deleteById(
    c: java.sql.Connection,
    user: java.util.UUID,
    id: Long
  ): Unit = {
    DeleteQuery.equals("id", id)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByIds(
    user: java.util.UUID,
    ids: Seq[Long]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByIds(c, user, ids)
    }
  }

  def deleteAllByIds(
    c: java.sql.Connection,
    user: java.util.UUID,
    ids: Seq[Long]
  ): Unit = {
    DeleteQuery.in("id", ids)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByVersionGuid(
    user: java.util.UUID,
    versionGuid: java.util.UUID
  ): Unit = {
    db.withConnection { c =>
      deleteAllByVersionGuid(c, user, versionGuid)
    }
  }

  def deleteAllByVersionGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    versionGuid: java.util.UUID
  ): Unit = {
    DeleteQuery.equals("version_guid", versionGuid)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByVersionGuids(
    user: java.util.UUID,
    versionGuids: Seq[java.util.UUID]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByVersionGuids(c, user, versionGuids)
    }
  }

  def deleteAllByVersionGuids(
    c: java.sql.Connection,
    user: java.util.UUID,
    versionGuids: Seq[java.util.UUID]
  ): Unit = {
    DeleteQuery.in("version_guid", versionGuids)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  private def bindQuery(
    query: io.flow.postgresql.Query,
    user: java.util.UUID,
    form: OriginalForm
  ): io.flow.postgresql.Query = {
    query
      .bind("version_guid", form.versionGuid.toString)
      .bind("type", form.`type`)
      .bind("data", form.data)
      .bind("updated_at", org.joda.time.DateTime.now)
  }

  private def toNamedParameter(
    user: java.util.UUID,
    form: OriginalForm
  ): Seq[anorm.NamedParameter] = {
    Seq(
      anorm.NamedParameter("version_guid", form.versionGuid.toString),
      anorm.NamedParameter("type", form.`type`),
      anorm.NamedParameter("data", form.data),
      anorm.NamedParameter("updated_at", org.joda.time.DateTime.now)
    )
  }
}