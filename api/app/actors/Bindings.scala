package actors

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class ActorsModule extends AbstractModule with AkkaGuiceSupport {
  def configure = {
    bindActor[MainActor]("main-actor")
    bindActor[GeneratorServiceActor]("generator-service-actor")
    bindActor[TaskActor]("task-actor")
    bindActor[UserActor]("user-actor")
  }
}
