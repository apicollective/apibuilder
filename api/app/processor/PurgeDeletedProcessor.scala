package processor

import anorm._
import cats.implicits._
import cats.data.ValidatedNec
import db.UsersDao
import io.apibuilder.task.v0.models.TaskType
import io.flow.postgresql.Query
import org.joda.time.DateTime
import play.api.db.Database

import javax.inject.Inject
import scala.annotation.tailrec


class PurgeDeletedProcessor @Inject()(
  args: TaskProcessorArgs,
  db: Database,
  usersDao: UsersDao,
) extends TaskProcessor(args, TaskType.PurgeOldDeleted) {

  override def processRecord(id: String): ValidatedNec[String, Unit] = {
    delete(TableMetadata.guid("cache.services"))
    delete(TableMetadata.long("public.originals"))
    exec(Query(
      """
        |update changes
        |   set deleted_at = now(),
        |       deleted_by_guid = {deleted_by_guid}::uuid
        | where deleted_at is null
        |   and from_version_guid in (select guid from versions where deleted_at is not null)
        |""".stripMargin).bind("deleted_by_guid", usersDao.AdminUser.guid)
    )
    exec(Query(
      """
        |update changes
        |   set deleted_at = now(),
        |       deleted_by_guid = {deleted_by_guid}::uuid
        | where deleted_at is null
        |   and to_version_guid in (select guid from versions where deleted_at is not null)
        |""".stripMargin).bind("deleted_by_guid", usersDao.AdminUser.guid)
    )
    delete(TableMetadata.guid("public.changes"))

    delete(Tables.versions)
    //delete(Tables.applications)
    //delete(Tables.organizations)
    ().validNec
  }

  private[this] val Limit = 1000
  private[this] case class DbRow(pkey: String, deletedAt: DateTime)
  private[this] def nextDeletedRows(table: TableMetadata): Seq[DbRow] = {
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

  private[this] def moveDeletedAtBack(table: TableMetadata, pkey: String): Unit = {
    exec(
      addPkey(table, pkey, Query(s"update ${table.name} set deleted_at = deleted_at - interval '45 days'"))
    )
  }

  @tailrec
  private[this] def delete(childTable: TableMetadata): Unit = {
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

  private[this] def addPkey(table: TableMetadata, pkey: String, query: Query): Query =  {
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