package db.generated

case class ApplicationMove(
  guid: java.util.UUID,
  applicationGuid: java.util.UUID,
  fromOrganizationGuid: java.util.UUID,
  toOrganizationGuid: java.util.UUID,
  createdAt: org.joda.time.DateTime,
  createdByGuid: java.util.UUID,
  updatedAt: org.joda.time.DateTime,
  deletedAt: Option[org.joda.time.DateTime],
  deletedByGuid: Option[java.util.UUID]
) {
  def form: ApplicationMoveForm = {
    ApplicationMoveForm(
      applicationGuid = applicationGuid,
      fromOrganizationGuid = fromOrganizationGuid,
      toOrganizationGuid = toOrganizationGuid,
    )
  }
}

case class ApplicationMoveForm(
  applicationGuid: java.util.UUID,
  fromOrganizationGuid: java.util.UUID,
  toOrganizationGuid: java.util.UUID
)

case object ApplicationMovesTable {
  val SchemaName: String = "public"

  val TableName: String = "application_moves"

  val QualifiedName: String = "public.application_moves"

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

    case object FromOrganizationGuid extends Column {
      override val name: String = "from_organization_guid"
    }

    case object ToOrganizationGuid extends Column {
      override val name: String = "to_organization_guid"
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

    val all: List[Column] = List(Guid, ApplicationGuid, FromOrganizationGuid, ToOrganizationGuid, CreatedAt, CreatedByGuid, UpdatedAt, DeletedAt, DeletedByGuid)
  }
}

trait BaseApplicationMovesDao {
  import anorm.*

  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def db: play.api.db.Database

