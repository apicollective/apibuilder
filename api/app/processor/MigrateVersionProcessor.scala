package processor

import cats.data.ValidatedNec
import db.InternalVersionsDao
import io.apibuilder.task.v0.models.TaskType

import java.util.UUID
import javax.inject.Inject

object MigrateVersion {
  val ServiceVersionNumber: String = io.apibuilder.spec.v0.Constants.Version.toLowerCase
}

class MigrateVersionProcessor @Inject()(
  args: TaskProcessorArgs,
  versionsDao: InternalVersionsDao
) extends TaskProcessorWithGuid(args, TaskType.MigrateVersion) {

  override def processRecord(versionGuid: UUID): ValidatedNec[String, Unit] = {
    versionsDao.migrateVersionGuid(versionGuid)
  }
}