package db.generated

case class Task(
  id: String,
  `type`: String,
  typeId: String,
  organizationGuid: Option[java.util.UUID],
  numAttempts: Int,
  nextAttemptAt: org.joda.time.DateTime,
  errors: Option[Seq[String]],
  stacktrace: Option[String],
  data: play.api.libs.json.JsValue,
  createdAt: org.joda.time.DateTime,
  updatedAt: org.joda.time.DateTime,
  updatedByGuid: String
) {
  def form: TaskForm = {
    TaskForm(
      id = id,
      `type` = `type`,
      typeId = typeId,
      organizationGuid = organizationGuid,
      numAttempts = numAttempts,
      nextAttemptAt = nextAttemptAt,
      errors = errors,
      stacktrace = stacktrace,
      data = data,
    )
  }
}

case class TaskForm(
  id: String,
  `type`: String,
  typeId: String,
  organizationGuid: Option[java.util.UUID],
  numAttempts: Int,
  nextAttemptAt: org.joda.time.DateTime,
  errors: Option[Seq[String]],
  stacktrace: Option[String],
  data: play.api.libs.json.JsValue
)

case object TasksTable {
  val SchemaName: String = "public"

  val TableName: String = "tasks"

  val QualifiedName: String = "public.tasks"

  sealed trait Column {
    def name: String
  }

  object Columns {
    case object Id extends Column {
      override val name: String = "id"
    }

    case object Type extends Column {
      override val name: String = "type"
    }

    case object TypeId extends Column {
      override val name: String = "type_id"
    }

    case object OrganizationGuid extends Column {
      override val name: String = "organization_guid"
    }

    case object NumAttempts extends Column {
      override val name: String = "num_attempts"
    }

    case object NextAttemptAt extends Column {
      override val name: String = "next_attempt_at"
    }

    case object Errors extends Column {
      override val name: String = "errors"
    }

    case object Stacktrace extends Column {
      override val name: String = "stacktrace"
    }

    case object Data extends Column {
      override val name: String = "data"
    }

    case object CreatedAt extends Column {
      override val name: String = "created_at"
    }

    case object UpdatedAt extends Column {
      override val name: String = "updated_at"
    }

    case object UpdatedByGuid extends Column {
      override val name: String = "updated_by_guid"
    }

    case object HashCode extends Column {
      override val name: String = "hash_code"
    }

    val all: List[Column] = List(Id, Type, TypeId, OrganizationGuid, NumAttempts, NextAttemptAt, Errors, Stacktrace, Data, CreatedAt, UpdatedAt, UpdatedByGuid, HashCode)
  }
}

trait BaseTasksDao {
  import anorm.*

  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def db: play.api.db.Database