  private val BaseQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | select guid::text,
     |        application_guid::text,
     |        from_organization_guid::text,
     |        to_organization_guid::text,
     |        created_at,
     |        created_by_guid::text,
     |        updated_at,
     |        deleted_at,
     |        deleted_by_guid::text
     |   from public.application_moves
     |""".stripMargin.stripTrailing
    )
  }

  def findAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    applicationGuid: Option[java.util.UUID] = None,
    applicationGuids: Option[Seq[java.util.UUID]] = None,
    fromOrganizationGuid: Option[java.util.UUID] = None,
    fromOrganizationGuids: Option[Seq[java.util.UUID]] = None,
    toOrganizationGuid: Option[java.util.UUID] = None,
    toOrganizationGuids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[ApplicationMove] = {
    db.withConnection { c =>
      findAllWithConnection(c, guid, guids, applicationGuid, applicationGuids, fromOrganizationGuid, fromOrganizationGuids, toOrganizationGuid, toOrganizationGuids, limit, offset, orderBy)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    applicationGuid: Option[java.util.UUID] = None,
    applicationGuids: Option[Seq[java.util.UUID]] = None,
    fromOrganizationGuid: Option[java.util.UUID] = None,
    fromOrganizationGuids: Option[Seq[java.util.UUID]] = None,
    toOrganizationGuid: Option[java.util.UUID] = None,
    toOrganizationGuids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[ApplicationMove] = {
    customQueryModifier(BaseQuery)
      .equals("application_moves.guid", guid)
      .optionalIn("application_moves.guid", guids)
      .equals("application_moves.application_guid", applicationGuid)
      .optionalIn("application_moves.application_guid", applicationGuids)
      .equals("application_moves.from_organization_guid", fromOrganizationGuid)
      .optionalIn("application_moves.from_organization_guid", fromOrganizationGuids)
      .equals("application_moves.to_organization_guid", toOrganizationGuid)
      .optionalIn("application_moves.to_organization_guid", toOrganizationGuids)
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
    fromOrganizationGuid: Option[java.util.UUID] = None,
    fromOrganizationGuids: Option[Seq[java.util.UUID]] = None,
    toOrganizationGuid: Option[java.util.UUID] = None,
    toOrganizationGuids: Option[Seq[java.util.UUID]] = None,
    pageSize: Long = 1000
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Iterator[ApplicationMove] = {
    assert(pageSize > 0, "pageSize must be > 0")

    def iterate(lastValue: Option[ApplicationMove]): Iterator[ApplicationMove] = {
      val page: Seq[ApplicationMove] = db.withConnection { c =>
        customQueryModifier(BaseQuery)
          .equals("application_moves.guid", guid)
          .optionalIn("application_moves.guid", guids)
          .equals("application_moves.application_guid", applicationGuid)
          .optionalIn("application_moves.application_guid", applicationGuids)
          .equals("application_moves.from_organization_guid", fromOrganizationGuid)
          .optionalIn("application_moves.from_organization_guid", fromOrganizationGuids)
          .equals("application_moves.to_organization_guid", toOrganizationGuid)
          .optionalIn("application_moves.to_organization_guid", toOrganizationGuids)
          .greaterThan("application_moves.guid", lastValue.map(_.guid))
          .orderBy("application_moves.guid")
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

  def findByGuid(guid: java.util.UUID): Option[ApplicationMove] = {
    db.withConnection { c =>
      findByGuidWithConnection(c, guid)
    }
  }

  def findByGuidWithConnection(
    c: java.sql.Connection,
    guid: java.util.UUID
  ): Option[ApplicationMove] = {
    findAllWithConnection(
      c = c,
      guid = Some(guid),
      limit = Some(1)
    ).headOption
  }

  def findAllByApplicationGuid(applicationGuid: java.util.UUID): Seq[ApplicationMove] = {
    db.withConnection { c =>
      findAllByApplicationGuidWithConnection(c, applicationGuid)
    }
  }

  def findAllByApplicationGuidWithConnection(
    c: java.sql.Connection,
    applicationGuid: java.util.UUID
  ): Seq[ApplicationMove] = {
    findAllWithConnection(
      c = c,
      applicationGuid = Some(applicationGuid),
      limit = None
    )
  }

  def findAllByFromOrganizationGuid(fromOrganizationGuid: java.util.UUID): Seq[ApplicationMove] = {
    db.withConnection { c =>
      findAllByFromOrganizationGuidWithConnection(c, fromOrganizationGuid)
    }
  }

  def findAllByFromOrganizationGuidWithConnection(
    c: java.sql.Connection,
    fromOrganizationGuid: java.util.UUID
  ): Seq[ApplicationMove] = {
    findAllWithConnection(
      c = c,
      fromOrganizationGuid = Some(fromOrganizationGuid),
      limit = None
    )
  }

  def findAllByToOrganizationGuid(toOrganizationGuid: java.util.UUID): Seq[ApplicationMove] = {
    db.withConnection { c =>
      findAllByToOrganizationGuidWithConnection(c, toOrganizationGuid)
    }
  }

  def findAllByToOrganizationGuidWithConnection(
    c: java.sql.Connection,
    toOrganizationGuid: java.util.UUID
  ): Seq[ApplicationMove] = {
    findAllWithConnection(
      c = c,
      toOrganizationGuid = Some(toOrganizationGuid),
      limit = None
    )
  }

  private val parser: anorm.RowParser[ApplicationMove] = {
    anorm.SqlParser.str("guid") ~
      anorm.SqlParser.str("application_guid") ~
      anorm.SqlParser.str("from_organization_guid") ~
      anorm.SqlParser.str("to_organization_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("created_at") ~
      anorm.SqlParser.str("created_by_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("updated_at") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("deleted_at").? ~
      anorm.SqlParser.str("deleted_by_guid").? map { case guid ~ applicationGuid ~ fromOrganizationGuid ~ toOrganizationGuid ~ createdAt ~ createdByGuid ~ updatedAt ~ deletedAt ~ deletedByGuid =>
      ApplicationMove(
        guid = java.util.UUID.fromString(guid),
        applicationGuid = java.util.UUID.fromString(applicationGuid),
        fromOrganizationGuid = java.util.UUID.fromString(fromOrganizationGuid),
        toOrganizationGuid = java.util.UUID.fromString(toOrganizationGuid),
        createdAt = createdAt,
        createdByGuid = java.util.UUID.fromString(createdByGuid),
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        deletedByGuid = deletedByGuid.map { v => java.util.UUID.fromString(v) }
      )
    }
  }
}

class ApplicationMovesDao @javax.inject.Inject() (override val db: play.api.db.Database) extends BaseApplicationMovesDao {
  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def randomPkey: java.util.UUID = {
    java.util.UUID.randomUUID
  }

  private val InsertQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | insert into public.application_moves
     | (guid, application_guid, from_organization_guid, to_organization_guid, created_at, created_by_guid, updated_at)
     | values
     | ({guid}::uuid, {application_guid}::uuid, {from_organization_guid}::uuid, {to_organization_guid}::uuid, {created_at}::timestamptz, {created_by_guid}::uuid, {updated_at}::timestamptz)
    """.stripMargin)
  }

  private val UpdateQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | update public.application_moves
     | set application_guid = {application_guid}::uuid,
     |     from_organization_guid = {from_organization_guid}::uuid,
     |     to_organization_guid = {to_organization_guid}::uuid,
     |     updated_at = {updated_at}::timestamptz
     | where guid = {guid}::uuid
    """.stripMargin)
  }

  private val DeleteQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("update public.application_moves set deleted_at = {deleted_at}::timestamptz, deleted_by_guid = {deleted_by_guid}::uuid").isNull("deleted_at")
  }

  def insert(
    user: java.util.UUID,
    form: ApplicationMoveForm
  ): java.util.UUID = {
    db.withConnection { c =>
      insert(c, user, form)
    }
  }

  def insert(
    c: java.sql.Connection,
    user: java.util.UUID,
    form: ApplicationMoveForm
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
    forms: Seq[ApplicationMoveForm]
  ): Seq[java.util.UUID] = {
    db.withConnection { c =>
      insertBatch(c, user, forms)
    }
  }

  def insertBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[ApplicationMoveForm]
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
    applicationMove: ApplicationMove,
    form: ApplicationMoveForm
  ): Unit = {
    db.withConnection { c =>
      update(c, user, applicationMove, form)
    }
  }

  def update(
    c: java.sql.Connection,
    user: java.util.UUID,
    applicationMove: ApplicationMove,
    form: ApplicationMoveForm
  ): Unit = {
    updateByGuid(
      c = c,
      user = user,
      guid = applicationMove.guid,
      form = form
    )
  }

  def updateByGuid(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: ApplicationMoveForm
  ): Unit = {
    db.withConnection { c =>
      updateByGuid(c, user, guid, form)
    }
  }

  def updateByGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    guid: java.util.UUID,
    form: ApplicationMoveForm
  ): Unit = {
    bindQuery(UpdateQuery, user, form)
      .bind("guid", guid)
      .execute(c)
    ()
  }

  def updateBatch(
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, ApplicationMoveForm)]
  ): Unit = {
    db.withConnection { c =>
      updateBatch(c, user, forms)
    }
  }

  def updateBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, ApplicationMoveForm)]
  ): Unit = {
    forms.map { case (guid, f) => toNamedParameter(user, guid, f) }.toList match {
      case Nil => // no-op
      case first :: rest => anorm.BatchSql(UpdateQuery.sql(), first, rest*).execute()(c)
    }
  }

  def delete(
    user: java.util.UUID,
    applicationMove: ApplicationMove
  ): Unit = {
    db.withConnection { c =>
      delete(c, user, applicationMove)
    }
  }

  def delete(
    c: java.sql.Connection,
    user: java.util.UUID,
    applicationMove: ApplicationMove
  ): Unit = {
    deleteByGuid(
      c = c,
      user = user,
      guid = applicationMove.guid
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

  def deleteAllByFromOrganizationGuid(
    user: java.util.UUID,
    fromOrganizationGuid: java.util.UUID
  ): Unit = {
    db.withConnection { c =>
      deleteAllByFromOrganizationGuid(c, user, fromOrganizationGuid)
    }
  }

  def deleteAllByFromOrganizationGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    fromOrganizationGuid: java.util.UUID
  ): Unit = {
    DeleteQuery.equals("from_organization_guid", fromOrganizationGuid)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByFromOrganizationGuids(
    user: java.util.UUID,
    fromOrganizationGuids: Seq[java.util.UUID]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByFromOrganizationGuids(c, user, fromOrganizationGuids)
    }
  }

  def deleteAllByFromOrganizationGuids(
    c: java.sql.Connection,
    user: java.util.UUID,
    fromOrganizationGuids: Seq[java.util.UUID]
  ): Unit = {
    DeleteQuery.in("from_organization_guid", fromOrganizationGuids)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByToOrganizationGuid(
    user: java.util.UUID,
    toOrganizationGuid: java.util.UUID
  ): Unit = {
    db.withConnection { c =>
      deleteAllByToOrganizationGuid(c, user, toOrganizationGuid)
    }
  }

  def deleteAllByToOrganizationGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    toOrganizationGuid: java.util.UUID
  ): Unit = {
    DeleteQuery.equals("to_organization_guid", toOrganizationGuid)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByToOrganizationGuids(
    user: java.util.UUID,
    toOrganizationGuids: Seq[java.util.UUID]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByToOrganizationGuids(c, user, toOrganizationGuids)
    }
  }

  def deleteAllByToOrganizationGuids(
    c: java.sql.Connection,
    user: java.util.UUID,
    toOrganizationGuids: Seq[java.util.UUID]
  ): Unit = {
    DeleteQuery.in("to_organization_guid", toOrganizationGuids)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  private def bindQuery(
    query: io.flow.postgresql.Query,
    user: java.util.UUID,
    form: ApplicationMoveForm
  ): io.flow.postgresql.Query = {
    query
      .bind("application_guid", form.applicationGuid.toString)
      .bind("from_organization_guid", form.fromOrganizationGuid.toString)
      .bind("to_organization_guid", form.toOrganizationGuid.toString)
      .bind("updated_at", org.joda.time.DateTime.now)
  }

  private def toNamedParameter(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: ApplicationMoveForm
  ): Seq[anorm.NamedParameter] = {
    Seq(
      anorm.NamedParameter("guid", guid.toString),
      anorm.NamedParameter("application_guid", form.applicationGuid.toString),
      anorm.NamedParameter("from_organization_guid", form.fromOrganizationGuid.toString),
      anorm.NamedParameter("to_organization_guid", form.toOrganizationGuid.toString),
      anorm.NamedParameter("updated_at", org.joda.time.DateTime.now)
    )
  }
}