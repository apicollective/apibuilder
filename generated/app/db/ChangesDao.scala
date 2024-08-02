package db.generated

case class Change(
  guid: java.util.UUID,
  applicationGuid: java.util.UUID,
  fromVersionGuid: java.util.UUID,
  toVersionGuid: java.util.UUID,
  `type`: String,
  description: String,
  changedAt: org.joda.time.DateTime,
  changedByGuid: java.util.UUID,
  isMaterial: Boolean,
  createdAt: org.joda.time.DateTime,
  createdByGuid: java.util.UUID,
  updatedAt: org.joda.time.DateTime,
  deletedAt: Option[org.joda.time.DateTime],
  deletedByGuid: Option[java.util.UUID]
) {
  def form: ChangeForm = {
    ChangeForm(
      applicationGuid = applicationGuid,
      fromVersionGuid = fromVersionGuid,
      toVersionGuid = toVersionGuid,
      `type` = `type`,
      description = description,
      changedAt = changedAt,
      changedByGuid = changedByGuid,
      isMaterial = isMaterial,
    )
  }
}

case class ChangeForm(
  applicationGuid: java.util.UUID,
  fromVersionGuid: java.util.UUID,
  toVersionGuid: java.util.UUID,
  `type`: String,
  description: String,
  changedAt: org.joda.time.DateTime,
  changedByGuid: java.util.UUID,
  isMaterial: Boolean
)

