package db.generated

import anorm._
import io.flow.postgresql.{OrderBy, Query}
import java.sql.Connection
import java.util.UUID
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.db.Database
import play.api.libs.json.Json
import util.IdGenerator

case class Migration(
  id: String,
  versionGuid: UUID,
  numAttempts: Long,
  errors: Option[Seq[String]],
  updatedByGuid: String,
  createdAt: DateTime,
  updatedAt: DateTime
) {

  lazy val form: MigrationForm = MigrationForm(
    versionGuid = versionGuid,
    numAttempts = numAttempts,
    errors = errors
  )

}

case class MigrationForm(
  versionGuid: UUID,
  numAttempts: Long,
  errors: Option[Seq[String]]
)

object MigrationsTable {
  val Schema: String = "public"
  val Name: String = "migrations"
  val QualifiedName: String = s"$Schema.$Name"

  object Columns {
    val Id: String = "id"
    val VersionGuid: String = "version_guid"
    val NumAttempts: String = "num_attempts"
    val Errors: String = "errors"
    val UpdatedByGuid: String = "updated_by_guid"
    val CreatedAt: String = "created_at"
    val UpdatedAt: String = "updated_at"
    val HashCode: String = "hash_code"
    val all: List[String] = List(Id, VersionGuid, NumAttempts, Errors, UpdatedByGuid, CreatedAt, UpdatedAt, HashCode)
  }
}

trait BaseMigrationsDao {

  def db: Database

  private[this] val BaseQuery = Query("""
      | select migrations.id,
      |        migrations.version_guid,
      |        migrations.num_attempts,
      |        migrations.errors::text as errors_text,
      |        migrations.updated_by_guid,
      |        migrations.created_at,
      |        migrations.updated_at,
      |        migrations.hash_code
      |   from migrations
  """.stripMargin)

  def findById(id: String): Option[Migration] = {
    db.withConnection { c =>
      findByIdWithConnection(c, id)
    }
  }

  def findByIdWithConnection(c: java.sql.Connection, id: String): Option[Migration] = {
    findAllWithConnection(c, ids = Some(Seq(id)), limit = Some(1L), orderBy = None).headOption
  }

  def iterateAll(
    ids: Option[Seq[String]] = None,
    versionGuid: Option[UUID] = None,
    numAttempts: Option[Long] = None,
    numAttemptsGreaterThanOrEquals: Option[Long] = None,
    numAttemptsGreaterThan: Option[Long] = None,
    numAttemptsLessThanOrEquals: Option[Long] = None,
    numAttemptsLessThan: Option[Long] = None,
    numAttemptses: Option[Seq[Long]] = None,
    createdAt: Option[DateTime] = None,
    createdAtGreaterThanOrEquals: Option[DateTime] = None,
    createdAtGreaterThan: Option[DateTime] = None,
    createdAtLessThanOrEquals: Option[DateTime] = None,
    createdAtLessThan: Option[DateTime] = None,
    numAttemptsCreatedAts: Option[Seq[(Long, DateTime)]] = None,
    pageSize: Long = 2000L,
  ) (
    implicit customQueryModifier: Query => Query = { q => q }
  ): Iterator[Migration] = {
    def iterate(lastValue: Option[Migration]): Iterator[Migration] = {
      val page = findAll(
        ids = ids,
        versionGuid = versionGuid,
        numAttempts = numAttempts,
        numAttemptsGreaterThanOrEquals = numAttemptsGreaterThanOrEquals,
        numAttemptsGreaterThan = numAttemptsGreaterThan,
        numAttemptsLessThanOrEquals = numAttemptsLessThanOrEquals,
        numAttemptsLessThan = numAttemptsLessThan,
        numAttemptses = numAttemptses,
        createdAt = createdAt,
        createdAtGreaterThanOrEquals = createdAtGreaterThanOrEquals,
        createdAtGreaterThan = createdAtGreaterThan,
        createdAtLessThanOrEquals = createdAtLessThanOrEquals,
        createdAtLessThan = createdAtLessThan,
        numAttemptsCreatedAts = numAttemptsCreatedAts,
        limit = Some(pageSize),
        orderBy = Some(OrderBy("migrations.id")),
      ) { q => customQueryModifier(q).greaterThan("migrations.id", lastValue.map(_.id)) }

      page.lastOption match {
        case None => Iterator.empty
        case lastValue => page.iterator ++ iterate(lastValue)
      }
    }

    iterate(None)
  }

