package db.generated

case class GeneratorInvocation(
  id: String,
  key: String,
  organizationKey: Option[String],
  applicationKey: Option[String],
  createdAt: org.joda.time.DateTime,
  updatedAt: org.joda.time.DateTime,
  updatedByGuid: String
) {
  def form: GeneratorInvocationForm = {
    GeneratorInvocationForm(
      key = key,
      organizationKey = organizationKey,
      applicationKey = applicationKey,
    )
  }
}

case class GeneratorInvocationForm(
  key: String,
  organizationKey: Option[String],
  applicationKey: Option[String]
)

case object GeneratorInvocationsTable {
  val SchemaName: String = "public"

  val TableName: String = "generator_invocations"

  val QualifiedName: String = "public.generator_invocations"

  sealed trait Column {
    def name: String
  }

  object Columns {
    case object Id extends Column {
      override val name: String = "id"
    }

    case object Key extends Column {
      override val name: String = "key"
    }

    case object OrganizationKey extends Column {
      override val name: String = "organization_key"
    }

    case object ApplicationKey extends Column {
      override val name: String = "application_key"
    }

    case object CreatedAt extends Column {
      override val name: String = "created_at"
    }

    case object UpdatedAt extends Column {
      override val name: String = "updated_at"
    }

    case object UpdatedByGuid extends Column {
      override val name: String = "updated_by_guid"
    }

    case object HashCode extends Column {
      override val name: String = "hash_code"
    }

    val all: List[Column] = List(Id, Key, OrganizationKey, ApplicationKey, CreatedAt, UpdatedAt, UpdatedByGuid, HashCode)
  }
}

trait BaseGeneratorInvocationsDao {
  import anorm.*

  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def db: play.api.db.Database

