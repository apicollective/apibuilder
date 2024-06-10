package processor

import anorm._
import cats.data.ValidatedNec
import cats.implicits._
import io.apibuilder.task.v0.models.TaskType
import io.flow.postgresql.Query
import org.joda.time.DateTime
import play.api.db.Database

import javax.inject.Inject
import scala.annotation.tailrec


class PurgeOldDeletedProcessor @Inject()(
  args: TaskProcessorArgs,
  db: Database,
) extends TaskProcessor(args, TaskType.PurgeOldDeleted) {
  import DeleteMetadata._

  override def processRecord(id: String): ValidatedNec[String, Unit] = {
    delete(TableMetadata.guid("versions"), VersionSoft)
    delete(TableMetadata.guid("applications"), ApplicationSoft)
    delete(TableMetadata.guid("organizations"), OrganizationSoft)
    ().validNec
  }

  private[this] val Limit = 1000
  private[this] case class DbRow(pkey: String, deletedAt: DateTime)
  private[this] def nextDeletedRows(table: TableMetadata): Seq[DbRow] = {
    db.withConnection { c =>
      Query(
        s"""
           |select ${table.pkey}::text as pkey, deleted_at
           |  from ${table.name}
           | where deleted_at < now() - interval '45 days'
           | limit 1000
           |""".stripMargin
      ).as(parser.*)(c)
    }
  }

  private[this] val parser: RowParser[DbRow] = {
    SqlParser.get[String]("pkey") ~
      SqlParser.get[DateTime]("deleted_at") map {
      case pkey ~ deletedAt =>
        DbRow(pkey, deletedAt)
    }
  }

  private[processor] def delete(parentTable: TableMetadata, tables: Seq[TableMetadata]): Unit = {
    println(s"starting delete ${parentTable.name}")
    tables.foreach { childTable =>
      println(s"Delete from child ${childTable.name}")
      delete(childTable)
    }
    delete(parentTable)
  }

  private[this] def moveDeletedAtBack(table: TableMetadata, pkey: String): Unit = {
    exec(
      addPkey(table, pkey, Query(s"update $table set deleted_at = deleted_at - interval '45 days'")
        .equals(table.pkey, pkey)
      )
    )
  }

  private[this] val Start: DateTime = DateTime.parse("2024-06-01T12:00:00.000-05:00")
  private[this] val End: DateTime = DateTime.parse("2024-06-01T12:00:00.000-05:00")

  @tailrec
  private[this] def delete(childTable: TableMetadata): Unit = {
    val rows = nextDeletedRows(childTable)
    rows.foreach { row =>
      if (row.deletedAt.isAfter(End) && row.deletedAt.isBefore(Start)) {
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
    val q = table.pkeyType match {
      case PkeyType.PkeyLong => query.and(s"${table.pkey} = {pkey}::bigint")
      case PkeyType.PkeyString => query.and(s"${table.pkey} = {pkey}")
      case PkeyType.PkeyUUID => query.and(s"${table.pkey} = {pkey}::uuid")
    }
    q.bind("pkey", pkey)
  }

  private[this] def exec(q: Query): Unit = {
    db.withConnection { c =>
      q.withDebugging().anormSql().executeUpdate()(c)
    }
    ()
  }
}