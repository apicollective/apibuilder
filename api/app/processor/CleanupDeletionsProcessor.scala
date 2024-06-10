package processor

import cats.data.ValidatedNec
import cats.implicits._
import db.UsersDao
import io.apibuilder.task.v0.models.TaskType
import io.flow.postgresql.Query
import play.api.db.Database

import javax.inject.Inject

sealed trait PrimaryKey {
  def name: String
}
object PrimaryKey {
  case object PkeyString extends PrimaryKey {
    override val name = "id"
  }
  case object PkeyLong extends PrimaryKey {
    override val name = "id"
  }
  case object PkeyUUID extends PrimaryKey {
    override val name = "guid"
  }
}
case class TableMetadata(name: String, pkey: PrimaryKey, references: Seq[TableMetadata])
object Tables {
  val organizations: TableMetadata = TableMetadata("organizations", PrimaryKey.PkeyUUID, Nil)
  val applications: TableMetadata = TableMetadata("applications", PrimaryKey.PkeyUUID, Nil)
  val versions: TableMetadata = TableMetadata("versions", PrimaryKey.PkeyUUID, Nil)
}
object DeleteMetadata {
  private[this] def guidReferencesOrganizations(name: String): TableMetadata = {
    TableMetadata(
      name,
      PrimaryKey.PkeyString,
      Seq(Tables.organizations)
    )
  }
  private[this] def guidReferencesApplications(name: String): TableMetadata = {
    TableMetadata(
      name,
      PrimaryKey.PkeyString,
      Seq(Tables.applications)
    )
  }

  val OrganizationSoft: Seq[TableMetadata] = Seq(
    "public.applications",
    "public.membership_requests",
    "public.memberships",
    "public.organization_attribute_values",
    "public.organization_domains",
    "public.subscriptions"
  ).map(guidReferencesOrganizations)
  val OrganizationHard: Seq[TableMetadata] = Seq(
    "public.organization_logs", "public.tasks", "search.items"
  ).map(guidReferencesOrganizations)
  val ApplicationSoft: Seq[TableMetadata] = Seq(
    "public.application_moves", "public.changes", "public.versions", "public.watches"
  ).map(guidReferencesApplications)
  val ApplicationHard: Seq[TableMetadata] = Seq("search.items").map(guidReferencesApplications)
  val VersionSoft: Seq[TableMetadata] = Seq(
    TableMetadata("cache.services", PrimaryKey.PkeyUUID, Seq(Tables.versions)),
    TableMetadata("public.originals", PrimaryKey.PkeyLong, Seq(Tables.versions)),
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