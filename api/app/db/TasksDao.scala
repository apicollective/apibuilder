package db

import com.gilt.apidoc.internal.v0.models.{Task, TaskData, TaskDataDiffVersion, TaskDataIndexVersion, TaskDataUndefinedType}
import com.gilt.apidoc.internal.v0.models.json._
import com.gilt.apidoc.api.v0.models.{User}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object TasksDao {

  private val BaseQuery = """
    select tasks.guid,
           tasks.data::varchar,
           tasks.number_attempts,
           tasks.last_error
      from tasks
     where true
  """

  val IncrementNumberAttemptsQuery = """
    update tasks
       set number_attempts = number_attempts + 1,
           updated_by_guid = {updated_by_guid}::uuid
     where guid = {guid}::uuid
  """

  val RecordErrorQuery = """
    update tasks
       set last_error = {last_error},
           updated_by_guid = {updated_by_guid}::uuid
     where guid = {guid}::uuid
  """


  private val InsertQuery = """
    insert into tasks
    (guid, data, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {data}::json, {created_by_guid}::uuid, {updated_by_guid}::uuid)
  """

  private[db] def insert(implicit c: java.sql.Connection, createdBy: User, data: TaskData): UUID = {
    val guid = UUID.randomUUID

    SQL(InsertQuery).on(
      'guid -> guid,
      'data -> Json.toJson(data).toString,
      'created_by_guid -> createdBy.guid,
      'updated_by_guid -> createdBy.guid
    ).execute()

    guid
  }

  def softDelete(deletedBy: User, task: Task) {
    SoftDelete.delete("tasks", deletedBy, task.guid)
  }

  def incrementNumberAttempts(user: User, task: Task) {
    DB.withConnection { implicit c =>
      SQL(IncrementNumberAttemptsQuery).on(
        'guid -> task.guid,
        'updated_by_guid -> user.guid
      ).execute()
    }
  }

  def recordError(user: User, task: Task, error: String) {
    DB.withConnection { implicit c =>
      SQL(RecordErrorQuery).on(
        'guid -> task.guid,
        'last_error -> error,
        'updated_by_guid -> user.guid
      ).execute()
    }
  }

  def findByGuid(guid: UUID): Option[Task] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    nOrFewerAttempts: Option[Long] = None,
    nOrMoreAttempts: Option[Long] = None,
    nOrMoreMinutesOld: Option[Long] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Task] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and tasks.guid = {guid}::uuid" },
      nOrFewerAttempts.map { v => "and tasks.number_attempts <= {n_or_fewer_attempts}" },
      nOrMoreAttempts.map { v => "and tasks.number_attempts >= {n_or_more_attempts}" },
      nOrMoreMinutesOld.map { v => s"and tasks.updated_at <= timezone('utc', now()) - interval '$v minutes'" },
      isDeleted.map { Filters.isDeleted("tasks", _) },
      Some(s"order by tasks.number_attempts, tasks.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      nOrFewerAttempts.map('n_or_fewer_attempts -> _),
      nOrMoreAttempts.map('n_or_more_attempts -> _)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ): Task = {
    Task(
      guid = row[UUID]("guid"),
      data = Json.parse(row[String]("data")).as[TaskData],
      numberAttempts = row[Long]("number_attempts"),
      lastError = row[Option[String]]("last_error")
    )
  }

}
