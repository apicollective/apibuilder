package processor

import cats.implicits._
import cats.data.ValidatedNec
import db.{Authorization, OrganizationsDao, UsersDao}
import io.apibuilder.task.v0.models.TaskType
import io.flow.postgresql.Query
import org.joda.time.DateTime
import play.api.{Environment, Mode}
import play.api.db.Database

import javax.inject.Inject
import scala.util.{Failure, Success, Try}


class PurgeOldDeletedProcessor @Inject()(
  args: TaskProcessorArgs,
  env: Environment,
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

  private[this] val cutoffProd = DateTime.parse("2024-06-05T12:00:00.0+00")
  private[this] val cutoff = env.mode match {
    case Mode.Prod => cutoffProd
    case _ => cutoffProd.minusYears(10)
  }

  private[this] def purgeOldOrganizations(): ValidatedNec[String, Unit] = {
    organizationsDao.findAll(
      Authorization.All,
      deletedAtBefore = Some(Seq(DateTime.now.minusYears(1), cutoff).max),
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
          |   and deleted_at >= {cutoff}::timestamptz
          |   and $column in (
          |  select guid from $parentTable where deleted_at <= now() - interval '6 months'
          |)
          |""".stripMargin
      )
    }
  }

  private[this] def exec(q: String): Unit = {
    exec(Query(q).bind("deleted_by_guid", usersDao.AdminUser.guid).bind("cutoff", cutoff))
  }

  private[this] def exec(q: Query): Unit = {
    db.withConnection { c =>
      q.anormSql().executeUpdate()(c)
    }
    ()
  }
}