case object ChangesTable {
  val SchemaName: String = "public"

  val TableName: String = "changes"

  val QualifiedName: String = "public.changes"

  sealed trait Column {
    def name: String
  }

  object Columns {
    case object Guid extends Column {
      override val name: String = "guid"
    }

    case object ApplicationGuid extends Column {
      override val name: String = "application_guid"
    }

    case object FromVersionGuid extends Column {
      override val name: String = "from_version_guid"
    }

    case object ToVersionGuid extends Column {
      override val name: String = "to_version_guid"
    }

    case object Type extends Column {
      override val name: String = "type"
    }

    case object Description extends Column {
      override val name: String = "description"
    }

    case object ChangedAt extends Column {
      override val name: String = "changed_at"
    }

    case object ChangedByGuid extends Column {
      override val name: String = "changed_by_guid"
    }

    case object IsMaterial extends Column {
      override val name: String = "is_material"
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

    val all: List[Column] = List(Guid, ApplicationGuid, FromVersionGuid, ToVersionGuid, Type, Description, ChangedAt, ChangedByGuid, IsMaterial, CreatedAt, CreatedByGuid, UpdatedAt, DeletedAt, DeletedByGuid)
  }
}

trait BaseChangesDao {
  import anorm.*

  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def db: play.api.db.Database

  private val BaseQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | select guid::text,
     |        application_guid::text,
     |        from_version_guid::text,
     |        to_version_guid::text,
     |        type,
     |        description,
     |        changed_at,
     |        changed_by_guid::text,
     |        is_material,
     |        created_at,
     |        created_by_guid::text,
     |        updated_at,
     |        deleted_at,
     |        deleted_by_guid::text
     |   from public.changes
     |""".stripMargin.stripTrailing
    )
  }

  def findAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    applicationGuid: Option[java.util.UUID] = None,
    applicationGuids: Option[Seq[java.util.UUID]] = None,
    toVersionGuid: Option[java.util.UUID] = None,
    toVersionGuids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[Change] = {
    db.withConnection { c =>
      findAllWithConnection(c, guid, guids, applicationGuid, applicationGuids, toVersionGuid, toVersionGuids, limit, offset, orderBy)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    applicationGuid: Option[java.util.UUID] = None,
    applicationGuids: Option[Seq[java.util.UUID]] = None,
    toVersionGuid: Option[java.util.UUID] = None,
    toVersionGuids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[Change] = {
    customQueryModifier(BaseQuery)
      .equals("changes.guid", guid)
      .optionalIn("changes.guid", guids)
      .equals("changes.application_guid", applicationGuid)
      .optionalIn("changes.application_guid", applicationGuids)
      .equals("changes.to_version_guid", toVersionGuid)
      .optionalIn("changes.to_version_guid", toVersionGuids)
      .optionalLimit(limit)
      .offset(offset)
      .orderBy(orderBy.flatMap(_.sql))
      .as(parser.*)(c)
  }

  def iterateAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    applicationGuid: Option[java.util.UUID] = None,
    applicationGuids: Option[Seq[java.util.UUID]] = None,
    toVersionGuid: Option[java.util.UUID] = None,
    toVersionGuids: Option[Seq[java.util.UUID]] = None,
    pageSize: Long = 1000
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Iterator[Change] = {
    assert(pageSize > 0, "pageSize must be > 0")

    def iterate(lastValue: Option[Change]): Iterator[Change] = {
      val page: Seq[Change] = db.withConnection { c =>
        customQueryModifier(BaseQuery)
          .equals("changes.guid", guid)
          .optionalIn("changes.guid", guids)
          .equals("changes.application_guid", applicationGuid)
          .optionalIn("changes.application_guid", applicationGuids)
          .equals("changes.to_version_guid", toVersionGuid)
          .optionalIn("changes.to_version_guid", toVersionGuids)
          .greaterThan("changes.guid", lastValue.map(_.guid))
          .orderBy("changes.guid")
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

  def findByGuid(guid: java.util.UUID): Option[Change] = {
    db.withConnection { c =>
      findByGuidWithConnection(c, guid)
    }
  }

  def findByGuidWithConnection(
    c: java.sql.Connection,
    guid: java.util.UUID
  ): Option[Change] = {
    findAllWithConnection(
      c = c,
      guid = Some(guid),
      limit = Some(1)
    ).headOption
  }

  def findAllByApplicationGuid(applicationGuid: java.util.UUID): Seq[Change] = {
    db.withConnection { c =>
      findAllByApplicationGuidWithConnection(c, applicationGuid)
    }
  }

  def findAllByApplicationGuidWithConnection(
    c: java.sql.Connection,
    applicationGuid: java.util.UUID
  ): Seq[Change] = {
    findAllWithConnection(
      c = c,
      applicationGuid = Some(applicationGuid),
      limit = None
    )
  }

  def findAllByToVersionGuid(toVersionGuid: java.util.UUID): Seq[Change] = {
    db.withConnection { c =>
      findAllByToVersionGuidWithConnection(c, toVersionGuid)
    }
  }

  def findAllByToVersionGuidWithConnection(
    c: java.sql.Connection,
    toVersionGuid: java.util.UUID
  ): Seq[Change] = {
    findAllWithConnection(
      c = c,
      toVersionGuid = Some(toVersionGuid),
      limit = None
    )
  }

  private val parser: anorm.RowParser[Change] = {
    anorm.SqlParser.str("guid") ~
      anorm.SqlParser.str("application_guid") ~
      anorm.SqlParser.str("from_version_guid") ~
      anorm.SqlParser.str("to_version_guid") ~
      anorm.SqlParser.str("type") ~
      anorm.SqlParser.str("description") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("changed_at") ~
      anorm.SqlParser.str("changed_by_guid") ~
      anorm.SqlParser.bool("is_material") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("created_at") ~
      anorm.SqlParser.str("created_by_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("updated_at") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("deleted_at").? ~
      anorm.SqlParser.str("deleted_by_guid").? map { case guid ~ applicationGuid ~ fromVersionGuid ~ toVersionGuid ~ type_ ~ description ~ changedAt ~ changedByGuid ~ isMaterial ~ createdAt ~ createdByGuid ~ updatedAt ~ deletedAt ~ deletedByGuid =>
      Change(
        guid = java.util.UUID.fromString(guid),
        applicationGuid = java.util.UUID.fromString(applicationGuid),
        fromVersionGuid = java.util.UUID.fromString(fromVersionGuid),
        toVersionGuid = java.util.UUID.fromString(toVersionGuid),
        `type` = type_,
        description = description,
        changedAt = changedAt,
        changedByGuid = java.util.UUID.fromString(changedByGuid),
        isMaterial = isMaterial,
        createdAt = createdAt,
        createdByGuid = java.util.UUID.fromString(createdByGuid),
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        deletedByGuid = deletedByGuid.map { v => java.util.UUID.fromString(v) }
      )
    }
  }
}

class ChangesDao @javax.inject.Inject() (override val db: play.api.db.Database) extends BaseChangesDao {
  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def randomPkey: java.util.UUID = {
    java.util.UUID.randomUUID
  }

  private val InsertQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | insert into public.changes
     | (guid, application_guid, from_version_guid, to_version_guid, type, description, changed_at, changed_by_guid, is_material, created_at, created_by_guid, updated_at)
     | values
     | ({guid}::uuid, {application_guid}::uuid, {from_version_guid}::uuid, {to_version_guid}::uuid, {type}, {description}, {changed_at}::timestamptz, {changed_by_guid}::uuid, {is_material}::boolean, {created_at}::timestamptz, {created_by_guid}::uuid, {updated_at}::timestamptz)
    """.stripMargin)
  }

  private val UpdateQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | update public.changes
     | set application_guid = {application_guid}::uuid,
     |     from_version_guid = {from_version_guid}::uuid,
     |     to_version_guid = {to_version_guid}::uuid,
     |     type = {type},
     |     description = {description},
     |     changed_at = {changed_at}::timestamptz,
     |     changed_by_guid = {changed_by_guid}::uuid,
     |     is_material = {is_material}::boolean,
     |     updated_at = {updated_at}::timestamptz
     | where guid = {guid}::uuid
    """.stripMargin)
  }

  private val DeleteQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("update public.changes set deleted_at = {deleted_at}::timestamptz, deleted_by_guid = {deleted_by_guid}::uuid").isNull("deleted_at")
  }

  def insert(
    user: java.util.UUID,
    form: ChangeForm
  ): java.util.UUID = {
    db.withConnection { c =>
      insert(c, user, form)
    }
  }

  def insert(
    c: java.sql.Connection,
    user: java.util.UUID,
    form: ChangeForm
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
    forms: Seq[ChangeForm]
  ): Seq[java.util.UUID] = {
    db.withConnection { c =>
      insertBatch(c, user, forms)
    }
  }

  def insertBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[ChangeForm]
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
    change: Change,
    form: ChangeForm
  ): Unit = {
    db.withConnection { c =>
      update(c, user, change, form)
    }
  }

  def update(
    c: java.sql.Connection,
    user: java.util.UUID,
    change: Change,
    form: ChangeForm
  ): Unit = {
    updateByGuid(
      c = c,
      user = user,
      guid = change.guid,
      form = form
    )
  }

  def updateByGuid(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: ChangeForm
  ): Unit = {
    db.withConnection { c =>
      updateByGuid(c, user, guid, form)
    }
  }

  def updateByGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    guid: java.util.UUID,
    form: ChangeForm
  ): Unit = {
    bindQuery(UpdateQuery, user, form)
      .bind("guid", guid)
      .execute(c)
    ()
  }

  def updateBatch(
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, ChangeForm)]
  ): Unit = {
    db.withConnection { c =>
      updateBatch(c, user, forms)
    }
  }

  def updateBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, ChangeForm)]
  ): Unit = {
    forms.map { case (guid, f) => toNamedParameter(user, guid, f) }.toList match {
      case Nil => // no-op
      case first :: rest => anorm.BatchSql(UpdateQuery.sql(), first, rest*).execute()(c)
    }
  }

  def delete(
    user: java.util.UUID,
    change: Change
  ): Unit = {
    db.withConnection { c =>
      delete(c, user, change)
    }
  }

  def delete(
    c: java.sql.Connection,
    user: java.util.UUID,
    change: Change
  ): Unit = {
    deleteByGuid(
      c = c,
      user = user,
      guid = change.guid
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

  def deleteAllByToVersionGuid(
    user: java.util.UUID,
    toVersionGuid: java.util.UUID
  ): Unit = {
    db.withConnection { c =>
      deleteAllByToVersionGuid(c, user, toVersionGuid)
    }
  }

  def deleteAllByToVersionGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    toVersionGuid: java.util.UUID
  ): Unit = {
    DeleteQuery.equals("to_version_guid", toVersionGuid)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByToVersionGuids(
    user: java.util.UUID,
    toVersionGuids: Seq[java.util.UUID]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByToVersionGuids(c, user, toVersionGuids)
    }
  }

  def deleteAllByToVersionGuids(
    c: java.sql.Connection,
    user: java.util.UUID,
    toVersionGuids: Seq[java.util.UUID]
  ): Unit = {
    DeleteQuery.in("to_version_guid", toVersionGuids)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  private def bindQuery(
    query: io.flow.postgresql.Query,
    user: java.util.UUID,
    form: ChangeForm
  ): io.flow.postgresql.Query = {
    query
      .bind("application_guid", form.applicationGuid.toString)
      .bind("from_version_guid", form.fromVersionGuid.toString)
      .bind("to_version_guid", form.toVersionGuid.toString)
      .bind("type", form.`type`)
      .bind("description", form.description)
      .bind("changed_at", form.changedAt)
      .bind("changed_by_guid", form.changedByGuid.toString)
      .bind("is_material", form.isMaterial)
      .bind("updated_at", org.joda.time.DateTime.now)
  }

  private def toNamedParameter(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: ChangeForm
  ): Seq[anorm.NamedParameter] = {
    Seq(
      anorm.NamedParameter("guid", guid.toString),
      anorm.NamedParameter("application_guid", form.applicationGuid.toString),
      anorm.NamedParameter("from_version_guid", form.fromVersionGuid.toString),
      anorm.NamedParameter("to_version_guid", form.toVersionGuid.toString),
      anorm.NamedParameter("type", form.`type`),
      anorm.NamedParameter("description", form.description),
      anorm.NamedParameter("changed_at", form.changedAt),
      anorm.NamedParameter("changed_by_guid", form.changedByGuid.toString),
      anorm.NamedParameter("is_material", form.isMaterial),
      anorm.NamedParameter("updated_at", org.joda.time.DateTime.now)
    )
  }
}