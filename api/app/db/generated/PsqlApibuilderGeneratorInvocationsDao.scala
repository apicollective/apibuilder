package db.generated

import anorm._
import io.flow.postgresql.{OrderBy, Query}
import io.flow.util.IdGenerator
import java.sql.Connection
import java.util.UUID
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.db.Database

case class GeneratorInvocation(
  id: String,
  key: String,
  updatedByGuid: String,
  createdAt: DateTime,
  updatedAt: DateTime
) {

  lazy val form: GeneratorInvocationForm = GeneratorInvocationForm(
    key = key
  )

}

case class GeneratorInvocationForm(
  key: String
)

object GeneratorInvocationsTable {
  val Schema: String = "public"
  val Name: String = "generator_invocations"

  object Columns {
    val Id: String = "id"
    val Key: String = "key"
    val UpdatedByGuid: String = "updated_by_guid"
    val CreatedAt: String = "created_at"
    val UpdatedAt: String = "updated_at"
    val HashCode: String = "hash_code"
    val all: List[String] = List(Id, Key, UpdatedByGuid, CreatedAt, UpdatedAt, HashCode)
  }
}

@Singleton
class GeneratorInvocationsDao @Inject() (
  val db: Database
) {

  private[this] val idGenerator = IdGenerator("gni")

  def randomId(): String = idGenerator.randomId()

  private[this] val BaseQuery = Query("""
      | select generator_invocations.id,
      |        generator_invocations.key,
      |        generator_invocations.updated_by_guid,
      |        generator_invocations.created_at,
      |        generator_invocations.updated_at,
      |        generator_invocations.hash_code
      |   from generator_invocations
  """.stripMargin)

  private[this] val InsertQuery = Query("""
    | insert into generator_invocations
    | (id, key, updated_by_guid, hash_code)
    | values
    | ({id}, {key}, {updated_by_guid}, {hash_code}::bigint)
  """.stripMargin)

  private[this] val UpdateQuery = Query("""
    | update generator_invocations
    |    set key = {key},
    |        updated_by_guid = {updated_by_guid},
    |        hash_code = {hash_code}::bigint
    |  where id = {id}
    |    and generator_invocations.hash_code != {hash_code}::bigint
  """.stripMargin)

  private[this] def bindQuery(query: Query, form: GeneratorInvocationForm): Query = {
    query.
      bind("key", form.key).
      bind("hash_code", form.hashCode())
  }

  def insert(updatedBy: UUID, form: GeneratorInvocationForm): String = {
    db.withConnection { c =>
      insert(c, updatedBy, form)
    }
  }

  def insert(c: Connection, updatedBy: UUID, form: GeneratorInvocationForm): String = {
    val id = randomId()
    bindQuery(InsertQuery, form).
      bind("id", id).
      bind("updated_by_guid", updatedBy).
      anormSql.execute()(c)
    id
  }

  def updateIfChangedById(updatedBy: UUID, id: String, form: GeneratorInvocationForm): Unit = {
    if (!findById(id).map(_.form).contains(form)) {
      updateById(updatedBy, id, form)
    }
  }

  def updateById(updatedBy: UUID, id: String, form: GeneratorInvocationForm): Unit = {
    db.withConnection { c =>
      updateById(c, updatedBy, id, form)
    }
  }

  def updateById(c: Connection, updatedBy: UUID, id: String, form: GeneratorInvocationForm): Unit = {
    bindQuery(UpdateQuery, form).
      bind("id", id).
      bind("updated_by_guid", updatedBy).
      anormSql.execute()(c)
    ()
  }

  def update(updatedBy: UUID, existing: GeneratorInvocation, form: GeneratorInvocationForm): Unit = {
    db.withConnection { c =>
      update(c, updatedBy, existing, form)
    }
  }

  def update(c: Connection, updatedBy: UUID, existing: GeneratorInvocation, form: GeneratorInvocationForm): Unit = {
    updateById(c, updatedBy, existing.id, form)
  }

  def findById(id: String): Option[GeneratorInvocation] = {
    db.withConnection { c =>
      findByIdWithConnection(c, id)
    }
  }

  def findByIdWithConnection(c: java.sql.Connection, id: String): Option[GeneratorInvocation] = {
    findAllWithConnection(c, ids = Some(Seq(id)), limit = Some(1L)).headOption
  }

  def iterateAll(
    ids: Option[Seq[String]] = None,
    pageSize: Long = 2000L,
  ) (
    implicit customQueryModifier: Query => Query = { q => q }
  ): Iterator[GeneratorInvocation] = {
    def iterate(lastValue: Option[GeneratorInvocation]): Iterator[GeneratorInvocation] = {
      val page = findAll(
        ids = ids,
        limit = Some(pageSize),
        orderBy = OrderBy("generator_invocations.id"),
      ) { q => customQueryModifier(q).greaterThan("generator_invocations.id", lastValue.map(_.id)) }

      page.lastOption match {
        case None => Iterator.empty
        case lastValue => page.iterator ++ iterate(lastValue)
      }
    }

    iterate(None)
  }

  def findAll(
    ids: Option[Seq[String]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: OrderBy = OrderBy("generator_invocations.id")
  ) (
    implicit customQueryModifier: Query => Query = { q => q }
  ): Seq[GeneratorInvocation] = {
    db.withConnection { c =>
      findAllWithConnection(
        c,
        ids = ids,
        limit = limit,
        offset = offset,
        orderBy = orderBy
      )(customQueryModifier)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    ids: Option[Seq[String]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: OrderBy = OrderBy("generator_invocations.id")
  ) (
    implicit customQueryModifier: Query => Query = { q => q }
  ): Seq[GeneratorInvocation] = {
    customQueryModifier(BaseQuery).
      optionalIn("generator_invocations.id", ids).
      optionalLimit(limit).
      offset(offset).
      orderBy(orderBy.sql).
      as(GeneratorInvocationsDao.parser.*)(c)
  }

  def delete(deletedBy: UUID, generatorInvocation: GeneratorInvocation): Unit = {
    db.withConnection { c =>
      delete(c, deletedBy, generatorInvocation)
    }
  }

  def delete(c: Connection, deletedBy: UUID, generatorInvocation: GeneratorInvocation): Unit = {
    deleteById(c, deletedBy, generatorInvocation.id)
  }

  def deleteById(deletedBy: UUID, id: String): Unit = {
    db.withConnection { c =>
      deleteById(c, deletedBy, id)
    }
  }

  def deleteById(c: Connection, deletedBy: UUID, id: String): Unit = {
    setJournalDeletedByUserId(c, deletedBy)
    Query("delete from generator_invocations")
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
    Query("delete from generator_invocations")
      .in("id", ids)
      .anormSql.executeUpdate()(c)
      ()
  }

  def setJournalDeletedByUserId(c: Connection, deletedBy: UUID): Unit = {
    anorm.SQL(s"SET journal.deleted_by_user_id = '${deletedBy}'").executeUpdate()(c)
    ()
  }

}

object GeneratorInvocationsDao {

  val parser: RowParser[GeneratorInvocation] = {
    SqlParser.str("id") ~
    SqlParser.str("key") ~
    SqlParser.str("updated_by_guid") ~
    SqlParser.get[DateTime]("created_at") ~
    SqlParser.get[DateTime]("updated_at") map {
      case id ~ key ~ updatedByGuid ~ createdAt ~ updatedAt => GeneratorInvocation(
        id = id,
        key = key,
        updatedByGuid = updatedByGuid,
        createdAt = createdAt,
        updatedAt = updatedAt
      )
    }
  }

}