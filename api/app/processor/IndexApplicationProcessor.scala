package processor

import cats.data.ValidatedNec
import cats.implicits._
import db.generated.Task

import java.util.UUID
import javax.inject.Inject


class IndexApplicationProcessor @Inject()(
                              args: TaskProcessorArgs,
                              ) extends BaseTaskProcessor(args, TaskType.Noop) {

  override def processTask(task: Task): ValidatedNec[String, Unit] = {
    validateGuid(task.typeId).andThen(processApplicationGuid)
  }

  def processApplicationGuid(guid: UUID): ValidatedNec[String, Unit] = {

  }
}