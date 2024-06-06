package processor

import cats.data.ValidatedNec
import cats.implicits._
import io.apibuilder.task.v0.models.{DiffVersionData, TaskType}

import javax.inject.Inject


class DiffVersionProcessor @Inject()(
                              args: TaskProcessorArgs,
) extends TaskProcessorWithData[DiffVersionData](args, TaskType.DiffVersion) {

  override def processRecord(id: String, data: DiffVersionData): ValidatedNec[String, Unit] = {
    "TODO".invalidNec
  }

}