package processor

import cats.data.ValidatedNec
import db.VersionsDao
import io.apibuilder.task.v0.models.TaskType

import java.util.UUID
import javax.inject.Inject


class MigrateVersionProcessor @Inject()(
  args: TaskProcessorArgs,
  versionsDao: VersionsDao
) extends TaskProcessorWithGuid(args, TaskType.MigrateVersion) {

  override def processRecord(versionGuid: UUID): ValidatedNec[String, Unit] = {
    versionsDao.migrateVersionGuid(versionGuid)
  }
}