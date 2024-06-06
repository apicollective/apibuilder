package actors

import akka.actor.{Actor, ActorLogging, Cancellable}
import db.InternalTasksDao
import io.apibuilder.task.v0.models.TaskType
import play.api.{Environment, Mode}

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, HOURS}

class PeriodicActor @Inject() (
  tasksDao: InternalTasksDao,
  env: Environment
) extends Actor with ActorLogging with ErrorHandler  {

  private[this] implicit val ec: ExecutionContext = context.system.dispatchers.lookup("periodic-actor-context")

  private[this] case class UpsertTask(typ: TaskType)

  private[this] def schedule(
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

  private[this] val cancellables: Seq[Cancellable] = {
    import TaskType._
    Seq(
      schedule(CleanupDeletions, FiniteDuration(1, HOURS)),
      schedule(ScheduleMigrateVersions, FiniteDuration(12, HOURS)),
      schedule(ScheduleSyncGeneratorServices, FiniteDuration(1, HOURS)),
      schedule(CheckInvariants, FiniteDuration(1, HOURS)),
    )
  }

  override def postStop(): Unit = {
    cancellables.foreach(_.cancel())
    super.postStop()
  }

  override def receive: Receive = {
    case UpsertTask(taskType) => tasksDao.queue(taskType, "periodic")
    case other => logUnhandledMessage(other)
  }

}
