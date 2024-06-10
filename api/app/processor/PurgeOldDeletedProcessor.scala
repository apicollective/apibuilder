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
    delete(Tables.versions, VersionSoft)
    delete(Tables.applications, ApplicationSoft)
    delete(Tables.organizations, OrganizationSoft)
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