  def findAll(
    ids: Option[Seq[String]] = None,
    versionGuid: Option[UUID] = None,
    numAttempts: Option[Long] = None,
    numAttemptsGreaterThanOrEquals: Option[Long] = None,
    numAttemptsGreaterThan: Option[Long] = None,
    numAttemptsLessThanOrEquals: Option[Long] = None,
    numAttemptsLessThan: Option[Long] = None,
    numAttemptses: Option[Seq[Long]] = None,
    createdAt: Option[DateTime] = None,
    createdAtGreaterThanOrEquals: Option[DateTime] = None,
    createdAtGreaterThan: Option[DateTime] = None,
    createdAtLessThanOrEquals: Option[DateTime] = None,
    createdAtLessThan: Option[DateTime] = None,
    numAttemptsCreatedAts: Option[Seq[(Long, DateTime)]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[OrderBy] = Some(OrderBy("migrations.id"))
  ) (
    implicit customQueryModifier: Query => Query = { q => q }
  ): Seq[Migration] = {
    db.withConnection { c =>
      findAllWithConnection(
        c,
        ids = ids,
        versionGuid = versionGuid,
        numAttempts = numAttempts,
        numAttemptsGreaterThanOrEquals = numAttemptsGreaterThanOrEquals,
        numAttemptsGreaterThan = numAttemptsGreaterThan,
        numAttemptsLessThanOrEquals = numAttemptsLessThanOrEquals,
        numAttemptsLessThan = numAttemptsLessThan,
        numAttemptses = numAttemptses,
        createdAt = createdAt,
        createdAtGreaterThanOrEquals = createdAtGreaterThanOrEquals,
        createdAtGreaterThan = createdAtGreaterThan,
        createdAtLessThanOrEquals = createdAtLessThanOrEquals,
        createdAtLessThan = createdAtLessThan,
        numAttemptsCreatedAts = numAttemptsCreatedAts,
        limit = limit,
        offset = offset,
        orderBy = orderBy
      )(customQueryModifier)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    ids: Option[Seq[String]] = None,
    versionGuid: Option[UUID] = None,
    numAttempts: Option[Long] = None,
    numAttemptsGreaterThanOrEquals: Option[Long] = None,
    numAttemptsGreaterThan: Option[Long] = None,
    numAttemptsLessThanOrEquals: Option[Long] = None,
    numAttemptsLessThan: Option[Long] = None,
    numAttemptses: Option[Seq[Long]] = None,
    createdAt: Option[DateTime] = None,
    createdAtGreaterThanOrEquals: Option[DateTime] = None,
    createdAtGreaterThan: Option[DateTime] = None,
    createdAtLessThanOrEquals: Option[DateTime] = None,
    createdAtLessThan: Option[DateTime] = None,
    numAttemptsCreatedAts: Option[Seq[(Long, DateTime)]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[OrderBy] = Some(OrderBy("migrations.id"))
  ) (
    implicit customQueryModifier: Query => Query = { q => q }
  ): Seq[Migration] = {
    customQueryModifier(BaseQuery).
      optionalIn("migrations.id", ids).
      equals("migrations.version_guid", versionGuid).
      equals("migrations.num_attempts", numAttempts).
      greaterThanOrEquals("migrations.num_attempts", numAttemptsGreaterThanOrEquals).
      greaterThan("migrations.num_attempts", numAttemptsGreaterThan).
      lessThanOrEquals("migrations.num_attempts", numAttemptsLessThanOrEquals).
      lessThan("migrations.num_attempts", numAttemptsLessThan).
      optionalIn("migrations.num_attempts", numAttemptses).
      equals("migrations.created_at", createdAt).
      greaterThanOrEquals("migrations.created_at", createdAtGreaterThanOrEquals).
      greaterThan("migrations.created_at", createdAtGreaterThan).
      lessThanOrEquals("migrations.created_at", createdAtLessThanOrEquals).
      lessThan("migrations.created_at", createdAtLessThan).
      optionalIn2(("migrations.num_attempts", "migrations.created_at"), numAttemptsCreatedAts).
      optionalLimit(limit).
      offset(offset).
      orderBy(orderBy.flatMap(_.sql)).
      as(MigrationsDao.parser.*)(c)
  }

}

object MigrationsDao {

  val parser: RowParser[Migration] = {
    SqlParser.str("id") ~
    SqlParser.get[UUID]("version_guid") ~
    SqlParser.long("num_attempts") ~
    SqlParser.str("errors_text").? ~
    SqlParser.str("updated_by_guid") ~
    SqlParser.get[DateTime]("created_at") ~
    SqlParser.get[DateTime]("updated_at") map {
      case id ~ versionGuid ~ numAttempts ~ errors ~ updatedByGuid ~ createdAt ~ updatedAt => Migration(
        id = id,
        versionGuid = versionGuid,
        numAttempts = numAttempts,
        errors = errors.map { text => Json.parse(text).as[Seq[String]] },
        updatedByGuid = updatedByGuid,
        createdAt = createdAt,
        updatedAt = updatedAt
      )
    }
  }

}

@Singleton
class MigrationsDao @Inject() (
  override val db: Database
) extends BaseMigrationsDao {

  private[this] val idGenerator = util.IdGenerator("mig")

  def randomId(): String = idGenerator.randomId()

  private[this] val InsertQuery = Query("""
    | insert into migrations
    | (id, version_guid, num_attempts, errors, updated_by_guid, hash_code)
    | values
    | ({id}, {version_guid}::uuid, {num_attempts}::bigint, {errors}::json, {updated_by_guid}, {hash_code}::bigint)
  """.stripMargin)

  private[this] val UpdateQuery = Query("""
    | update migrations
    |    set version_guid = {version_guid}::uuid,
    |        num_attempts = {num_attempts}::bigint,
    |        errors = {errors}::json,
    |        updated_by_guid = {updated_by_guid},
    |        hash_code = {hash_code}::bigint
    |  where id = {id}
    |    and migrations.hash_code != {hash_code}::bigint
  """.stripMargin)

  private[this] def bindQuery(query: Query, form: MigrationForm): Query = {
    query.
      bind("version_guid", form.versionGuid).
      bind("num_attempts", form.numAttempts).
      bind("errors", form.errors.map { v => Json.toJson(v) }).
      bind("hash_code", form.hashCode())
  }

  private[this] def toNamedParameter(updatedBy: UUID, id: String, form: MigrationForm): Seq[NamedParameter] = {
    Seq(
      scala.Symbol("id") -> id,
      scala.Symbol("version_guid") -> form.versionGuid,
      scala.Symbol("num_attempts") -> form.numAttempts,
      scala.Symbol("errors") -> form.errors.map { v => Json.toJson(v).toString },
      scala.Symbol("updated_by_guid") -> updatedBy,
      scala.Symbol("hash_code") -> form.hashCode()
    )
  }

  def insert(updatedBy: UUID, form: MigrationForm): String = {
    db.withConnection { c =>
      insert(c, updatedBy, form)
    }
  }

  def insert(c: Connection, updatedBy: UUID, form: MigrationForm): String = {
    val id = randomId()
    bindQuery(InsertQuery, form).
      bind("id", id).
      bind("updated_by_guid", updatedBy).
      anormSql.execute()(c)
    id
  }

  def insertBatch(updatedBy: UUID, forms: Seq[MigrationForm]): Seq[String] = {
    db.withConnection { c =>
      insertBatchWithConnection(c, updatedBy, forms)
    }
  }

  def insertBatchWithConnection(c: Connection, updatedBy: UUID, forms: Seq[MigrationForm]): Seq[String] = {
    if (forms.nonEmpty) {
      val ids = forms.map(_ => randomId())
      val params = ids.zip(forms).map { case (id, form) => toNamedParameter(updatedBy, id, form) }
      BatchSql(InsertQuery.sql(), params.head, params.tail: _*).execute()(c)
      ids
    } else {
      Nil
    }
  }

  def updateIfChangedById(updatedBy: UUID, id: String, form: MigrationForm): Unit = {
    if (!findById(id).map(_.form).contains(form)) {
      updateById(updatedBy, id, form)
    }
  }

  def updateById(updatedBy: UUID, id: String, form: MigrationForm): Unit = {
    db.withConnection { c =>
      updateById(c, updatedBy, id, form)
    }
  }

  def updateById(c: Connection, updatedBy: UUID, id: String, form: MigrationForm): Unit = {
    bindQuery(UpdateQuery, form).
      bind("id", id).
      bind("updated_by_guid", updatedBy).
      anormSql.execute()(c)
    ()
  }

  def update(updatedBy: UUID, existing: Migration, form: MigrationForm): Unit = {
    db.withConnection { c =>
      update(c, updatedBy, existing, form)
    }
  }

  def update(c: Connection, updatedBy: UUID, existing: Migration, form: MigrationForm): Unit = {
    updateById(c, updatedBy, existing.id, form)
  }

  def updateBatch(updatedBy: UUID, idsAndForms: Seq[(String, MigrationForm)]): Unit = {
    db.withConnection { c =>
      updateBatchWithConnection(c, updatedBy, idsAndForms)
    }
  }

  def updateBatchWithConnection(c: Connection, updatedBy: UUID, idsAndForms: Seq[(String, MigrationForm)]): Unit = {
    if (idsAndForms.nonEmpty) {
      val params = idsAndForms.map { case (id, form) => toNamedParameter(updatedBy, id, form) }
      BatchSql(UpdateQuery.sql(), params.head, params.tail: _*).execute()(c)
      ()
    }
  }

  def delete(deletedBy: UUID, migration: Migration): Unit = {
    db.withConnection { c =>
      delete(c, deletedBy, migration)
    }
  }

  def delete(c: Connection, deletedBy: UUID, migration: Migration): Unit = {
    deleteById(c, deletedBy, migration.id)
  }

  def deleteById(deletedBy: UUID, id: String): Unit = {
    db.withConnection { c =>
      deleteById(c, deletedBy, id)
    }
  }

  def deleteById(c: Connection, deletedBy: UUID, id: String): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from migrations")
      .equals("id", id)
      .anormSql.executeUpdate()(c)
      ()
  }

  def deleteAllByIds(deletedBy: UUID, ids: Seq[String]): Unit = {
    db.withConnection { c =>
      deleteAllByIds(c, deletedBy, ids)
    }
  }

  def deleteAllByIds(c: Connection, deletedBy: UUID, ids: Seq[String]): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from migrations")
      .in("id", ids)
      .anormSql.executeUpdate()(c)
      ()
  }

  def deleteAllByNumAttempts(deletedBy: UUID, numAttempts: Long): Unit = {
    db.withConnection { c =>
      deleteAllByNumAttempts(c, deletedBy, numAttempts)
    }
  }

  def deleteAllByNumAttempts(c: Connection, deletedBy: UUID, numAttempts: Long): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from migrations")
      .equals("num_attempts", numAttempts)
      .anormSql.executeUpdate()(c)
      ()
  }

