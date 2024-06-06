package processor

import cats.implicits._
import cats.data.ValidatedNec
import db.generated.Task

import javax.inject.Inject

class TaskActorCompanion @Inject() (
  indexApplication: IndexApplicationProcessor
) {

  def process(typ: TaskType): Unit = {
    lookup(typ).process()
  }

  private[this] def lookup(typ: TaskType): BaseTaskProcessor = {
    import TaskType._
    typ match {
      case IndexApplication => indexApplication
    }
  }
}
