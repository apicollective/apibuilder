package processor

import cats.implicits._
import cats.data.ValidatedNec
import db.generated.Task

import javax.inject.Inject

class TaskActorCompanion @Inject() (
  noopProcessor: NoopProcessor
) {

  def process(typ: TaskType): Unit = {
    lookup(typ).process()
  }

  private[this] def lookup(typ: TaskType): BaseTaskProcessor = {
    import TaskType._
    typ match {
      case Noop => noopProcessor
    }
  }
}

class NoopProcessor @Inject() (
                              args: TaskProcessorArgs
                              ) extends BaseTaskProcessor(args, TaskType.Noop) {
  override def processTask(task: Task): ValidatedNec[String, Unit] = {
    ().validNec
  }
}