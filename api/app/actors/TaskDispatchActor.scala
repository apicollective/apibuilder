package actors

import akka.actor._
import io.apibuilder.task.v0.models.TaskType
import play.api.libs.concurrent.InjectedActorSupport
import processor.TaskDispatchActorCompanion

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, SECONDS}

@Singleton
class TaskDispatchActor @Inject() (
  factory: TaskActor.Factory,
  companion: TaskDispatchActorCompanion
) extends Actor with ActorLogging with ErrorHandler with InjectedActorSupport {
  private val ec: ExecutionContext = context.system.dispatchers.lookup("task-context")
  private case object Process

  private val actors = scala.collection.mutable.Map[TaskType, ActorRef]()

  private def schedule(message: Any, interval: FiniteDuration): Cancellable = {
    context.system.scheduler.scheduleWithFixedDelay(FiniteDuration(1, SECONDS), interval, self, message)(ec)
  }

  private val cancellables: Seq[Cancellable] = {
    Seq(
      schedule(Process, FiniteDuration(2, SECONDS))
    )
  }

  override def postStop(): Unit = {
    cancellables.foreach(_.cancel())
    super.postStop()
  }

  override def receive: Receive = {
    case Process => process()
    case other => logUnhandledMessage(other)
  }

  private def process(): Unit = {
    companion.typesWithWork.foreach { typ =>
      upsertActor(typ) ! TaskActor.Process
    }
  }

  private def upsertActor(typ: TaskType): ActorRef = {
    actors.getOrElse(
      typ, {
        val name = s"task$typ"
        val ref = injectedChild(
          factory(typ),
          name = name,
          _.withDispatcher("task-context-dispatcher")
        )
        actors += (typ -> ref)
        ref
      }
    )
  }

}
