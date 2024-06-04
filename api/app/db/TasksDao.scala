package db

import anorm._
import io.apibuilder.internal.v0.models.{Task, TaskData}
import io.apibuilder.internal.v0.models.json._
import io.apibuilder.api.v0.models.User
import io.flow.postgresql.Query
import javax.inject.{Inject, Named, Singleton}
import org.joda.time.DateTime
import play.api.db._
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
  @NamedDatabase("default") db: Database,
  @Named("main-actor") mainActor: akka.actor.ActorRef
) {

  private[this] val dbHelpers = DbHelpers(db, "tasks")

  private[this] val BaseQuery = Query("""
    select tasks.guid,
           tasks.data::text,
           tasks.number_attempts,
           tasks.last_error
      from tasks
  """)

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
    val taskGuid = db.withConnection { implicit c =>
      insert(c, createdBy, data)
    }
    mainActor ! actors.MainActor.Messages.TaskCreated(taskGuid)
    taskGuid
  }

  private[db] def insert(implicit c: java.sql.Connection, createdBy: User, data: TaskData): UUID = {
    val guid = UUID.randomUUID

    SQL(InsertQuery).on(
      "guid" -> guid,
      "data" -> Json.toJson(data).toString,
      "created_by_guid" -> createdBy.guid,
      "updated_by_guid" -> createdBy.guid
    ).execute()

    guid
  }

  def softDelete(deletedBy: User, task: Task): Unit = {
    dbHelpers.delete(deletedBy, task.guid)
  }

  def purge(task: Task): Unit = {
    db.withConnection { implicit c =>
      SQL(PurgeQuery).on("guid" -> task.guid).execute()
    }
  }

  def incrementNumberAttempts(user: User, task: Task): Unit = {
    db.withConnection { implicit c =>
      SQL(IncrementNumberAttemptsQuery).on(
        "guid" -> task.guid,
        "updated_by_guid" -> user.guid
      ).execute()
    }
  }

  def recordError(user: User, task: Task, error: Throwable): Unit = {
    val sw = new java.io.StringWriter
    error.printStackTrace(new java.io.PrintWriter(sw))
    recordError(user, task, sw.toString)
  }

  def recordError(user: User, task: Task, error: String): Unit = {
    db.withConnection { implicit c =>
      SQL(RecordErrorQuery).on(
        "guid" -> task.guid,
        "last_error" -> error,
        "updated_by_guid" -> user.guid
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
    createdOnOrBefore: Option[DateTime] = None,
    createdOnOrAfter: Option[DateTime] = None,
    isDeleted: Option[Boolean] = Some(false),
    deletedAtLeastNDaysAgo: Option[Long] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Task] = {
    deletedAtLeastNDaysAgo.foreach { _ =>
      isDeleted match {
        case None | Some(true) => // no-op
        case Some(false) => sys.error("When filtering by deletedAtLeastNDaysAgo, you must also set isDeleted")
      }
    }

    db.withConnection { implicit c =>
      BaseQuery.
        equals("tasks.guid", guid).
        lessThanOrEquals("tasks.number_attempts", nOrFewerAttempts).
        greaterThanOrEquals("tasks.number_attempts", nOrMoreAttempts).
        lessThanOrEquals("tasks.created_at", createdOnOrBefore).
        greaterThanOrEquals("tasks.created_at", createdOnOrAfter).
        and(isDeleted.map {
          Filters.isDeleted("tasks", _)
        }).
        lessThanOrEquals("tasks.deleted_at", deletedAtLeastNDaysAgo.map(days => DateTime.now.minusDays(days.toInt))).
        orderBy("tasks.number_attempts, tasks.created_at").
        limit(limit).
        offset(offset).
        anormSql().as(
        parser().*
      )
    }
  }

  private[this] def parser(): RowParser[Task] = {
    SqlParser.get[UUID]("guid") ~
    SqlParser.str("data") ~
    SqlParser.long("number_attempts") ~
    SqlParser.str("last_error").? map {
      case guid ~ data ~ numberAttempts ~ lastError => {
        Task(
          guid = guid,
          data = Json.parse(data).as[TaskData],
          numberAttempts = numberAttempts,
          lastError = lastError
        )
      }
    }
  }

}
