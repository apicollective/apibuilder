package actors

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class ActorsModule extends AbstractModule with AkkaGuiceSupport {
  override def configure = {
    bindActor[MainActor]("main-actor")
    bindActor[PeriodicActor]("PeriodicActor")
    bindActor[GeneratorServiceActor]("generator-service-actor")
    bindActor[EmailActor]("email-actor")
    bindActor[TaskDispatchActor](
      "TaskDispatchActor",
      _.withDispatcher("task-context-dispatcher")
    )
    bindActorFactory[TaskActor, actors.TaskActor.Factory]
  }
}
