package actors

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class ActorsModule extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    bindActor[ScheduleTasksActor]("ScheduleTasksActor")
    bindActor[TaskDispatchActor](
      "TaskDispatchActor",
      _.withDispatcher("task-context-dispatcher")
    )
    bindActorFactory[TaskActor, actors.TaskActor.Factory]
  }
}
