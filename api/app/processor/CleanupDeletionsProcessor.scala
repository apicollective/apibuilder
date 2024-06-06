package processor

import cats.data.ValidatedNec
import cats.implicits._
import db.UsersDao
import io.apibuilder.task.v0.models.TaskType
import io.flow.postgresql.Query
import play.api.db.Database

import javax.inject.Inject

object DeleteMetadata {
  val OrganizationSoft: Seq[String] = Seq(
    "public.applications",
    "public.membership_requests",
    "public.memberships",
    "public.organization_attribute_values",
    "public.organization_domains",
    "public.subscriptions"
  )
  val OrganizationHard: Seq[String] = Seq(
    "public.organization_logs", "public.tasks", "search.items"
  )
  val ApplicationSoft: Seq[String] = Seq(
    "public.application_moves", "public.changes", "public.versions", "public.watches"
  )
  val ApplicationHard: Seq[String] = Seq("search.items")
  val VersionSoft: Seq[String] = Seq(
    "cache.services", "public.originals"
  )
  val VersionHard: Seq[String] = Nil
}

class CleanupDeletionsProcessor @Inject()(
  args: TaskProcessorArgs,
  db: Database,
  usersDao: UsersDao
) extends TaskProcessor(args, TaskType.CleanupDeletions) {
  import DeleteMetadata._

  override def processRecord(id: String): ValidatedNec[String, Unit] = {
    organizations()
    applications()
    versions()
    ().validNec
  }


  private[processor] def organizations(): Unit = {
    OrganizationSoft.foreach { table =>
      exec(
        s"""
           |update $table
           |   set deleted_at=now(),deleted_by_guid={deleted_by_guid}::uuid
           | where deleted_at is null
           |   and organization_guid in (select guid from organizations where deleted_at is not null)
           |""".stripMargin
      )
    }

    OrganizationHard.foreach { table =>
      exec(
        s"""
           |delete from $table
           | where organization_guid in (select guid from organizations where deleted_at is not null)
           |""".stripMargin
      )
    }
  }

  private[processor] def applications(): Unit = {
    ApplicationSoft.foreach { table =>
      exec(
        s"""
           |update $table
           |   set deleted_at=now(),deleted_by_guid={deleted_by_guid}::uuid
           | where deleted_at is null
           |   and application_guid in (select guid from applications where deleted_at is not null)
           |""".stripMargin
      )
    }

    ApplicationHard.foreach { table =>
      exec(
        s"""
           |delete from $table
           | where application_guid in (select guid from applications where deleted_at is not null)
           |""".stripMargin
      )
    }
  }

  private[processor] def versions(): Unit = {
    VersionSoft.foreach { table =>
      exec(
        s"""
           |update $table
           |   set deleted_at=now(),deleted_by_guid={deleted_by_guid}::uuid
           | where deleted_at is null
           |   and version_guid in (select guid from versions where deleted_at is not null)
           |""".stripMargin
      )
    }

    VersionHard.foreach { table =>
      exec(
        s"""
           |delete from $table
           | where version_guid in (select guid from versions where deleted_at is not null)
           |""".stripMargin
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