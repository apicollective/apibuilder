package db.generated

import anorm.JodaParameterMetaData._
import anorm._
import io.flow.postgresql.{OrderBy, Query}
import java.sql.Connection
import java.util.UUID
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.db.Database
import play.api.libs.json.{JsValue, Json}

case class Task(
  id: String,
  `type`: String,
  typeId: String,
  organizationGuid: Option[UUID],
  numAttempts: Int,
  nextAttemptAt: DateTime,
  errors: Option[Seq[String]],
  stacktrace: Option[String],
  data: JsValue,
  updatedByGuid: String,
  createdAt: DateTime,
  updatedAt: DateTime
) {

  lazy val form: TaskForm = TaskForm(
    id = id,
    `type` = `type`,
    typeId = typeId,
    organizationGuid = organizationGuid,
    numAttempts = numAttempts,
    nextAttemptAt = nextAttemptAt,
    errors = errors,
    stacktrace = stacktrace,
    data = data
  )

}

case class TaskForm(
  id: String,
  `type`: String,
  typeId: String,
  organizationGuid: Option[UUID],
  numAttempts: Int,
  nextAttemptAt: DateTime,
  errors: Option[Seq[String]],
  stacktrace: Option[String],
  data: JsValue
)

object TasksTable {
  val Schema: String = "public"
  val Name: String = "tasks"
  val QualifiedName: String = s"$Schema.$Name"

  object Columns {
    val Id: String = "id"
    val Type: String = "type"
    val TypeId: String = "type_id"
    val OrganizationGuid: String = "organization_guid"
    val NumAttempts: String = "num_attempts"
    val NextAttemptAt: String = "next_attempt_at"
    val Errors: String = "errors"
    val Stacktrace: String = "stacktrace"
    val Data: String = "data"
    val UpdatedByGuid: String = "updated_by_guid"
    val CreatedAt: String = "created_at"
    val UpdatedAt: String = "updated_at"
    val HashCode: String = "hash_code"
    val all: List[String] = List(Id, Type, TypeId, OrganizationGuid, NumAttempts, NextAttemptAt, Errors, Stacktrace, Data, UpdatedByGuid, CreatedAt, UpdatedAt, HashCode)
  }
}

trait BaseTasksDao {

  def db: Database

  private[this] val BaseQuery = Query("""
      | select tasks.id,
      |        tasks.type,
      |        tasks.type_id,
      |        tasks.organization_guid,
      |        tasks.num_attempts,
      |        tasks.next_attempt_at,
      |        tasks.errors::text as errors_text,
      |        tasks.stacktrace,
      |        tasks.data::text as data_text,
      |        tasks.updated_by_guid,
      |        tasks.created_at,
      |        tasks.updated_at,
      |        tasks.hash_code
      |   from tasks
  """.stripMargin)

  def findById(id: String): Option[Task] = {
    db.withConnection { c =>
      findByIdWithConnection(c, id)
    }
  }

  def findByIdWithConnection(c: java.sql.Connection, id: String): Option[Task] = {
    findAllWithConnection(c, ids = Some(Seq(id)), limit = Some(1L), orderBy = None).headOption
  }

  def findByTypeIdAndType(typeId: String, `type`: String): Option[Task] = {
    db.withConnection { c =>
      findByTypeIdAndTypeWithConnection(c, typeId, `type`)
    }
  }

  def findByTypeIdAndTypeWithConnection(c: java.sql.Connection, typeId: String, `type`: String): Option[Task] = {
    findAllWithConnection(c, typeId = Some(typeId), `type` = Some(`type`), limit = Some(1L), orderBy = None).headOption
  }

