package db.generated

case class EmailVerificationConfirmation(
  guid: java.util.UUID,
  emailVerificationGuid: java.util.UUID,
  createdAt: org.joda.time.DateTime,
  createdByGuid: java.util.UUID,
  updatedAt: org.joda.time.DateTime,
  deletedAt: Option[org.joda.time.DateTime],
  deletedByGuid: Option[java.util.UUID]
) {
  def form: EmailVerificationConfirmationForm = {
    EmailVerificationConfirmationForm(
      emailVerificationGuid = emailVerificationGuid,
    )
  }
}

case class EmailVerificationConfirmationForm(emailVerificationGuid: java.util.UUID)

case object EmailVerificationConfirmationsTable {
  val SchemaName: String = "public"

  val TableName: String = "email_verification_confirmations"

  val QualifiedName: String = "public.email_verification_confirmations"

  sealed trait Column {
    def name: String
  }

  object Columns {
    case object Guid extends Column {
      override val name: String = "guid"
    }

    case object EmailVerificationGuid extends Column {
      override val name: String = "email_verification_guid"
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

    val all: List[Column] = List(Guid, EmailVerificationGuid, CreatedAt, CreatedByGuid, UpdatedAt, DeletedAt, DeletedByGuid)
  }
}

trait BaseEmailVerificationConfirmationsDao {
  import anorm.*

  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def db: play.api.db.Database

  private val BaseQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | select guid::text,
     |        email_verification_guid::text,
     |        created_at,
     |        created_by_guid::text,
     |        updated_at,
     |        deleted_at,
     |        deleted_by_guid::text
     |   from public.email_verification_confirmations
     |""".stripMargin.stripTrailing
    )
  }

  def findAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    emailVerificationGuid: Option[java.util.UUID] = None,
    emailVerificationGuids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[EmailVerificationConfirmation] = {
    db.withConnection { c =>
      findAllWithConnection(c, guid, guids, emailVerificationGuid, emailVerificationGuids, limit, offset, orderBy)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    emailVerificationGuid: Option[java.util.UUID] = None,
    emailVerificationGuids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[EmailVerificationConfirmation] = {
    customQueryModifier(BaseQuery)
      .equals("email_verification_confirmations.guid", guid)
      .optionalIn("email_verification_confirmations.guid", guids)
      .equals("email_verification_confirmations.email_verification_guid", emailVerificationGuid)
      .optionalIn("email_verification_confirmations.email_verification_guid", emailVerificationGuids)
      .optionalLimit(limit)
      .offset(offset)
      .orderBy(orderBy.flatMap(_.sql))
      .as(parser.*)(c)
  }

  def iterateAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    emailVerificationGuid: Option[java.util.UUID] = None,
    emailVerificationGuids: Option[Seq[java.util.UUID]] = None,
    pageSize: Long = 1000
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Iterator[EmailVerificationConfirmation] = {
    assert(pageSize > 0, "pageSize must be > 0")

    def iterate(lastValue: Option[EmailVerificationConfirmation]): Iterator[EmailVerificationConfirmation] = {
      val page: Seq[EmailVerificationConfirmation] = db.withConnection { c =>
        customQueryModifier(BaseQuery)
          .equals("email_verification_confirmations.guid", guid)
          .optionalIn("email_verification_confirmations.guid", guids)
          .equals("email_verification_confirmations.email_verification_guid", emailVerificationGuid)
          .optionalIn("email_verification_confirmations.email_verification_guid", emailVerificationGuids)
          .greaterThan("email_verification_confirmations.guid", lastValue.map(_.guid))
          .orderBy("email_verification_confirmations.guid")
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

  def findByGuid(guid: java.util.UUID): Option[EmailVerificationConfirmation] = {
    db.withConnection { c =>
      findByGuidWithConnection(c, guid)
    }
  }

  def findByGuidWithConnection(
    c: java.sql.Connection,
    guid: java.util.UUID
  ): Option[EmailVerificationConfirmation] = {
    findAllWithConnection(
      c = c,
      guid = Some(guid),
      limit = Some(1)
    ).headOption
  }

  def findByEmailVerificationGuid(emailVerificationGuid: java.util.UUID): Option[EmailVerificationConfirmation] = {
    db.withConnection { c =>
      findByEmailVerificationGuidWithConnection(c, emailVerificationGuid)
    }
  }

  def findByEmailVerificationGuidWithConnection(
    c: java.sql.Connection,
    emailVerificationGuid: java.util.UUID
  ): Option[EmailVerificationConfirmation] = {
    findAllWithConnection(
      c = c,
      emailVerificationGuid = Some(emailVerificationGuid),
      limit = Some(1)
    ).headOption
  }

  private val parser: anorm.RowParser[EmailVerificationConfirmation] = {
    anorm.SqlParser.str("guid") ~
      anorm.SqlParser.str("email_verification_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("created_at") ~
      anorm.SqlParser.str("created_by_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("updated_at") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("deleted_at").? ~
      anorm.SqlParser.str("deleted_by_guid").? map { case guid ~ emailVerificationGuid ~ createdAt ~ createdByGuid ~ updatedAt ~ deletedAt ~ deletedByGuid =>
      EmailVerificationConfirmation(
        guid = java.util.UUID.fromString(guid),
        emailVerificationGuid = java.util.UUID.fromString(emailVerificationGuid),
        createdAt = createdAt,
        createdByGuid = java.util.UUID.fromString(createdByGuid),
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        deletedByGuid = deletedByGuid.map { v => java.util.UUID.fromString(v) }
      )
    }
  }
}

class EmailVerificationConfirmationsDao @javax.inject.Inject() (override val db: play.api.db.Database) extends BaseEmailVerificationConfirmationsDao {
  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def randomPkey: java.util.UUID = {
    java.util.UUID.randomUUID
  }

  private val InsertQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | insert into public.email_verification_confirmations
     | (guid, email_verification_guid, created_at, created_by_guid, updated_at)
     | values
     | ({guid}::uuid, {email_verification_guid}::uuid, {created_at}::timestamptz, {created_by_guid}::uuid, {updated_at}::timestamptz)
    """.stripMargin)
  }

  private val UpdateQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | update public.email_verification_confirmations
     | set email_verification_guid = {email_verification_guid}::uuid,
     |     updated_at = {updated_at}::timestamptz
     | where guid = {guid}::uuid
    """.stripMargin)
  }

  private val DeleteQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("update public.email_verification_confirmations set deleted_at = {deleted_at}::timestamptz, deleted_by_guid = {deleted_by_guid}::uuid").isNull("deleted_at")
  }

  def insert(
    user: java.util.UUID,
    form: EmailVerificationConfirmationForm
  ): java.util.UUID = {
    db.withConnection { c =>
      insert(c, user, form)
    }
  }

  def insert(
    c: java.sql.Connection,
    user: java.util.UUID,
    form: EmailVerificationConfirmationForm
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
    forms: Seq[EmailVerificationConfirmationForm]
  ): Seq[java.util.UUID] = {
    db.withConnection { c =>
      insertBatch(c, user, forms)
    }
  }

  def insertBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[EmailVerificationConfirmationForm]
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
    emailVerificationConfirmation: EmailVerificationConfirmation,
    form: EmailVerificationConfirmationForm
  ): Unit = {
    db.withConnection { c =>
      update(c, user, emailVerificationConfirmation, form)
    }
  }

  def update(
    c: java.sql.Connection,
    user: java.util.UUID,
    emailVerificationConfirmation: EmailVerificationConfirmation,
    form: EmailVerificationConfirmationForm
  ): Unit = {
    updateByGuid(
      c = c,
      user = user,
      guid = emailVerificationConfirmation.guid,
      form = form
    )
  }

  def updateByGuid(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: EmailVerificationConfirmationForm
  ): Unit = {
    db.withConnection { c =>
      updateByGuid(c, user, guid, form)
    }
  }

  def updateByGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    guid: java.util.UUID,
    form: EmailVerificationConfirmationForm
  ): Unit = {
    bindQuery(UpdateQuery, user, form)
      .bind("guid", guid)
      .execute(c)
    ()
  }

  def updateBatch(
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, EmailVerificationConfirmationForm)]
  ): Unit = {
    db.withConnection { c =>
      updateBatch(c, user, forms)
    }
  }

  def updateBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, EmailVerificationConfirmationForm)]
  ): Unit = {
    forms.map { case (guid, f) => toNamedParameter(user, guid, f) }.toList match {
      case Nil => // no-op
      case first :: rest => anorm.BatchSql(UpdateQuery.sql(), first, rest*).execute()(c)
    }
  }

  def delete(
    user: java.util.UUID,
    emailVerificationConfirmation: EmailVerificationConfirmation
  ): Unit = {
    db.withConnection { c =>
      delete(c, user, emailVerificationConfirmation)
    }
  }

  def delete(
    c: java.sql.Connection,
    user: java.util.UUID,
    emailVerificationConfirmation: EmailVerificationConfirmation
  ): Unit = {
    deleteByGuid(
      c = c,
      user = user,
      guid = emailVerificationConfirmation.guid
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

  def deleteByEmailVerificationGuid(
    user: java.util.UUID,
    emailVerificationGuid: java.util.UUID
  ): Unit = {
    db.withConnection { c =>
      deleteByEmailVerificationGuid(c, user, emailVerificationGuid)
    }
  }

  def deleteByEmailVerificationGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    emailVerificationGuid: java.util.UUID
  ): Unit = {
    DeleteQuery.equals("email_verification_guid", emailVerificationGuid)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByEmailVerificationGuids(
    user: java.util.UUID,
    emailVerificationGuids: Seq[java.util.UUID]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByEmailVerificationGuids(c, user, emailVerificationGuids)
    }
  }

  def deleteAllByEmailVerificationGuids(
    c: java.sql.Connection,
    user: java.util.UUID,
    emailVerificationGuids: Seq[java.util.UUID]
  ): Unit = {
    DeleteQuery.in("email_verification_guid", emailVerificationGuids)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  private def bindQuery(
    query: io.flow.postgresql.Query,
    user: java.util.UUID,
    form: EmailVerificationConfirmationForm
  ): io.flow.postgresql.Query = {
    query
      .bind("email_verification_guid", form.emailVerificationGuid.toString)
      .bind("updated_at", org.joda.time.DateTime.now)
  }

  private def toNamedParameter(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: EmailVerificationConfirmationForm
  ): Seq[anorm.NamedParameter] = {
    Seq(
      anorm.NamedParameter("guid", guid.toString),
      anorm.NamedParameter("email_verification_guid", form.emailVerificationGuid.toString),
      anorm.NamedParameter("updated_at", org.joda.time.DateTime.now)
    )
  }
}