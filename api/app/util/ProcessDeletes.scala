package util

import db.UsersDao
import io.flow.postgresql.Query
import play.api.db.Database

import javax.inject.Inject

object ProcessDeletes {
  val OrganizationChildren: Seq[String] = Seq(
    "public.applications",
    "public.membership_requests",
    "public.memberships",
    "public.organization_attribute_values",
    "public.organization_domains",
    "public.subscriptions"
  )
  val ApplicationChildren: Seq[String] = Seq(
    "public.changes", "public.versions", "public.watches"
  )
  val VersionChildren = Seq(
    "cache.services", "public.originals"
  )
}

class ProcessDeletes @Inject() (
                               db: Database,
                               usersDao: UsersDao
                               ) {
  import ProcessDeletes._

  def all(): Unit = {
    organizations()
    applications()
    versions()
  }

  private[util] def organizations(): Unit = {
    OrganizationChildren.foreach { table =>
      exec(
        s"""
          |update $table
          |   set deleted_at=now(),deleted_by_guid={deleted_by_guid}::uuid
          | where deleted_at is null
          |   and organization_guid in (select guid from organizations where deleted_at is not null)
          |""".stripMargin
      )
    }
    Seq("organization_logs").foreach { table =>
      exec(
        s"""
           |delete from $table
           | where organization_guid in (select guid from organizations where deleted_at is not null)
           |""".stripMargin
      )
    }
  }

  private[util] def applications(): Unit = {
    ApplicationChildren.foreach { table =>
      exec(
        s"""
          |update $table
          |   set deleted_at=now(),deleted_by_guid={deleted_by_guid}::uuid
          | where deleted_at is null
          |   and application_guid in (select guid from applications where deleted_at is not null)
          |""".stripMargin
      )
    }
  }

  private[util] def versions(): Unit = {
    exec("""
       |delete from migrations
       | where version_guid in (select guid from versions where deleted_at is not null)
       |""".stripMargin
    )

    VersionChildren.foreach { table =>
      exec(
        s"""
           |update $table
           |   set deleted_at=now(),deleted_by_guid={deleted_by_guid}::uuid
           | where deleted_at is null
           |   and version_guid in (select guid from versions where deleted_at is not null)
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