  def iterateAll(
    ids: Option[Seq[String]] = None,
    typeId: Option[String] = None,
    typeIds: Option[Seq[String]] = None,
    `type`: Option[String] = None,
    types: Option[Seq[String]] = None,
    numAttempts: Option[Int] = None,
    numAttemptsGreaterThanOrEquals: Option[Int] = None,
    numAttemptsGreaterThan: Option[Int] = None,
    numAttemptsLessThanOrEquals: Option[Int] = None,
    numAttemptsLessThan: Option[Int] = None,
    nextAttemptAt: Option[DateTime] = None,
    nextAttemptAtGreaterThanOrEquals: Option[DateTime] = None,
    nextAttemptAtGreaterThan: Option[DateTime] = None,
    nextAttemptAtLessThanOrEquals: Option[DateTime] = None,
    nextAttemptAtLessThan: Option[DateTime] = None,
    numAttemptsNextAttemptAts: Option[Seq[(Int, DateTime)]] = None,
    typeIdTypes: Option[Seq[(String, String)]] = None,
    pageSize: Long = 2000L,
  ) (
    implicit customQueryModifier: Query => Query = { q => q }
  ): Iterator[Task] = {
    def iterate(lastValue: Option[Task]): Iterator[Task] = {
      val page = findAll(
        ids = ids,
        typeId = typeId,
        typeIds = typeIds,
        `type` = `type`,
        types = types,
        numAttempts = numAttempts,
        numAttemptsGreaterThanOrEquals = numAttemptsGreaterThanOrEquals,
        numAttemptsGreaterThan = numAttemptsGreaterThan,
        numAttemptsLessThanOrEquals = numAttemptsLessThanOrEquals,
        numAttemptsLessThan = numAttemptsLessThan,
        nextAttemptAt = nextAttemptAt,
        nextAttemptAtGreaterThanOrEquals = nextAttemptAtGreaterThanOrEquals,
        nextAttemptAtGreaterThan = nextAttemptAtGreaterThan,
        nextAttemptAtLessThanOrEquals = nextAttemptAtLessThanOrEquals,
        nextAttemptAtLessThan = nextAttemptAtLessThan,
        numAttemptsNextAttemptAts = numAttemptsNextAttemptAts,
        typeIdTypes = typeIdTypes,
        limit = Some(pageSize),
        orderBy = Some(OrderBy("tasks.id")),
      ) { q => customQueryModifier(q).greaterThan("tasks.id", lastValue.map(_.id)) }

      page.lastOption match {
        case None => Iterator.empty
        case lastValue => page.iterator ++ iterate(lastValue)
      }
    }

    iterate(None)
  }

