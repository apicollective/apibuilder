package processor

import cats.data.ValidatedNec
import cats.implicits._
import db.UsersDao
import io.apibuilder.task.v0.models.TaskType
import io.flow.postgresql.Query
import play.api.db.Database

import javax.inject.Inject

sealed trait PkeyType
object PkeyType {
  case object PkeyString extends PkeyType
  case object PkeyLong extends PkeyType
  case object PkeyUUID extends PkeyType
}
case class TableMetadata(name: String, pkey: String, pkeyType: PkeyType)
object TableMetadata {
  def guid(name: String): TableMetadata = TableMetadata(name, "guid", PkeyType.PkeyUUID)
  def long(name: String): TableMetadata = TableMetadata(name, "id", PkeyType.PkeyLong)
  def string(name: String): TableMetadata = TableMetadata(name, "id", PkeyType.PkeyString)
}
object DeleteMetadata {
  private[this] def guids(names: String*): Seq[TableMetadata] = {
    names.map { n => TableMetadata.guid(n) }
  }
  val OrganizationSoft: Seq[TableMetadata] = guids(
    "public.applications",
    "public.membership_requests",
    "public.memberships",
    "public.organization_attribute_values",
    "public.organization_domains",
    "public.subscriptions"
  )
  val OrganizationHard: Seq[TableMetadata] = guids(
    "public.organization_logs", "public.tasks", "search.items"
  )
  val ApplicationSoft: Seq[TableMetadata] = guids(
    "public.application_moves", "public.changes", "public.versions", "public.watches"
  )
  val ApplicationHard: Seq[TableMetadata] = guids("search.items")
  val VersionSoft: Seq[TableMetadata] = Seq(
    TableMetadata.guid("cache.services"),
    TableMetadata.long("public.originals")
  )
  val VersionHard: Seq[TableMetadata] = Nil
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
    OrganizationSoft.map(_.name).foreach { table =>
      exec(
        s"""
           |update $table
           |   set deleted_at=now(),deleted_by_guid={deleted_by_guid}::uuid
           | where deleted_at is null
           |   and organization_guid in (select guid from organizations where deleted_at is not null)
           |""".stripMargin
      )
    }

    OrganizationHard.map(_.name).foreach { table =>
      exec(
        s"""
           |delete from $table
           | where organization_guid in (select guid from organizations where deleted_at is not null)
           |""".stripMargin
      )
    }
  }

  private[processor] def applications(): Unit = {
    ApplicationSoft.map(_.name).foreach { table =>
      exec(
        s"""
           |update $table
           |   set deleted_at=now(),deleted_by_guid={deleted_by_guid}::uuid
           | where deleted_at is null
           |   and application_guid in (select guid from applications where deleted_at is not null)
           |""".stripMargin
      )
    }

    ApplicationHard.map(_.name).foreach { table =>
      exec(
        s"""
           |delete from $table
           | where application_guid in (select guid from applications where deleted_at is not null)
           |""".stripMargin
      )
    }
  }

  private[processor] def versions(): Unit = {
    VersionSoft.map(_.name).foreach { table =>
      exec(
        s"""
           |update $table
           |   set deleted_at=now(),deleted_by_guid={deleted_by_guid}::uuid
           | where deleted_at is null
           |   and version_guid in (select guid from versions where deleted_at is not null)
           |""".stripMargin
      )
    }

    VersionHard.map(_.name).foreach { table =>
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