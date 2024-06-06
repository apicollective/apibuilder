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


  private[this] val VersionsNeedingUpgrade = Query(
    """
      |select v.guid
      |  from versions v
      |  join applications apps on apps.guid = v.application_guid and apps.deleted_at is null
      |  left join tasks t on t.type = {task_type} and t.type_id::uuid = v.guid
      | where v.deleted_at is null
      |   and t.id is null
      |   and not exists (
      |    select 1
      |      from cache.services
      |      where services.deleted_at is null
      |        and services.version_guid = v.guid
      |        and services.version = {service_version}
      |  )
      |limit 250
      |""".stripMargin
  ).bind("service_version", MigrateVersion.ServiceVersionNumber)
    .bind("task_type", TaskType.MigrateVersion.toString)

  @tailrec
  private[this] def scheduleMigrationTasks(): Unit = {
    val versionGuids = db.withConnection { implicit c =>
      VersionsNeedingUpgrade
        .as(SqlParser.get[_root_.java.util.UUID](1).*)
    }
    if (versionGuids.nonEmpty) {
      versionGuids.foreach { vGuid =>
        internalTasksDao.queue(TaskType.MigrateVersion, vGuid.toString)
      }
      scheduleMigrationTasks()
    }
  }
}