package processor

import cats.implicits._
import cats.data.ValidatedNec
import db.{Authorization, OrganizationsDao, UsersDao}
import io.apibuilder.task.v0.models.TaskType
import io.flow.postgresql.Query
import org.joda.time.DateTime
import play.api.db.Database

import javax.inject.Inject
import scala.util.{Failure, Success, Try}


class PurgeOldDeletedProcessor @Inject()(
  args: TaskProcessorArgs,
  db: Database,
  usersDao: UsersDao,
  organizationsDao: OrganizationsDao
) extends TaskProcessor(args, TaskType.PurgeOldDeleted) {
  import DeleteMetadata._

  override def processRecord(id: String): ValidatedNec[String, Unit] = {
    delete("versions", "version_guid", VersionSoft)
    delete("applications", "application_guid", ApplicationSoft)
    delete("organizations", "organization_guid", OrganizationSoft)
    purgeOldOrganizations()
  }

  private[this] def purgeOldOrganizations(): ValidatedNec[String, Unit] = {
    organizationsDao.findAll(
      Authorization.All,
      deletedAtBefore = Some(DateTime.now.minusYears(1)),
      isDeleted = Some(true),
      limit = 1000,
      offset = 0,
    ).map { org =>
      Try {
        exec(Query("delete from organizations").equals("guid", org.guid))
      } match {
        case Success(_) => ().validNec
        case Failure(e) => s"Failed to delete org where guid = '${org.guid}': ${e.getMessage}".invalidNec
      }
    }.sequence.map { _ => () }
  }

  private[processor] def delete(parentTable: String, column: String, tables: Seq[String]): Unit = {
    tables.foreach { table =>
      exec(
        s"""
          |delete from $table
          | where deleted_at < now() - interval '45 days'
          |   and $column in (
          |  select guid from $parentTable where deleted_at <= now() - interval '6 months'
          |)
          |""".stripMargin
      )
    }
  }

  private[this] def exec(q: String): Unit = {
    exec(Query(q).bind("deleted_by_guid", usersDao.AdminUser.guid))
  }

  private[this] def exec(q: Query): Unit = {
    db.withConnection { c =>
      q.anormSql().executeUpdate()(c)
    }
    ()
  }
}