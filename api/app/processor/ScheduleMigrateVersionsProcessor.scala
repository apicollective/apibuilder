package processor

import anorm.SqlParser
import cats.data.ValidatedNec
import cats.implicits._
import db.InternalTasksDao
import io.apibuilder.task.v0.models.TaskType
import io.flow.postgresql.Query
import play.api.db.{Database, NamedDatabase}

import javax.inject.Inject
import scala.annotation.tailrec


class ScheduleMigrateVersionsProcessor @Inject()(
  args: TaskProcessorArgs,
  @NamedDatabase("default") db: Database,
  internalTasksDao: InternalTasksDao,
) extends TaskProcessor(args, TaskType.ScheduleMigrateVersions) {

  override def processRecord(id: String): ValidatedNec[String, Unit] = {
    scheduleMigrationTasks().validNec
  }


  private val Limit = 1000
  private val VersionsNeedingUpgrade = Query(
    s"""
      |select v.guid
      |  from versions v
      |  join applications apps on apps.guid = v.application_guid and apps.deleted_at is null
      |  left join cache.services on services.deleted_at is null
      |                          and services.version_guid = v.guid
      |                          and services.version = {service_version}
      | where v.deleted_at is null
      |   and services.guid is null
      |   and v.guid not in (select type_id::uuid from tasks where type = {task_type})
      |limit $Limit
      |""".stripMargin
  ).bind("service_version", MigrateVersion.ServiceVersionNumber)
    .bind("task_type", TaskType.MigrateVersion.toString)

  @tailrec
  private def scheduleMigrationTasks(): Unit = {
    val versionGuids = db.withConnection { implicit c =>
      VersionsNeedingUpgrade
        .as(SqlParser.get[_root_.java.util.UUID](1).*)
    }
    if (versionGuids.nonEmpty) {
      internalTasksDao.queueBatch(TaskType.MigrateVersion, versionGuids.map(_.toString))
      if (versionGuids.length >= Limit) {
        scheduleMigrationTasks()
      }
    }
  }
}