  def findAll(
    ids: Option[Seq[String]] = None,
    typeId: Option[String] = None,
    typeIds: Option[Seq[String]] = None,
    `type`: Option[String] = None,
    types: Option[Seq[String]] = None,
    numAttempts: Option[Int] = None,
    numAttemptsGreaterThanOrEquals: Option[Int] = None,
    numAttemptsGreaterThan: Option[Int] = None,
    numAttemptsLessThanOrEquals: Option[Int] = None,
    numAttemptsLessThan: Option[Int] = None,
    nextAttemptAt: Option[DateTime] = None,
    nextAttemptAtGreaterThanOrEquals: Option[DateTime] = None,
    nextAttemptAtGreaterThan: Option[DateTime] = None,
    nextAttemptAtLessThanOrEquals: Option[DateTime] = None,
    nextAttemptAtLessThan: Option[DateTime] = None,
    numAttemptsNextAttemptAts: Option[Seq[(Int, DateTime)]] = None,
    typeIdTypes: Option[Seq[(String, String)]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[OrderBy] = Some(OrderBy("tasks.id"))
  ) (
    implicit customQueryModifier: Query => Query = { q => q }
  ): Seq[Task] = {
    db.withConnection { c =>
      findAllWithConnection(
        c,
        ids = ids,
        typeId = typeId,
        typeIds = typeIds,
        `type` = `type`,
        types = types,
        numAttempts = numAttempts,
        numAttemptsGreaterThanOrEquals = numAttemptsGreaterThanOrEquals,
        numAttemptsGreaterThan = numAttemptsGreaterThan,
        numAttemptsLessThanOrEquals = numAttemptsLessThanOrEquals,
        numAttemptsLessThan = numAttemptsLessThan,
        nextAttemptAt = nextAttemptAt,
        nextAttemptAtGreaterThanOrEquals = nextAttemptAtGreaterThanOrEquals,
        nextAttemptAtGreaterThan = nextAttemptAtGreaterThan,
        nextAttemptAtLessThanOrEquals = nextAttemptAtLessThanOrEquals,
        nextAttemptAtLessThan = nextAttemptAtLessThan,
        numAttemptsNextAttemptAts = numAttemptsNextAttemptAts,
        typeIdTypes = typeIdTypes,
        limit = limit,
        offset = offset,
        orderBy = orderBy
      )(customQueryModifier)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    ids: Option[Seq[String]] = None,
    typeId: Option[String] = None,
    typeIds: Option[Seq[String]] = None,
    `type`: Option[String] = None,
    types: Option[Seq[String]] = None,
    numAttempts: Option[Int] = None,
    numAttemptsGreaterThanOrEquals: Option[Int] = None,
    numAttemptsGreaterThan: Option[Int] = None,
    numAttemptsLessThanOrEquals: Option[Int] = None,
    numAttemptsLessThan: Option[Int] = None,
    nextAttemptAt: Option[DateTime] = None,
    nextAttemptAtGreaterThanOrEquals: Option[DateTime] = None,
    nextAttemptAtGreaterThan: Option[DateTime] = None,
    nextAttemptAtLessThanOrEquals: Option[DateTime] = None,
    nextAttemptAtLessThan: Option[DateTime] = None,
    numAttemptsNextAttemptAts: Option[Seq[(Int, DateTime)]] = None,
    typeIdTypes: Option[Seq[(String, String)]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[OrderBy] = Some(OrderBy("tasks.id"))
  ) (
    implicit customQueryModifier: Query => Query = { q => q }
  ): Seq[Task] = {
    customQueryModifier(BaseQuery).
      optionalIn("tasks.id", ids).
      equals("tasks.type_id", typeId).
      optionalIn("tasks.type_id", typeIds).
      equals("tasks.type", `type`).
      optionalIn("tasks.type", types).
      equals("tasks.num_attempts", numAttempts).
      greaterThanOrEquals("tasks.num_attempts", numAttemptsGreaterThanOrEquals).
      greaterThan("tasks.num_attempts", numAttemptsGreaterThan).
      lessThanOrEquals("tasks.num_attempts", numAttemptsLessThanOrEquals).
      lessThan("tasks.num_attempts", numAttemptsLessThan).
      equals("tasks.next_attempt_at", nextAttemptAt).
      greaterThanOrEquals("tasks.next_attempt_at", nextAttemptAtGreaterThanOrEquals).
      greaterThan("tasks.next_attempt_at", nextAttemptAtGreaterThan).
      lessThanOrEquals("tasks.next_attempt_at", nextAttemptAtLessThanOrEquals).
      lessThan("tasks.next_attempt_at", nextAttemptAtLessThan).
      optionalIn2(("tasks.num_attempts", "tasks.next_attempt_at"), numAttemptsNextAttemptAts).
      optionalIn2(("tasks.type_id", "tasks.type"), typeIdTypes).
      optionalLimit(limit).
      offset(offset).
      orderBy(orderBy.flatMap(_.sql)).
      as(TasksDao.parser.*)(c)
  }

}

object TasksDao {

  val parser: RowParser[Task] = {
    SqlParser.str("id") ~
    SqlParser.str("type") ~
    SqlParser.str("type_id") ~
    SqlParser.get[UUID]("organization_guid").? ~
    SqlParser.int("num_attempts") ~
    SqlParser.get[DateTime]("next_attempt_at") ~
    SqlParser.str("errors_text").? ~
    SqlParser.str("stacktrace").? ~
    SqlParser.str("data_text") ~
    SqlParser.str("updated_by_guid") ~
    SqlParser.get[DateTime]("created_at") ~
    SqlParser.get[DateTime]("updated_at") map {
      case id ~ type_ ~ typeId ~ organizationGuid ~ numAttempts ~ nextAttemptAt ~ errors ~ stacktrace ~ data ~ updatedByGuid ~ createdAt ~ updatedAt => Task(
        id = id,
        `type` = type_,
        typeId = typeId,
        organizationGuid = organizationGuid,
        numAttempts = numAttempts,
        nextAttemptAt = nextAttemptAt,
        errors = errors.map { text => Json.parse(text).as[Seq[String]] },
        stacktrace = stacktrace,
        data = Json.parse(data),
        updatedByGuid = updatedByGuid,
        createdAt = createdAt,
        updatedAt = updatedAt
      )
    }
  }

}

@Singleton
class TasksDao @Inject() (
  override val db: Database
) extends BaseTasksDao {

  private[this] val UpsertQuery = Query("""
    | insert into tasks
    | (id, type, type_id, organization_guid, num_attempts, next_attempt_at, errors, stacktrace, data, updated_by_guid, hash_code)
    | values
    | ({id}, {type}, {type_id}, {organization_guid}::uuid, {num_attempts}::int, {next_attempt_at}::timestamptz, {errors}::json, {stacktrace}, {data}::json, {updated_by_guid}, {hash_code}::bigint)
    | on conflict (type_id, type)
    | do update
    |    set organization_guid = {organization_guid}::uuid,
    |        num_attempts = {num_attempts}::int,
    |        next_attempt_at = {next_attempt_at}::timestamptz,
    |        errors = {errors}::json,
    |        stacktrace = {stacktrace},
    |        data = {data}::json,
    |        updated_by_guid = {updated_by_guid},
    |        hash_code = {hash_code}::bigint
    |  where tasks.hash_code != {hash_code}::bigint
    | returning id
  """.stripMargin)

  private[this] val UpdateQuery = Query("""
    | update tasks
    |    set type = {type},
    |        type_id = {type_id},
    |        organization_guid = {organization_guid}::uuid,
    |        num_attempts = {num_attempts}::int,
    |        next_attempt_at = {next_attempt_at}::timestamptz,
    |        errors = {errors}::json,
    |        stacktrace = {stacktrace},
    |        data = {data}::json,
    |        updated_by_guid = {updated_by_guid},
    |        hash_code = {hash_code}::bigint
    |  where id = {id}
    |    and tasks.hash_code != {hash_code}::bigint
  """.stripMargin)

  private[this] def bindQuery(query: Query, form: TaskForm): Query = {
    query.
      bind("type", form.`type`).
      bind("type_id", form.typeId).
      bind("organization_guid", form.organizationGuid).
      bind("num_attempts", form.numAttempts).
      bind("next_attempt_at", form.nextAttemptAt).
      bind("errors", form.errors.map { v => Json.toJson(v) }).
      bind("stacktrace", form.stacktrace).
      bind("data", form.data).
      bind("hash_code", form.hashCode())
  }

  private[this] def toNamedParameter(updatedBy: UUID, form: TaskForm): Seq[NamedParameter] = {
    Seq(
      "id" -> form.id,
      "type" -> form.`type`,
      "type_id" -> form.typeId,
      "organization_guid" -> form.organizationGuid,
      "num_attempts" -> form.numAttempts,
      "next_attempt_at" -> form.nextAttemptAt,
      "errors" -> form.errors.map { v => Json.toJson(v).toString },
      "stacktrace" -> form.stacktrace,
      "data" -> form.data.toString,
      "updated_by_guid" -> updatedBy,
      "hash_code" -> form.hashCode()
    )
  }

  def upsertIfChangedByTypeIdAndType(updatedBy: UUID, form: TaskForm): Unit = {
    if (!findByTypeIdAndType(form.typeId, form.`type`).map(_.form).contains(form)) {
      upsertByTypeIdAndType(updatedBy, form)
    }
  }

  def upsertByTypeIdAndType(updatedBy: UUID, form: TaskForm): Unit = {
    db.withConnection { c =>
      upsertByTypeIdAndType(c, updatedBy, form)
    }
  }

  def upsertByTypeIdAndType(c: Connection, updatedBy: UUID, form: TaskForm): Unit = {
    bindQuery(UpsertQuery, form).
      bind("id", form.id).
      bind("updated_by_guid", updatedBy).
      anormSql().execute()(c)
    ()
  }

  def upsertBatchByTypeIdAndType(updatedBy: UUID, forms: Seq[TaskForm]): Unit = {
    db.withConnection { c =>
      upsertBatchByTypeIdAndType(c, updatedBy, forms)
    }
  }

  def upsertBatchByTypeIdAndType(c: Connection, updatedBy: UUID, forms: Seq[TaskForm]): Unit = {
    if (forms.nonEmpty) {
      val params = forms.map(toNamedParameter(updatedBy, _))
      BatchSql(UpsertQuery.sql(), params.head, params.tail: _*).execute()(c)
      ()
    }
  }

  def updateIfChangedById(updatedBy: UUID, id: String, form: TaskForm): Unit = {
    if (!findById(id).map(_.form).contains(form)) {
      updateById(updatedBy, id, form)
    }
  }

  def updateById(updatedBy: UUID, id: String, form: TaskForm): Unit = {
    db.withConnection { c =>
      updateById(c, updatedBy, id, form)
    }
  }

  def updateById(c: Connection, updatedBy: UUID, id: String, form: TaskForm): Unit = {
    bindQuery(UpdateQuery, form).
      bind("id", id).
      bind("updated_by_guid", updatedBy).
      anormSql().execute()(c)
    ()
  }

  def update(updatedBy: UUID, existing: Task, form: TaskForm): Unit = {
    db.withConnection { c =>
      update(c, updatedBy, existing, form)
    }
  }

  def update(c: Connection, updatedBy: UUID, existing: Task, form: TaskForm): Unit = {
    updateById(c, updatedBy, existing.id, form)
  }

  def updateBatch(updatedBy: UUID, forms: Seq[TaskForm]): Unit = {
    db.withConnection { c =>
      updateBatchWithConnection(c, updatedBy, forms)
    }
  }

  def updateBatchWithConnection(c: Connection, updatedBy: UUID, forms: Seq[TaskForm]): Unit = {
    if (forms.nonEmpty) {
      val params = forms.map(toNamedParameter(updatedBy, _))
      BatchSql(UpdateQuery.sql(), params.head, params.tail: _*).execute()(c)
      ()
    }
  }

  def delete(deletedBy: UUID, task: Task): Unit = {
    db.withConnection { c =>
      delete(c, deletedBy, task)
    }
  }

  def delete(c: Connection, deletedBy: UUID, task: Task): Unit = {
    deleteById(c, deletedBy, task.id)
  }

  def deleteById(deletedBy: UUID, id: String): Unit = {
    db.withConnection { c =>
      deleteById(c, deletedBy, id)
    }
  }

  def deleteById(c: Connection, deletedBy: UUID, id: String): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from tasks")
      .equals("id", id)
      .anormSql().executeUpdate()(c)
    ()
  }

  def deleteAllByIds(deletedBy: UUID, ids: Seq[String]): Unit = {
    db.withConnection { c =>
      deleteAllByIds(c, deletedBy, ids)
    }
  }

  def deleteAllByIds(c: Connection, deletedBy: UUID, ids: Seq[String]): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from tasks")
      .in("id", ids)
      .anormSql().executeUpdate()(c)
    ()
  }

  def deleteAllByNumAttempts(deletedBy: UUID, numAttempts: Int): Unit = {
    db.withConnection { c =>
      deleteAllByNumAttempts(c, deletedBy, numAttempts)
    }
  }

  def deleteAllByNumAttempts(c: Connection, deletedBy: UUID, numAttempts: Int): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from tasks")
      .equals("num_attempts", numAttempts)
      .anormSql().executeUpdate()(c)
    ()
  }

