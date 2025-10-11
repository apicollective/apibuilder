package actors

import org.apache.pekko.actor.{Actor, ActorLogging}
import com.google.inject.assistedinject.Assisted
import io.apibuilder.task.v0.models.TaskType
import processor.TaskActorCompanion

import javax.inject.Inject

object TaskActor {
  case object Process
  trait Factory {
    def apply(
      @Assisted("type") `type`: TaskType
    ): Actor
  }
}

class TaskActor @Inject() (
  @Assisted("type") `type`: TaskType,
  companion: TaskActorCompanion
) extends Actor with ActorLogging with ErrorHandler {

  def receive: Receive = {
    case TaskActor.Process => companion.process(`type`)
    case m: Any => logUnhandledMessage(m)
  }

}
