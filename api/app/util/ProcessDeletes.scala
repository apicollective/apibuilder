package util

import db.UsersDao
import io.flow.postgresql.Query
import play.api.db.Database

import javax.inject.Inject

class ProcessDeletes @Inject() (
                               db: Database,
                               usersDao: UsersDao
                               ) {

  def all(): Unit = {
    organizations()
    applications()
    versions()
  }

  private[util] def organizations(): Unit = {
    Seq("applications", "memberships", "membership_requests", "organization_attribute_values", "organization_domains", "subscriptions").foreach { table =>
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
    Seq("versions", "watches").foreach { table =>
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
    Seq("originals", "cache.services").foreach { table =>
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
        .withDebugging()
        .anormSql()
        .executeUpdate()(c)
    }
    ()
  }
}