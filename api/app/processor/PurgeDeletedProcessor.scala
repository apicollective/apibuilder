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
import scala.collection.concurrent.TrieMap


class PurgeDeletedProcessor @Inject()(
  args: TaskProcessorArgs,
  db: Database,
  usersDao: UsersDao,
) extends TaskProcessor(args, TaskType.PurgeDeleted) {

  override def processRecord(id: String): ValidatedNec[String, Unit] = {
    softDelete(Tables.applications)(_.in("organization_guid", Query("select guid from organizations where deleted_at is not null")))
    softDelete(Tables.versions)(_.in("application_guid", Query("select guid from applications where deleted_at is not null")))

    deleteAll(Tables.versions) { row =>
      delete(Table.guid("cache", "services"))(_.equals("version_guid", row.pkey))
      delete(Table.long("public", "originals"))(_.equals("version_guid", row.pkey))
      delete(Table.guid("public", "changes"))(_.equals("from_version_guid", row.pkey))
      delete(Table.guid("public", "changes"))(_.equals("to_version_guid", row.pkey))
    }

    deleteAll(Tables.applications) { row =>
      delete(Table.guid("public", "application_moves"))(_.equals("application_guid", row.pkey))
      delete(Table.guid("public", "changes"))(_.equals("application_guid", row.pkey))
      delete(Table.guid("search", "items"))(_.equals("application_guid", row.pkey))
      delete(Table.guid("public", "watches"))(_.equals("application_guid", row.pkey))
      delete(Table.guid("public", "versions"))(_.equals("application_guid", row.pkey))
    }

    deleteAll(Tables.organizations) { row =>
      delete(Table.guid("public", "application_moves"))(_.equals("from_organization_guid", row.pkey))
      delete(Table.guid("public", "application_moves"))(_.equals("to_organization_guid", row.pkey))
      delete(Table.guid("search", "items"))(_.equals("organization_guid", row.pkey))
      delete(Table.guid("public", "membership_requests"))(_.equals("organization_guid", row.pkey))
      delete(Table.guid("public", "memberships"))(_.equals("organization_guid", row.pkey))
      delete(Table.guid("public", "organization_attribute_values"))(_.equals("organization_guid", row.pkey))
      delete(Table.guid("public", "organization_domains"))(_.equals("organization_guid", row.pkey))
      delete(Table.guid("public", "organization_logs"))(_.equals("organization_guid", row.pkey))
      delete(Table.guid("public", "applications"))(_.equals("organization_guid", row.pkey))
      delete(Table.guid("public", "subscriptions"))(_.equals("organization_guid", row.pkey))
    }

    ().validNec
  }

  @tailrec
  private def deleteAll(table: Table)(f: DbRow[_] => Any): Unit =  {
    val all = nextDeletedRows(table)
    if (all.nonEmpty) {
      all.foreach { row =>
        f(row)
        delete(table)(_.equals(table.pkey.name, row.pkey))
      }
      if (all.length >= Limit) {
        deleteAll(table)(f)
      }
    }
  }

  private val Limit = 1000
  private case class DbRow[T](pkey: T, deletedAt: DateTime)
  private def nextDeletedRows(table: Table): Seq[DbRow[_]] = {
    db.withConnection { c =>
      Query(
        s"select ${table.pkey.name}::text as pkey, deleted_at from ${table.qualified}"
      ).isNotNull("deleted_at")
        .limit(Limit)
        .as(parser(table.pkey).*)(c)
    }
  }

  private def parser(primaryKey: PrimaryKey): RowParser[DbRow[_]] = {
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

  private val deletedAtCache = TrieMap[Table, Boolean]()
  private val DeletedAtQuery = Query(
    """
      |select count(*)
      |  from information_schema.columns
      | where table_schema = {table_schema}
      |   and table_name = {table_name}
      |   and column_name = 'deleted_at'
      |""".stripMargin)
  private def hasDeletedAt(table: Table): Boolean = {
    deletedAtCache.getOrElseUpdate(table, {
      db.withConnection { c =>
        DeletedAtQuery
          .bind("table_schema", table.schema)
          .bind("table_name", table.name)
          .as(SqlParser.int(1).single)(c)
      } > 0
    })
  }

  private def softDelete(table: Table)(filter: Query => Query): Unit = {
    exec(
      filter(Query(
        s"update ${table.qualified} set deleted_at = now() - interval '45 days', deleted_by_guid = {deleted_by_guid}::uuid"
      ).bind("deleted_by_guid", usersDao.AdminUser.guid)
        .and("(deleted_at is null or deleted_at > now() - interval '32 days')")
      ))
  }

  private def delete(table: Table)(filter: Query => Query): Unit = {
    if (hasDeletedAt(table)) {
      softDelete(table)(filter)
    }

    exec(
      filter(Query(s"delete from ${table.qualified}"))
    )
  }

  private def exec(q: Query): Unit = {
    db.withConnection { c =>
      q.anormSql().executeUpdate()(c)
    }
    ()
  }
}