  private val BaseQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | select id,
     |        type,
     |        type_id,
     |        organization_guid,
     |        num_attempts,
     |        next_attempt_at,
     |        errors::text,
     |        stacktrace,
     |        data::text,
     |        created_at,
     |        updated_at,
     |        updated_by_guid,
     |        hash_code
     |   from public.tasks
     |""".stripMargin.stripTrailing
    )
  }

  def findAll(
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    typeId: Option[String] = None,
    typeIds: Option[Seq[String]] = None,
    typeIdAndType: Option[(String, String)] = None,
    typeIdsAndTypes: Option[Seq[(String, String)]] = None,
    numAttempts: Option[Int] = None,
    numAttemptses: Option[Seq[Int]] = None,
    numAttemptsAndNextAttemptAt: Option[(Int, org.joda.time.DateTime)] = None,
    numAttemptsesAndNextAttemptAts: Option[Seq[(Int, org.joda.time.DateTime)]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[Task] = {
    db.withConnection { c =>
      findAllWithConnection(c, id, ids, typeId, typeIds, typeIdAndType, typeIdsAndTypes, numAttempts, numAttemptses, numAttemptsAndNextAttemptAt, numAttemptsesAndNextAttemptAts, limit, offset, orderBy)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    typeId: Option[String] = None,
    typeIds: Option[Seq[String]] = None,
    typeIdAndType: Option[(String, String)] = None,
    typeIdsAndTypes: Option[Seq[(String, String)]] = None,
    numAttempts: Option[Int] = None,
    numAttemptses: Option[Seq[Int]] = None,
    numAttemptsAndNextAttemptAt: Option[(Int, org.joda.time.DateTime)] = None,
    numAttemptsesAndNextAttemptAts: Option[Seq[(Int, org.joda.time.DateTime)]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[Task] = {
    customQueryModifier(BaseQuery)
      .equals("tasks.id", id)
      .optionalIn("tasks.id", ids)
      .equals("tasks.type_id", typeId)
      .optionalIn("tasks.type_id", typeIds)
      .optionalIn2(("tasks.type_id", "tasks.type"), typeIdAndType.map(Seq(_)))
      .optionalIn2(("tasks.type_id", "tasks.type"), typeIdsAndTypes)
      .equals("tasks.num_attempts", numAttempts)
      .optionalIn("tasks.num_attempts", numAttemptses)
      .optionalIn2(("tasks.num_attempts", "tasks.next_attempt_at"), numAttemptsAndNextAttemptAt.map(Seq(_)))
      .optionalIn2(("tasks.num_attempts", "tasks.next_attempt_at"), numAttemptsesAndNextAttemptAts)
      .optionalLimit(limit)
      .offset(offset)
      .orderBy(orderBy.flatMap(_.sql))
      .as(parser.*)(c)
  }

  def iterateAll(
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    typeId: Option[String] = None,
    typeIds: Option[Seq[String]] = None,
    typeIdAndType: Option[(String, String)] = None,
    typeIdsAndTypes: Option[Seq[(String, String)]] = None,
    numAttempts: Option[Int] = None,
    numAttemptses: Option[Seq[Int]] = None,
    numAttemptsAndNextAttemptAt: Option[(Int, org.joda.time.DateTime)] = None,
    numAttemptsesAndNextAttemptAts: Option[Seq[(Int, org.joda.time.DateTime)]] = None,
    pageSize: Long = 1000
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Iterator[Task] = {
    assert(pageSize > 0, "pageSize must be > 0")

    def iterate(lastValue: Option[Task]): Iterator[Task] = {
      val page: Seq[Task] = db.withConnection { c =>
        customQueryModifier(BaseQuery)
          .equals("tasks.id", id)
          .optionalIn("tasks.id", ids)
          .equals("tasks.type_id", typeId)
          .optionalIn("tasks.type_id", typeIds)
          .optionalIn2(("tasks.type_id", "tasks.type"), typeIdAndType.map(Seq(_)))
          .optionalIn2(("tasks.type_id", "tasks.type"), typeIdsAndTypes)
          .equals("tasks.num_attempts", numAttempts)
          .optionalIn("tasks.num_attempts", numAttemptses)
          .optionalIn2(("tasks.num_attempts", "tasks.next_attempt_at"), numAttemptsAndNextAttemptAt.map(Seq(_)))
          .optionalIn2(("tasks.num_attempts", "tasks.next_attempt_at"), numAttemptsesAndNextAttemptAts)
          .greaterThan("tasks.id", lastValue.map(_.id))
          .orderBy("tasks.id")
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

  def findById(id: String): Option[Task] = {
    db.withConnection { c =>
      findByIdWithConnection(c, id)
    }
  }

  def findByIdWithConnection(
    c: java.sql.Connection,
    id: String
  ): Option[Task] = {
    findAllWithConnection(
      c = c,
      id = Some(id),
      limit = Some(1)
    ).headOption
  }

  def findAllByTypeId(typeId: String): Seq[Task] = {
    db.withConnection { c =>
      findAllByTypeIdWithConnection(c, typeId)
    }
  }

  def findAllByTypeIdWithConnection(
    c: java.sql.Connection,
    typeId: String
  ): Seq[Task] = {
    findAllWithConnection(
      c = c,
      typeId = Some(typeId),
      limit = None
    )
  }

  def findByTypeIdAndType(typeIdAndType: (String, String)): Option[Task] = {
    db.withConnection { c =>
      findByTypeIdAndTypeWithConnection(c, typeIdAndType)
    }
  }

  def findByTypeIdAndTypeWithConnection(
    c: java.sql.Connection,
    typeIdAndType: (String, String)
  ): Option[Task] = {
    findAllWithConnection(
      c = c,
      typeIdAndType = Some(typeIdAndType),
      limit = Some(1)
    ).headOption
  }

  def findAllByNumAttempts(numAttempts: Int): Seq[Task] = {
    db.withConnection { c =>
      findAllByNumAttemptsWithConnection(c, numAttempts)
    }
  }

  def findAllByNumAttemptsWithConnection(
    c: java.sql.Connection,
    numAttempts: Int
  ): Seq[Task] = {
    findAllWithConnection(
      c = c,
      numAttempts = Some(numAttempts),
      limit = None
    )
  }

  def findAllByNumAttemptsAndNextAttemptAt(numAttemptsAndNextAttemptAt: (Int, org.joda.time.DateTime)): Seq[Task] = {
    db.withConnection { c =>
      findAllByNumAttemptsAndNextAttemptAtWithConnection(c, numAttemptsAndNextAttemptAt)
    }
  }

  def findAllByNumAttemptsAndNextAttemptAtWithConnection(
    c: java.sql.Connection,
    numAttemptsAndNextAttemptAt: (Int, org.joda.time.DateTime)
  ): Seq[Task] = {
    findAllWithConnection(
      c = c,
      numAttemptsAndNextAttemptAt = Some(numAttemptsAndNextAttemptAt),
      limit = None
    )
  }

  private val parser: anorm.RowParser[Task] = {
    anorm.SqlParser.str("id") ~
      anorm.SqlParser.str("type") ~
      anorm.SqlParser.str("type_id") ~
      anorm.SqlParser.str("organization_guid").? ~
      anorm.SqlParser.int("num_attempts") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("next_attempt_at") ~
      anorm.SqlParser.str("errors").? ~
      anorm.SqlParser.str("stacktrace").? ~
      anorm.SqlParser.str("data") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("created_at") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("updated_at") ~
      anorm.SqlParser.str("updated_by_guid") ~
      anorm.SqlParser.long("hash_code") map { case id ~ type_ ~ typeId ~ organizationGuid ~ numAttempts ~ nextAttemptAt ~ errors ~ stacktrace ~ data ~ createdAt ~ updatedAt ~ updatedByGuid ~ hashCode =>
      Task(
        id = id,
        `type` = type_,
        typeId = typeId,
        organizationGuid = organizationGuid.map { v => java.util.UUID.fromString(v) },
        numAttempts = numAttempts,
        nextAttemptAt = nextAttemptAt,
        errors = errors.map { v => play.api.libs.json.Json.parse(v).asInstanceOf[play.api.libs.json.JsArray].value.toSeq.map(_.asInstanceOf[play.api.libs.json.JsString].value) },
        stacktrace = stacktrace,
        data = play.api.libs.json.Json.parse(data),
        createdAt = createdAt,
        updatedAt = updatedAt,
        updatedByGuid = updatedByGuid
      )
    }
  }
}

class TasksDao @javax.inject.Inject() (override val db: play.api.db.Database) extends BaseTasksDao {
  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  private val UpsertQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | insert into public.tasks
     | (id, type, type_id, organization_guid, num_attempts, next_attempt_at, errors, stacktrace, data, created_at, updated_at, updated_by_guid, hash_code)
     | values
     | ({id}, {type}, {type_id}, {organization_guid}::uuid, {num_attempts}::integer, {next_attempt_at}::timestamptz, {errors}::json, {stacktrace}, {data}::json, {created_at}::timestamptz, {updated_at}::timestamptz, {updated_by_guid}, {hash_code}::bigint)
     | on conflict(type_id, type) do update
     | set organization_guid = {organization_guid}::uuid,
     |     num_attempts = {num_attempts}::integer,
     |     next_attempt_at = {next_attempt_at}::timestamptz,
     |     errors = {errors}::json,
     |     stacktrace = {stacktrace},
     |     data = {data}::json,
     |     updated_at = {updated_at}::timestamptz,
     |     updated_by_guid = {updated_by_guid},
     |     hash_code = {hash_code}::bigint
     |  where tasks.hash_code != {hash_code}::bigint
    """.stripMargin)
  }

  private val UpdateQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | update public.tasks
     | set type = {type},
     |     type_id = {type_id},
     |     organization_guid = {organization_guid}::uuid,
     |     num_attempts = {num_attempts}::integer,
     |     next_attempt_at = {next_attempt_at}::timestamptz,
     |     errors = {errors}::json,
     |     stacktrace = {stacktrace},
     |     data = {data}::json,
     |     updated_at = {updated_at}::timestamptz,
     |     updated_by_guid = {updated_by_guid},
     |     hash_code = {hash_code}::bigint
     | where id = {id} and tasks.hash_code != {hash_code}::bigint
    """.stripMargin)
  }

  private val DeleteQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("delete from public.tasks")
  }

  def upsertByTypeIdAndType(
    user: java.util.UUID,
    form: TaskForm
  ): Unit = {
    db.withConnection { c =>
      upsertByTypeIdAndType(c, user, form)
    }
  }

  def upsertByTypeIdAndType(
    c: java.sql.Connection,
    user: java.util.UUID,
    form: TaskForm
  ): Unit = {
    bindQuery(UpsertQuery,user,  form)
      .bind("created_at", org.joda.time.DateTime.now)
      .execute(c)
  }

  def upsertBatchByTypeIdAndType(
    user: java.util.UUID,
    forms: Seq[TaskForm]
  ): Seq[Unit] = {
    db.withConnection { c =>
      upsertBatchByTypeIdAndType(c, user, forms)
    }
  }

  def upsertBatchByTypeIdAndType(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[TaskForm]
  ): Seq[Unit] = {
    forms.map { f => Seq(anorm.NamedParameter("created_at", org.joda.time.DateTime.now)) ++ toNamedParameter(user, f) }.toList match {
      case Nil => Nil
      case one :: rest => {
        anorm.BatchSql(UpsertQuery.sql(), one, rest*).execute()(c)
        (Seq(one) ++ rest).map { _ => () }
      }
    }
  }

  def update(
    user: java.util.UUID,
    task: Task,
    form: TaskForm
  ): Unit = {
    db.withConnection { c =>
      update(c, user, task, form)
    }
  }

  def update(
    c: java.sql.Connection,
    user: java.util.UUID,
    task: Task,
    form: TaskForm
  ): Unit = {
    updateById(
      c = c,
      user = user,
      id = task.id,
      form = form
    )
  }

  def updateById(
    user: java.util.UUID,
    id: String,
    form: TaskForm
  ): Unit = {
    db.withConnection { c =>
      updateById(c, user, id, form)
    }
  }

  def updateById(
    c: java.sql.Connection,
    user: java.util.UUID,
    id: String,
    form: TaskForm
  ): Unit = {
    bindQuery(UpdateQuery,user,  form)
      .bind("id", id)
      .bind("updated_by_guid", user)
      .execute(c)
    ()
  }

  def updateBatch(
    user: java.util.UUID,
    forms: Seq[TaskForm]
  ): Unit = {
    db.withConnection { c =>
      updateBatch(c, user, forms)
    }
  }

  def updateBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[TaskForm]
  ): Unit = {
    forms.map { f => toNamedParameter(user, f) }.toList match {
      case Nil => // no-op
      case first :: rest => anorm.BatchSql(UpdateQuery.sql(), first, rest*).execute()(c)
    }
  }

  def delete(
    user: java.util.UUID,
    task: Task
  ): Unit = {
    db.withConnection { c =>
      delete(c, user, task)
    }
  }

  def delete(
    c: java.sql.Connection,
    user: java.util.UUID,
    task: Task
  ): Unit = {
    deleteById(
      c = c,
      user = user,
      id = task.id
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
    DeleteQuery.equals("id", id).execute(c)
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
    DeleteQuery.in("id", ids).execute(c)
  }

  def deleteAllByTypeId(
    user: java.util.UUID,
    typeId: String
  ): Unit = {
    db.withConnection { c =>
      deleteAllByTypeId(c, user, typeId)
    }
  }

  def deleteAllByTypeId(
    c: java.sql.Connection,
    user: java.util.UUID,
    typeId: String
  ): Unit = {
    DeleteQuery.equals("type_id", typeId).execute(c)
  }

  def deleteAllByTypeIds(
    user: java.util.UUID,
    typeIds: Seq[String]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByTypeIds(c, user, typeIds)
    }
  }

  def deleteAllByTypeIds(
    c: java.sql.Connection,
    user: java.util.UUID,
    typeIds: Seq[String]
  ): Unit = {
    DeleteQuery.in("type_id", typeIds).execute(c)
  }

  def deleteByTypeIdAndType(
    user: java.util.UUID,
    typeIdAndType: (String, String)
  ): Unit = {
    db.withConnection { c =>
      deleteByTypeIdAndType(c, user, typeIdAndType)
    }
  }

  def deleteByTypeIdAndType(
    c: java.sql.Connection,
    user: java.util.UUID,
    typeIdAndType: (String, String)
  ): Unit = {
    DeleteQuery.in2(("type_id", "type"), Seq(typeIdAndType)).execute(c)
  }

  def deleteAllByTypeIdsAndTypes(
    user: java.util.UUID,
    typeIdsAndTypes: Seq[(String, String)]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByTypeIdsAndTypes(c, user, typeIdsAndTypes)
    }
  }

  def deleteAllByTypeIdsAndTypes(
    c: java.sql.Connection,
    user: java.util.UUID,
    typeIdsAndTypes: Seq[(String, String)]
  ): Unit = {
    DeleteQuery.in2(("type_id", "type"), typeIdsAndTypes).execute(c)
  }

  def deleteAllByNumAttempts(
    user: java.util.UUID,
    numAttempts: Int
  ): Unit = {
    db.withConnection { c =>
      deleteAllByNumAttempts(c, user, numAttempts)
    }
  }

  def deleteAllByNumAttempts(
    c: java.sql.Connection,
    user: java.util.UUID,
    numAttempts: Int
  ): Unit = {
    DeleteQuery.equals("num_attempts", numAttempts).execute(c)
  }

  def deleteAllByNumAttemptses(
    user: java.util.UUID,
    numAttemptses: Seq[Int]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByNumAttemptses(c, user, numAttemptses)
    }
  }

  def deleteAllByNumAttemptses(
    c: java.sql.Connection,
    user: java.util.UUID,
    numAttemptses: Seq[Int]
  ): Unit = {
    DeleteQuery.in("num_attempts", numAttemptses).execute(c)
  }

  def deleteAllByNumAttemptsAndNextAttemptAt(
    user: java.util.UUID,
    numAttemptsAndNextAttemptAt: (Int, org.joda.time.DateTime)
  ): Unit = {
    db.withConnection { c =>
      deleteAllByNumAttemptsAndNextAttemptAt(c, user, numAttemptsAndNextAttemptAt)
    }
  }

  def deleteAllByNumAttemptsAndNextAttemptAt(
    c: java.sql.Connection,
    user: java.util.UUID,
    numAttemptsAndNextAttemptAt: (Int, org.joda.time.DateTime)
  ): Unit = {
    DeleteQuery.in2(("num_attempts", "next_attempt_at"), Seq(numAttemptsAndNextAttemptAt)).execute(c)
  }

  def deleteAllByNumAttemptsesAndNextAttemptAts(
    user: java.util.UUID,
    numAttemptsesAndNextAttemptAts: Seq[(Int, org.joda.time.DateTime)]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByNumAttemptsesAndNextAttemptAts(c, user, numAttemptsesAndNextAttemptAts)
    }
  }

  def deleteAllByNumAttemptsesAndNextAttemptAts(
    c: java.sql.Connection,
    user: java.util.UUID,
    numAttemptsesAndNextAttemptAts: Seq[(Int, org.joda.time.DateTime)]
  ): Unit = {
    DeleteQuery.in2(("num_attempts", "next_attempt_at"), numAttemptsesAndNextAttemptAts).execute(c)
  }

  private def bindQuery(
    query: io.flow.postgresql.Query,
    user: java.util.UUID,
    form: TaskForm
  ): io.flow.postgresql.Query = {
    query
      .bind("id", form.id)
      .bind("type", form.`type`)
      .bind("type_id", form.typeId)
      .bind("organization_guid", form.organizationGuid.map(_.toString))
      .bind("num_attempts", form.numAttempts)
      .bind("next_attempt_at", form.nextAttemptAt)
      .bind("errors", form.errors.map { v => play.api.libs.json.Json.toJson(v).toString })
      .bind("stacktrace", form.stacktrace)
      .bind("data", play.api.libs.json.Json.toJson(form.data).toString)
      .bind("updated_at", org.joda.time.DateTime.now)
      .bind("updated_by_guid", user)
      .bind("hash_code", form.hashCode())
  }

  private def toNamedParameter(
    user: java.util.UUID,
    form: TaskForm
  ): Seq[anorm.NamedParameter] = {
    Seq(
      anorm.NamedParameter("id", form.id),
      anorm.NamedParameter("type", form.`type`),
      anorm.NamedParameter("type_id", form.typeId),
      anorm.NamedParameter("organization_guid", form.organizationGuid.map(_.toString)),
      anorm.NamedParameter("num_attempts", form.numAttempts),
      anorm.NamedParameter("next_attempt_at", form.nextAttemptAt),
      anorm.NamedParameter("errors", form.errors.map { v => play.api.libs.json.Json.toJson(v).toString }),
      anorm.NamedParameter("stacktrace", form.stacktrace),
      anorm.NamedParameter("data", play.api.libs.json.Json.toJson(form.data).toString),
      anorm.NamedParameter("updated_at", org.joda.time.DateTime.now),
      anorm.NamedParameter("updated_by_guid", user),
      anorm.NamedParameter("hash_code", form.hashCode())
    )
  }
}