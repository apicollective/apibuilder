package db

import com.bryzek.apidoc.internal.v0.models.{Task, TaskData}
import com.bryzek.apidoc.internal.v0.models.json._
import com.bryzek.apidoc.api.v0.models.User
import anorm._
import javax.inject.{Inject, Named, Singleton}
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

/**
  * A task represents a bit of work that should be completed asynchronously. Example tasks include:
  *
  *   - generating a diff between two versions of a service
  *   - updates a search index for a particular service
  *
  * Tasks provide for the following semantics:
  *   - transactionally record to execute a task
  *   - at least once execution
  *   - near real time execution
  *   - mostly sequential execution of tasks (but not guaranteed)
  *
  * The implementation works by:
  *   - caller inserts a task transactionally
  *   - to trigger real time execution, send a TaskCreated message to
  *     the Main Actor - send this AFTER the transaction commits or the
  *     actor may not see the task
  *   - periodically, the task system picks up tasks that have not
  *     been processed and executes them
  */
@Singleton
class TasksDao @Inject() (
  @Named("main-actor") mainActor: akka.actor.ActorRef
) {

  private[this] val BaseQuery = """
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

  private[this] val InsertQuery = """
    insert into tasks
    (guid, data, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {data}::json, {created_by_guid}::uuid, {updated_by_guid}::uuid)
  """

  private[this] val PurgeQuery = """
    delete from tasks where guid = {guid}::uuid
  """

  def create(createdBy: User, data: TaskData): UUID = {
    val taskGuid = DB.withConnection { implicit c =>
      insert(c, createdBy, data)
    }
    mainActor ! actors.MainActor.Messages.TaskCreated(taskGuid)
    taskGuid
  }

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

  def purge(deletedBy: User, task: Task) {
    DB.withConnection { implicit c =>
      SQL(PurgeQuery).on('guid -> task.guid).execute()
    }
  }

  def incrementNumberAttempts(user: User, task: Task) {
    DB.withConnection { implicit c =>
      SQL(IncrementNumberAttemptsQuery).on(
        'guid -> task.guid,
        'updated_by_guid -> user.guid
      ).execute()
    }
  }

  def recordError(user: User, task: Task, error: Throwable) {
    val sw = new java.io.StringWriter
    error.printStackTrace(new java.io.PrintWriter(sw))
    recordError(user, task, sw.toString)
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
    deletedAtLeastNDaysAgo: Option[Long] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Task] = {
    deletedAtLeastNDaysAgo.map { n =>
      isDeleted match {
        case None | Some(true) => {}
        case Some(false) => sys.error("When filtering by deletedAtLeastNDaysAgo, you must also set isDeleted")
      }
    }

    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and tasks.guid = {guid}::uuid" },
      nOrFewerAttempts.map { v => "and tasks.number_attempts <= {n_or_fewer_attempts}" },
      nOrMoreAttempts.map { v => "and tasks.number_attempts >= {n_or_more_attempts}" },
      nOrMoreMinutesOld.map { v => s"and tasks.updated_at <= timezone('utc', now()) - interval '$v minutes'" },
      isDeleted.map { Filters.isDeleted("tasks", _) },
      deletedAtLeastNDaysAgo.map { v => s"and tasks.deleted_at <= timezone('utc', now()) - interval '$v days'" },
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
