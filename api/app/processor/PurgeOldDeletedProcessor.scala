package processor

import cats.data.ValidatedNec
import cats.implicits._
import db.UsersDao
import io.apibuilder.task.v0.models.TaskType
import io.flow.postgresql.Query
import play.api.db.Database

import javax.inject.Inject


class PurgeOldDeletedProcessor @Inject()(
  args: TaskProcessorArgs,
  db: Database,
  usersDao: UsersDao
) extends TaskProcessor(args, TaskType.PurgeOldDeleted) {
  import DeleteMetadata._

  override def processRecord(id: String): ValidatedNec[String, Unit] = {
    delete("versions", "version_guid", VersionSoft)
    delete("applications", "application_guid", ApplicationSoft)
    delete("organizations", "organization_guid", OrganizationSoft)
    ().validNec
  }


  private[processor] def delete(parentTable: String, column: String, tables: Seq[String]): Unit = {
    tables.foreach { table =>
      exec(
        s"delete from $table where $column in (select guid from $parentTable where deleted_at <= now() - interval '6 months')"
      )
    }
  }

  private[this] def exec(q: String): Unit = {
    db.withConnection { c =>
      Query(q)
        .bind("deleted_by_guid", usersDao.AdminUser.guid)
        .anormSql()
        .executeUpdate()(c)
    }
    ()
  }
}