package actors

import akka.actor.{Actor, ActorLogging, Cancellable}
import db.InternalTasksDao
import io.apibuilder.task.v0.models.TaskType
import play.api.{Environment, Mode}

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, HOURS, SECONDS}

class ScheduleTasksActor @Inject()(
  tasksDao: InternalTasksDao,
  env: Environment
) extends Actor with ActorLogging with ErrorHandler  {

  private implicit val ec: ExecutionContext = context.system.dispatchers.lookup("schedule-tasks-actor-context")

  private case class UpsertTask(typ: TaskType)

  private def schedule(
    taskType: TaskType,
    interval: FiniteDuration
  )(implicit
    initialInterval: FiniteDuration = interval
  ): Cancellable = {
    val finalInitial = env.mode match {
      case Mode.Test => FiniteDuration(24, HOURS)
      case _ => initialInterval
    }
    context.system.scheduler.scheduleWithFixedDelay(finalInitial, interval, self, UpsertTask(taskType))
  }

  private def scheduleOnce(taskType: TaskType): Unit = {
    context.system.scheduler.scheduleOnce(FiniteDuration(10, SECONDS)) {
      UpsertTask(taskType)
    }
  }


  private val cancellables: Seq[Cancellable] = {
    import TaskType._
    scheduleOnce(ScheduleMigrateVersions)
    Seq(
      schedule(ScheduleSyncGeneratorServices, FiniteDuration(1, HOURS)),
      schedule(CheckInvariants, FiniteDuration(12, HOURS)),
      schedule(PurgeDeleted, FiniteDuration(1, HOURS))(FiniteDuration(5, SECONDS)),
    )
  }

  override def postStop(): Unit = {
    cancellables.foreach(_.cancel())
    super.postStop()
  }

  override def receive: Receive = {
    case UpsertTask(taskType) => tasksDao.queue(taskType, "ScheduleTasksActor")
    case other => logUnhandledMessage(other)
  }

}
