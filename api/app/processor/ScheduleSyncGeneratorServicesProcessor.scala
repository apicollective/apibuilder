package processor

import cats.data.ValidatedNec
import cats.implicits._
import db.generators.ServicesDao
import db.{Authorization, InternalTasksDao}
import io.apibuilder.task.v0.models.TaskType
import lib.ValidatedHelpers

import javax.inject.Inject
import scala.annotation.tailrec


class ScheduleSyncGeneratorServicesProcessor @Inject()(
  args: TaskProcessorArgs,
  servicesDao: ServicesDao,
  internalTasksDao: InternalTasksDao
) extends TaskProcessor(args, TaskType.ScheduleSyncGeneratorServices) with ValidatedHelpers {

  override def processRecord(id: String): ValidatedNec[String, Unit] = {
    doSyncAll(pageSize = 200, offset = 0).validNec
  }

  @tailrec
  private def doSyncAll(pageSize: Long, offset: Long): Unit = {
    val all = servicesDao.findAll(
      Authorization.All,
      limit = pageSize,
      offset = offset
    ).map(_.guid.toString)

    if (all.nonEmpty) {
      internalTasksDao.queueBatch(TaskType.SyncGeneratorService, all)
      doSyncAll(pageSize, offset + all.length)
    }
  }
}