  def deleteAllByNumAttemptses(deletedBy: UUID, numAttemptses: Seq[Long]): Unit = {
    db.withConnection { c =>
      deleteAllByNumAttemptses(c, deletedBy, numAttemptses)
    }
  }

  def deleteAllByNumAttemptses(c: Connection, deletedBy: UUID, numAttemptses: Seq[Long]): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from migrations")
      .in("num_attempts", numAttemptses)
      .anormSql.executeUpdate()(c)
      ()
  }

  def deleteAllByNumAttemptsAndCreatedAt(deletedBy: UUID, numAttempts: Long, createdAt: DateTime): Unit = {
    db.withConnection { c =>
      deleteAllByNumAttemptsAndCreatedAt(c, deletedBy, numAttempts, createdAt)
    }
  }

  def deleteAllByNumAttemptsAndCreatedAt(c: Connection, deletedBy: UUID, numAttempts: Long, createdAt: DateTime): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from migrations")
      .equals("num_attempts", numAttempts)
      .equals("created_at", createdAt)
      .anormSql.executeUpdate()(c)
      ()
  }

  def deleteAllByNumAttemptsAndCreatedAts(deletedBy: UUID, numAttempts: Long, createdAts: Seq[DateTime]): Unit = {
    db.withConnection { c =>
      deleteAllByNumAttemptsAndCreatedAts(c, deletedBy, numAttempts, createdAts)
    }
  }

  def deleteAllByNumAttemptsAndCreatedAts(c: Connection, deletedBy: UUID, numAttempts: Long, createdAts: Seq[DateTime]): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from migrations")
      .equals("num_attempts", numAttempts)
      .in("created_at", createdAts)
      .anormSql.executeUpdate()(c)
      ()
  }

  def deleteAllByVersionGuid(deletedBy: UUID, versionGuid: UUID): Unit = {
    db.withConnection { c =>
      deleteAllByVersionGuid(c, deletedBy, versionGuid)
    }
  }

  def deleteAllByVersionGuid(c: Connection, deletedBy: UUID, versionGuid: UUID): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from migrations")
      .equals("version_guid", versionGuid)
      .anormSql.executeUpdate()(c)
      ()
  }

  def deleteAllByVersionGuids(deletedBy: UUID, versionGuids: Seq[UUID]): Unit = {
    db.withConnection { c =>
      deleteAllByVersionGuids(c, deletedBy, versionGuids)
    }
  }

  def deleteAllByVersionGuids(c: Connection, deletedBy: UUID, versionGuids: Seq[UUID]): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from migrations")
      .in("version_guid", versionGuids)
      .anormSql.executeUpdate()(c)
      ()
  }

  def setJournalDeletedByUserId(c: Connection, deletedBy: UUID): Unit = {
    anorm.SQL(s"SET journal.deleted_by_user_id = '${deletedBy}'").executeUpdate()(c)
    ()
  }

}