package processor

import anorm._
import cats.implicits._
import cats.data.ValidatedNec
import db.UsersDao
import io.apibuilder.task.v0.models.TaskType
import io.flow.postgresql.Query
import org.joda.time.DateTime
import play.api.db.Database
import util.{PrimaryKey, Table, Tables}

import javax.inject.Inject
import scala.annotation.tailrec


class PurgeDeletedProcessor @Inject()(
  args: TaskProcessorArgs,
  db: Database,
  usersDao: UsersDao,
) extends TaskProcessor(args, TaskType.PurgeOldDeleted) {

  override def processRecord(id: String): ValidatedNec[String, Unit] = {
    def versionFilter(column: String) = filter(Tables.versions)(column)
    softDelete(Table.guid("cache.services"), versionFilter("version_guid"))
    softDelete(Table.long("public.originals"), versionFilter("version_guid"))
    softDelete(Table.guid("public.changes"), versionFilter("from_version_guid"))
    softDelete(Table.guid("public.changes"), versionFilter("to_version_guid"))
    delete(Tables.versions)

    def appFilter(column: String) = filter(Tables.applications)(column)
    softDelete(Table.guid("public.application_moves"), appFilter("application_guid"))
    softDelete(Table.guid("public.changes"), appFilter("application_guid"))
    hardDelete(Table.guid("search.items"), appFilter("application_guid"))
    softDelete(Table.guid("public.watches"), appFilter("application_guid"))
    softDelete(Table.guid("public.versions"), appFilter("application_guid"))
    delete(Tables.applications)

    def orgFilter(column: String) = filter(Tables.organizations)(column)
    softDelete(Table.guid("public.application_moves"), orgFilter("from_organization_guid"))
    softDelete(Table.guid("public.application_moves"), orgFilter("to_organization_guid"))
    hardDelete(Table.guid("search.items"), orgFilter("organization_guid"))
    softDelete(Table.guid("public.membership_requests"), orgFilter("organization_guid"))
    softDelete(Table.guid("public.memberships"), orgFilter("organization_guid"))
    softDelete(Table.guid("public.organization_attribute_values"), orgFilter("organization_guid"))
    softDelete(Table.guid("public.organization_domains"), orgFilter("organization_guid"))
    hardDelete(Table.guid("public.organization_logs"), orgFilter("organization_guid"))
    softDelete(Table.guid("public.applications"), orgFilter("organization_guid"))
    delete(Tables.organizations)
    ().validNec
  }

  private[this] def filter(table: Table)(column: String) = s"$column in (select ${table.pkey.name} from ${table.name} where deleted_at is not null)"

  private[this] val Limit = 1000
  private[this] case class DbRow(pkey: String, deletedAt: DateTime)
  private[this] def nextDeletedRows(table: Table): Seq[DbRow] = {
    db.withConnection { c =>
      Query(
        s"""
           |select ${table.pkey.name}::text as pkey, deleted_at
           |  from ${table.name}
           | where deleted_at is not null
           | limit 1000
           |""".stripMargin
      ).withDebugging().as(parser.*)(c)
    }
  }

  private[this] val parser: RowParser[DbRow] = {
    SqlParser.get[String]("pkey") ~
      SqlParser.get[DateTime]("deleted_at") map {
      case pkey ~ deletedAt =>
        DbRow(pkey, deletedAt)
    }
  }

  private[this] def moveDeletedAtBack(table: Table, pkey: String): Unit = {
    exec(
      addPkey(table, pkey, Query(s"update ${table.name} set deleted_at = deleted_at - interval '45 days'"))
    )
  }

  @tailrec
  private[this] def delete(childTable: Table): Unit = {
    val rows = nextDeletedRows(childTable)
    rows.foreach { row =>
      if (row.deletedAt.isAfter(DateTime.now.minusDays(35))) {
        // Temporarily work around triggers
        moveDeletedAtBack(childTable, row.pkey)
      }

      exec(
        addPkey(childTable, row.pkey, Query(s"delete from ${childTable.name}"))
      )
    }
    if (rows.length >= Limit) {
      delete(childTable)
    }
  }

  private[this] def softDelete(table: Table, filter: String): Unit = {
    exec(
      Query(
        s"""
          |update ${table.name}
          |   set deleted_at = now(), deleted_by_guid = {deleted_by_guid}::uuid
          | where deleted_at is null
          |   and $filter
          |""".stripMargin
      ).bind("deleted_by_guid", usersDao.AdminUser.guid)
    )
    delete(table)
  }
  private[this] def hardDelete(table: Table, filter: String): Unit = {
    exec(
      Query(s"delete from ${table.name} where $filter")
    )
  }

  private[this] def addPkey(table: Table, pkey: String, query: Query): Query =  {
    val clause = table.pkey match {
      case PrimaryKey.PkeyLong => "{pkey}::bigint"
      case PrimaryKey.PkeyString => "{pkey}"
      case PrimaryKey.PkeyUUID => "{pkey}::uuid"
    }
    query.and(s"${table.pkey.name} = $clause").bind("pkey", pkey)
  }

  private[this] def exec(q: Query): Unit = {
    db.withConnection { c =>
      q.withDebugging().anormSql().executeUpdate()(c)
    }
    ()
  }
}