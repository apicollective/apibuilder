package actors

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class ActorsModule extends AbstractModule with AkkaGuiceSupport {
  override def configure = {
    bindActor[PeriodicActor]("PeriodicActor")
    bindActor[TaskDispatchActor](
      "TaskDispatchActor",
      _.withDispatcher("task-context-dispatcher")
    )
    bindActorFactory[TaskActor, actors.TaskActor.Factory]
  }
}
