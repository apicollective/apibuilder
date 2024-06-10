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

import java.util.UUID
import javax.inject.Inject
import scala.annotation.tailrec


class PurgeDeletedProcessor @Inject()(
  args: TaskProcessorArgs,
  db: Database,
  usersDao: UsersDao,
) extends TaskProcessor(args, TaskType.PurgeOldDeleted) {

  override def processRecord(id: String): ValidatedNec[String, Unit] = {
    deleteAll(Tables.versions) { row =>
      softDelete(Table.guid("cache.services"))(_.equals("version_guid", row.pkey))
      softDelete(Table.long("public.originals"))(_.equals("version_guid", row.pkey))
      softDelete(Table.guid("public.changes"))(_.equals("from_version_guid", row.pkey))
      softDelete(Table.guid("public.changes"))(_.equals("to_version_guid", row.pkey))
    }

    deleteAll(Tables.applications) { row =>
      softDelete(Table.guid("public.application_moves"))(_.equals("application_guid", row.pkey))
      softDelete(Table.guid("public.changes"))(_.equals("application_guid", row.pkey))
      hardDelete(Table.guid("search.items"))(_.equals("application_guid", row.pkey))
      softDelete(Table.guid("public.watches"))(_.equals("application_guid", row.pkey))
      softDelete(Table.guid("public.versions"))(_.equals("application_guid", row.pkey))
    }

    deleteAll(Tables.organizations) { row =>
      softDelete(Table.guid("public.application_moves"))(_.equals("from_organization_guid", row.pkey))
      softDelete(Table.guid("public.application_moves"))(_.equals("to_organization_guid", row.pkey))
      hardDelete(Table.guid("search.items"))(_.equals("organization_guid", row.pkey))
      softDelete(Table.guid("public.membership_requests"))(_.equals("organization_guid", row.pkey))
      softDelete(Table.guid("public.memberships"))(_.equals("organization_guid", row.pkey))
      softDelete(Table.guid("public.organization_attribute_values"))(_.equals("organization_guid", row.pkey))
      softDelete(Table.guid("public.organization_domains"))(_.equals("organization_guid", row.pkey))
      hardDelete(Table.guid("public.organization_logs"))(_.equals("organization_guid", row.pkey))
      softDelete(Table.guid("public.applications"))(_.equals("organization_guid", row.pkey))
    }

    ().validNec
  }

  @tailrec
  private[this] def deleteAll(table: Table)(f: DbRow[_] => Any): Unit =  {
    val all = nextDeletedRows(table)
    if (all.nonEmpty) {
      all.foreach(f)
      if (all.length >= Limit) {
        deleteAll(table)(f)
      } else {
        delete(table)
      }
    }
  }

  private[this] val Limit = 1000
  private[this] case class DbRow[T](pkey: T, deletedAt: DateTime)
  private[this] def nextDeletedRows(table: Table): Seq[DbRow[_]] = {
    db.withConnection { c =>
      Query(
        s"select ${table.pkey.name}::text as pkey, deleted_at from ${table.name}"
      ).isNotNull("deleted_at")
        .limit(Limit)
        .withDebugging()
        .as(parser(table.pkey).*)(c)
    }
  }

  private[this] def parser(primaryKey: PrimaryKey): RowParser[DbRow[_]] = {
    import PrimaryKey._
    SqlParser.get[String]("pkey") ~
      SqlParser.get[DateTime]("deleted_at") map {
      case pkey ~ deletedAt =>
        val pkeyValue = primaryKey match {
          case PkeyUUID => UUID.fromString(pkey)
          case PkeyString => pkey
          case PkeyLong => BigInt(pkey)
        }
        DbRow(pkeyValue, deletedAt)
    }
  }

  private[this] def moveDeletedAtBack(table: Table, pkey: Any): Unit = {
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

  private[this] def softDelete(table: Table)(filter: Query => Query): Unit = {
    exec(filter(Query(
        s"""
          |update ${table.name}
          |   set deleted_at = now() - interval '45 days', deleted_by_guid = {deleted_by_guid}::uuid
          |""".stripMargin
      ).and("deleted_at is null")
      .bind("deleted_by_guid", usersDao.AdminUser.guid)
    ))
  }

  private[this] def hardDelete(table: Table)(filter: Query => Query): Unit = {
    exec(
      filter(Query(s"delete from ${table.name}"))
    )
  }

  private[this] def addPkey(table: Table, pkey: Any, query: Query): Query =  {
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