  private val BaseQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | select id,
     |        key,
     |        organization_key,
     |        application_key,
     |        created_at,
     |        updated_at,
     |        updated_by_guid,
     |        hash_code
     |   from public.generator_invocations
     |""".stripMargin.stripTrailing
    )
  }

  def findAll(
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[GeneratorInvocation] = {
    db.withConnection { c =>
      findAllWithConnection(c, id, ids, limit, offset, orderBy)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[GeneratorInvocation] = {
    customQueryModifier(BaseQuery)
      .equals("generator_invocations.id", id)
      .optionalIn("generator_invocations.id", ids)
      .optionalLimit(limit)
      .offset(offset)
      .orderBy(orderBy.flatMap(_.sql))
      .as(parser.*)(c)
  }

  def iterateAll(
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    pageSize: Long = 1000
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Iterator[GeneratorInvocation] = {
    assert(pageSize > 0, "pageSize must be > 0")

    def iterate(lastValue: Option[GeneratorInvocation]): Iterator[GeneratorInvocation] = {
      val page: Seq[GeneratorInvocation] = db.withConnection { c =>
        customQueryModifier(BaseQuery)
          .equals("generator_invocations.id", id)
          .optionalIn("generator_invocations.id", ids)
          .greaterThan("generator_invocations.id", lastValue.map(_.id))
          .orderBy("generator_invocations.id")
          .limit(pageSize)
          .as(parser.*)(c)
      }
      if (page.length >= pageSize) {
        page.iterator ++ iterate(page.lastOption)
      } else {
        page.iterator
      }
    }

    iterate(None)
  }

  def findById(id: String): Option[GeneratorInvocation] = {
    db.withConnection { c =>
      findByIdWithConnection(c, id)
    }
  }

  def findByIdWithConnection(
    c: java.sql.Connection,
    id: String
  ): Option[GeneratorInvocation] = {
    findAllWithConnection(
      c = c,
      id = Some(id),
      limit = Some(1)
    ).headOption
  }

  private val parser: anorm.RowParser[GeneratorInvocation] = {
    anorm.SqlParser.str("id") ~
      anorm.SqlParser.str("key") ~
      anorm.SqlParser.str("organization_key").? ~
      anorm.SqlParser.str("application_key").? ~
      anorm.SqlParser.get[org.joda.time.DateTime]("created_at") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("updated_at") ~
      anorm.SqlParser.str("updated_by_guid") ~
      anorm.SqlParser.long("hash_code") map { case id ~ key ~ organizationKey ~ applicationKey ~ createdAt ~ updatedAt ~ updatedByGuid ~ hashCode =>
      GeneratorInvocation(
        id = id,
        key = key,
        organizationKey = organizationKey,
        applicationKey = applicationKey,
        createdAt = createdAt,
        updatedAt = updatedAt,
        updatedByGuid = updatedByGuid
      )
    }
  }
}

class GeneratorInvocationsDao @javax.inject.Inject() (override val db: play.api.db.Database) extends BaseGeneratorInvocationsDao {
  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  private val pkeyGenerator: com.mbryzek.util.IdGenerator = {
    com.mbryzek.util.IdGenerator("gni")
  }

  def randomId: String = {
    pkeyGenerator.randomId()
  }

  private val InsertQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | insert into public.generator_invocations
     | (id, key, organization_key, application_key, created_at, updated_at, updated_by_guid, hash_code)
     | values
     | ({id}, {key}, {organization_key}, {application_key}, {created_at}::timestamptz, {updated_at}::timestamptz, {updated_by_guid}, {hash_code}::bigint)
    """.stripMargin)
  }

  private val UpdateQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | update public.generator_invocations
     | set key = {key},
     |     organization_key = {organization_key},
     |     application_key = {application_key},
     |     updated_at = {updated_at}::timestamptz,
     |     updated_by_guid = {updated_by_guid},
     |     hash_code = {hash_code}::bigint
     | where id = {id} and generator_invocations.hash_code != {hash_code}::bigint
    """.stripMargin)
  }

  private val DeleteQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("delete from public.generator_invocations")
  }

  def insert(
    user: java.util.UUID,
    form: GeneratorInvocationForm
  ): String = {
    db.withConnection { c =>
      insert(c, user, form)
    }
  }

  def insert(
    c: java.sql.Connection,
    user: java.util.UUID,
    form: GeneratorInvocationForm
  ): String = {
    val id = randomId
    bindQuery(InsertQuery, user, form)
      .bind("created_at", org.joda.time.DateTime.now)
      .bind("id", id)
      .execute(c)
    id
  }

  def insertBatch(
    user: java.util.UUID,
    forms: Seq[GeneratorInvocationForm]
  ): Seq[String] = {
    db.withConnection { c =>
      insertBatch(c, user, forms)
    }
  }

  def insertBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[GeneratorInvocationForm]
  ): Seq[String] = {
    forms.map { f =>
      val id = randomId
      (id, Seq(anorm.NamedParameter("created_at", org.joda.time.DateTime.now)) ++ toNamedParameter(user, id, f))
    }.toList match {
      case Nil => Nil
      case one :: rest => {
        anorm.BatchSql(InsertQuery.sql(), one._2, rest.map(_._2)*).execute()(c)
        Seq(one._1) ++ rest.map(_._1)
      }
    }
  }

  def update(
    user: java.util.UUID,
    generatorInvocation: GeneratorInvocation,
    form: GeneratorInvocationForm
  ): Unit = {
    db.withConnection { c =>
      update(c, user, generatorInvocation, form)
    }
  }

  def update(
    c: java.sql.Connection,
    user: java.util.UUID,
    generatorInvocation: GeneratorInvocation,
    form: GeneratorInvocationForm
  ): Unit = {
    updateById(
      c = c,
      user = user,
      id = generatorInvocation.id,
      form = form
    )
  }

  def updateById(
    user: java.util.UUID,
    id: String,
    form: GeneratorInvocationForm
  ): Unit = {
    db.withConnection { c =>
      updateById(c, user, id, form)
    }
  }

  def updateById(
    c: java.sql.Connection,
    user: java.util.UUID,
    id: String,
    form: GeneratorInvocationForm
  ): Unit = {
    bindQuery(UpdateQuery, user, form)
      .bind("id", id)
      .bind("updated_by_guid", user)
      .execute(c)
    ()
  }

  def updateBatch(
    user: java.util.UUID,
    forms: Seq[(String, GeneratorInvocationForm)]
  ): Unit = {
    db.withConnection { c =>
      updateBatch(c, user, forms)
    }
  }

  def updateBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[(String, GeneratorInvocationForm)]
  ): Unit = {
    forms.map { case (id, f) => toNamedParameter(user, id, f) }.toList match {
      case Nil => // no-op
      case first :: rest => anorm.BatchSql(UpdateQuery.sql(), first, rest*).execute()(c)
    }
  }

  def delete(
    user: java.util.UUID,
    generatorInvocation: GeneratorInvocation
  ): Unit = {
    db.withConnection { c =>
      delete(c, user, generatorInvocation)
    }
  }

  def delete(
    c: java.sql.Connection,
    user: java.util.UUID,
    generatorInvocation: GeneratorInvocation
  ): Unit = {
    deleteById(
      c = c,
      user = user,
      id = generatorInvocation.id
    )
  }

  def deleteById(
    user: java.util.UUID,
    id: String
  ): Unit = {
    db.withConnection { c =>
      deleteById(c, user, id)
    }
  }

  def deleteById(
    c: java.sql.Connection,
    user: java.util.UUID,
    id: String
  ): Unit = {
    DeleteQuery.equals("id", id).execute(c)
  }

  def deleteAllByIds(
    user: java.util.UUID,
    ids: Seq[String]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByIds(c, user, ids)
    }
  }

  def deleteAllByIds(
    c: java.sql.Connection,
    user: java.util.UUID,
    ids: Seq[String]
  ): Unit = {
    DeleteQuery.in("id", ids).execute(c)
  }

  private def bindQuery(
    query: io.flow.postgresql.Query,
    user: java.util.UUID,
    form: GeneratorInvocationForm
  ): io.flow.postgresql.Query = {
    query
      .bind("key", form.key)
      .bind("organization_key", form.organizationKey)
      .bind("application_key", form.applicationKey)
      .bind("updated_at", org.joda.time.DateTime.now)
      .bind("updated_by_guid", user)
      .bind("hash_code", form.hashCode())
  }

  private def toNamedParameter(
    user: java.util.UUID,
    id: String,
    form: GeneratorInvocationForm
  ): Seq[anorm.NamedParameter] = {
    Seq(
      anorm.NamedParameter("id", id),
      anorm.NamedParameter("key", form.key),
      anorm.NamedParameter("organization_key", form.organizationKey),
      anorm.NamedParameter("application_key", form.applicationKey),
      anorm.NamedParameter("updated_at", org.joda.time.DateTime.now),
      anorm.NamedParameter("updated_by_guid", user),
      anorm.NamedParameter("hash_code", form.hashCode())
    )
  }
}