  def deleteAllByNumAttemptses(deletedBy: UUID, numAttemptses: Seq[Int]): Unit = {
    db.withConnection { c =>
      deleteAllByNumAttemptses(c, deletedBy, numAttemptses)
    }
  }

  def deleteAllByNumAttemptses(c: Connection, deletedBy: UUID, numAttemptses: Seq[Int]): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from tasks")
      .in("num_attempts", numAttemptses)
      .anormSql().executeUpdate()(c)
    ()
  }

  def deleteAllByNumAttemptsAndNextAttemptAt(deletedBy: UUID, numAttempts: Int, nextAttemptAt: DateTime): Unit = {
    db.withConnection { c =>
      deleteAllByNumAttemptsAndNextAttemptAt(c, deletedBy, numAttempts, nextAttemptAt)
    }
  }

  def deleteAllByNumAttemptsAndNextAttemptAt(c: Connection, deletedBy: UUID, numAttempts: Int, nextAttemptAt: DateTime): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from tasks")
      .equals("num_attempts", numAttempts)
      .equals("next_attempt_at", nextAttemptAt)
      .anormSql().executeUpdate()(c)
    ()
  }

  def deleteAllByNumAttemptsAndNextAttemptAts(deletedBy: UUID, numAttempts: Int, nextAttemptAts: Seq[DateTime]): Unit = {
    db.withConnection { c =>
      deleteAllByNumAttemptsAndNextAttemptAts(c, deletedBy, numAttempts, nextAttemptAts)
    }
  }

  def deleteAllByNumAttemptsAndNextAttemptAts(c: Connection, deletedBy: UUID, numAttempts: Int, nextAttemptAts: Seq[DateTime]): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from tasks")
      .equals("num_attempts", numAttempts)
      .in("next_attempt_at", nextAttemptAts)
      .anormSql().executeUpdate()(c)
    ()
  }

  def deleteAllByTypeId(deletedBy: UUID, typeId: String): Unit = {
    db.withConnection { c =>
      deleteAllByTypeId(c, deletedBy, typeId)
    }
  }

  def deleteAllByTypeId(c: Connection, deletedBy: UUID, typeId: String): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from tasks")
      .equals("type_id", typeId)
      .anormSql().executeUpdate()(c)
    ()
  }

  def deleteAllByTypeIds(deletedBy: UUID, typeIds: Seq[String]): Unit = {
    db.withConnection { c =>
      deleteAllByTypeIds(c, deletedBy, typeIds)
    }
  }

  def deleteAllByTypeIds(c: Connection, deletedBy: UUID, typeIds: Seq[String]): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from tasks")
      .in("type_id", typeIds)
      .anormSql().executeUpdate()(c)
    ()
  }

  def deleteByTypeIdAndType(deletedBy: UUID, typeId: String, `type`: String): Unit = {
    db.withConnection { c =>
      deleteByTypeIdAndType(c, deletedBy, typeId, `type`)
    }
  }

  def deleteByTypeIdAndType(c: Connection, deletedBy: UUID, typeId: String, `type`: String): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from tasks")
      .equals("type_id", typeId)
      .equals("type", `type`)
      .anormSql().executeUpdate()(c)
    ()
  }

  def deleteAllByTypeIdAndTypes(deletedBy: UUID, typeId: String, types: Seq[String]): Unit = {
    db.withConnection { c =>
      deleteAllByTypeIdAndTypes(c, deletedBy, typeId, types)
    }
  }

  def deleteAllByTypeIdAndTypes(c: Connection, deletedBy: UUID, typeId: String, types: Seq[String]): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from tasks")
      .equals("type_id", typeId)
      .in("type", types)
      .anormSql().executeUpdate()(c)
    ()
  }

  def setJournalDeletedByUserId(c: Connection, deletedBy: UUID): Unit = {
    Query(s"SET journal.deleted_by_user_id = '${deletedBy}'").anormSql().executeUpdate()(c)
    ()
  }

}