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
  }

  private[util] def organizations(): Unit = {
    exec("""
        |update applications
        |   set deleted_at=now(),deleted_by_guid={deleted_by_guid}::uuid
        | where deleted_at is null
        |   and organization_guid in (
        |     select guid from organizations where deleted_at is not null
        |   )
        |""".stripMargin
    )
  }

  private[util] def applications(): Unit = {
    exec("""
        |update versions
        |   set deleted_at=now(),deleted_by_guid={deleted_by_guid}::uuid
        | where deleted_at is null
        |   and application_guid in (
        |     select guid from applications where deleted_at is not null
        |   )
        |""".stripMargin
    )
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