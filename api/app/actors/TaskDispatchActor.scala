package actors

import akka.actor._
import play.api.libs.concurrent.InjectedActorSupport
import processor.{TaskDispatchActorCompanion, TaskType}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, SECONDS}

@Singleton
class TaskDispatchActor @Inject() (
  factory: TaskActor.Factory,
  companion: TaskDispatchActorCompanion
) extends Actor with ActorLogging with ErrorHandler with InjectedActorSupport {
  private[this] val ec: ExecutionContext = context.system.dispatchers.lookup("task-context")
  private[this] case object Process

  private[this] val actors = scala.collection.mutable.Map[TaskType, ActorRef]()

  private[this] def schedule(message: Any, interval: FiniteDuration): Cancellable = {
    context.system.scheduler.scheduleWithFixedDelay(FiniteDuration(1, SECONDS), interval, self, message)(ec)
  }

  private[this] val cancellables: Seq[Cancellable] = {
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

  private[this] def process(): Unit = {
    companion.typesWithWork.foreach { typ =>
      upsertActor(typ) ! TaskActor.Process
    }
  }

  private[this] def upsertActor(typ: TaskType): ActorRef = {
    actors.getOrElse(
      typ, {
        val name = s"task$typ"
        val ref = injectedChild(
          factory(typ),
          name = name,
          _.withDispatcher("task-context-dispatch")
        )
        actors += (typ -> ref)
        ref
      }
    )
  }

}
