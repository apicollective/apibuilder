package processor

import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec
import cats.implicits._
import db.generated.{Task, TaskForm, TasksDao}
import io.apibuilder.task.v0.models.TaskType
import io.flow.postgresql.OrderBy
import lib.Constants
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, JsValue, Reads, Writes}
import play.libs.exception.ExceptionUtils

import java.sql.Connection
import java.util.UUID
import javax.inject.Inject
import scala.util.{Failure, Success, Try}

class TaskProcessorArgs @Inject() (
  val dao: TasksDao
) {}

abstract class TaskProcessor(
  args: TaskProcessorArgs,
  typ: TaskType
) extends BaseTaskProcessor(args, typ) {
  def processRecord(id: String): ValidatedNec[String, Unit]

  override final def processTask(task: Task): ValidatedNec[String, Unit] = {
    processRecord(task.typeId)
  }

}

abstract class TaskProcessorWithGuid(
                              args: TaskProcessorArgs,
                              typ: TaskType
                            ) extends BaseTaskProcessor(args, typ) {

  def processRecord(guid: UUID): ValidatedNec[String, Unit]

  override def processTask(task: Task): ValidatedNec[String, Unit] = {
    validateGuid(task.typeId).andThen(processRecord)
  }

  protected def validateGuid(value: String): ValidatedNec[String, UUID] = {
    Try {
      UUID.fromString(value)
    } match {
      case Success(v) => v.validNec
      case Failure(_) => s"Invalid guid '$value'".invalidNec
    }
  }
}

abstract class TaskProcessorWithData[T](
  args: TaskProcessorArgs,
  typ: TaskType
)(implicit reads: Reads[T], writes: Writes[T])
  extends BaseTaskProcessor(args, typ) {

  def processRecord(id: String, data: T): ValidatedNec[String, Unit]

  override final def processTask(task: Task): ValidatedNec[String, Unit] = {
    parseData(task.data).andThen { d =>
      processRecord(task.typeId, d)
    }
  }

  private[this] def parseData(data: JsValue): ValidatedNec[String, T] = {
    data.asOpt[T] match {
      case None => "Failed to parse data".invalidNec
      case Some(instance) => instance.validNec
    }
  }

}

abstract class BaseTaskProcessor(
  args: TaskProcessorArgs,
  val typ: TaskType
) {

  private[this] val Limit = 100

  final def process(): Unit = {
    args.dao
      .findAll(
        `type` = Some(typ.toString),
        nextAttemptAtLessThanOrEquals = Some(DateTime.now),
        limit = Some(Limit),
        orderBy = Some(OrderBy("num_attempts, next_attempt_at"))
      )
      .foreach(processRecordSafe)
  }

  def processTask(task: Task): ValidatedNec[String, Unit]

  private[this] def processRecordSafe(task: Task): Unit = {
    Try {
      processTask(task)
    } match {
      case Success(r) =>
        r match {
          case Invalid(errors) => {
            setErrors(task, errors.toList, stacktrace = None)
          }
          case Valid(_) => args.dao.delete(Constants.DefaultUserGuid, task)
        }
      case Failure(e) => {
        setErrors(task, Seq("ERROR: " + e.getMessage), stacktrace = Some(ExceptionUtils.getStackTrace(e)))
      }
    }
  }

  private[this] def setErrors(task: Task, errors: Seq[String], stacktrace: Option[String]): Unit = {
    val numAttempts = task.numAttempts + 1

    args.dao.update(
      Constants.DefaultUserGuid,
      task,
      task.form.copy(
        numAttempts = numAttempts,
        nextAttemptAt = computeNextAttemptAt(task),
        errors = Some(errors.distinct),
        stacktrace = stacktrace
      )
    )
  }

  protected def computeNextAttemptAt(task: Task): DateTime = {
    DateTime.now.plusMinutes(5 * (task.numAttempts + 1))
  }

  final protected def makeInitialTaskForm(
    typeId: String,
    organizationGuid: Option[UUID],
    data: JsObject
  ): TaskForm = {
    TaskForm(
      id = s"$typ:$typeId",
      `type` = typ.toString,
      typeId = typeId,
      organizationGuid = organizationGuid,
      data = data,
      numAttempts = 0,
      nextAttemptAt = DateTime.now,
      errors = None,
      stacktrace = None
    )
  }

  final protected def insertIfNew(c: Connection, form: TaskForm): Unit = {
    if (args.dao.findByTypeIdAndTypeWithConnection(c, form.typeId, form.`type`).isEmpty) {
      args.dao.upsertByTypeIdAndType(c, Constants.DefaultUserGuid, form)
    }
  }

}
