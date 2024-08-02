package db.generated.generators

case class Generator(
  guid: java.util.UUID,
  serviceGuid: java.util.UUID,
  key: String,
  name: String,
  description: Option[String],
  language: Option[String],
  attributes: play.api.libs.json.JsValue,
  createdAt: org.joda.time.DateTime,
  createdByGuid: java.util.UUID,
  updatedAt: org.joda.time.DateTime,
  deletedAt: Option[org.joda.time.DateTime],
  deletedByGuid: Option[java.util.UUID]
) {
  def form: GeneratorForm = {
    GeneratorForm(
      serviceGuid = serviceGuid,
      key = key,
      name = name,
      description = description,
      language = language,
      attributes = attributes,
    )
  }
}

case class GeneratorForm(
  serviceGuid: java.util.UUID,
  key: String,
  name: String,
  description: Option[String],
  language: Option[String],
  attributes: play.api.libs.json.JsValue
)

case object GeneratorsTable {
  val SchemaName: String = "generators"

  val TableName: String = "generators"

  val QualifiedName: String = "generators.generators"

  sealed trait Column {
    def name: String
  }

  object Columns {
    case object Guid extends Column {
      override val name: String = "guid"
    }

    case object ServiceGuid extends Column {
      override val name: String = "service_guid"
    }

    case object Key extends Column {
      override val name: String = "key"
    }

    case object Name extends Column {
      override val name: String = "name"
    }

    case object Description extends Column {
      override val name: String = "description"
    }

    case object Language extends Column {
      override val name: String = "language"
    }

    case object Attributes extends Column {
      override val name: String = "attributes"
    }

    case object CreatedAt extends Column {
      override val name: String = "created_at"
    }

    case object CreatedByGuid extends Column {
      override val name: String = "created_by_guid"
    }

    case object UpdatedAt extends Column {
      override val name: String = "updated_at"
    }

    case object DeletedAt extends Column {
      override val name: String = "deleted_at"
    }

    case object DeletedByGuid extends Column {
      override val name: String = "deleted_by_guid"
    }

    val all: List[Column] = List(Guid, ServiceGuid, Key, Name, Description, Language, Attributes, CreatedAt, CreatedByGuid, UpdatedAt, DeletedAt, DeletedByGuid)
  }
}

trait BaseGeneratorsDao {
  import anorm.*

  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def db: play.api.db.Database

  private val BaseQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | select guid::text,
     |        service_guid::text,
     |        key,
     |        name,
     |        description,
     |        language,
     |        attributes::text,
     |        created_at,
     |        created_by_guid::text,
     |        updated_at,
     |        deleted_at,
     |        deleted_by_guid::text
     |   from generators.generators
     |""".stripMargin.stripTrailing
    )
  }

  def findAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[Generator] = {
    db.withConnection { c =>
      findAllWithConnection(c, guid, guids, limit, offset, orderBy)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[Generator] = {
    customQueryModifier(BaseQuery)
      .equals("generators.guid", guid)
      .optionalIn("generators.guid", guids)
      .optionalLimit(limit)
      .offset(offset)
      .orderBy(orderBy.flatMap(_.sql))
      .as(parser.*)(c)
  }

  def iterateAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    pageSize: Long = 1000
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Iterator[Generator] = {
    assert(pageSize > 0, "pageSize must be > 0")

    def iterate(lastValue: Option[Generator]): Iterator[Generator] = {
      val page: Seq[Generator] = db.withConnection { c =>
        customQueryModifier(BaseQuery)
          .equals("generators.guid", guid)
          .optionalIn("generators.guid", guids)
          .greaterThan("generators.guid", lastValue.map(_.guid))
          .orderBy("generators.guid")
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

  def findByGuid(guid: java.util.UUID): Option[Generator] = {
    db.withConnection { c =>
      findByGuidWithConnection(c, guid)
    }
  }

  def findByGuidWithConnection(
    c: java.sql.Connection,
    guid: java.util.UUID
  ): Option[Generator] = {
    findAllWithConnection(
      c = c,
      guid = Some(guid),
      limit = Some(1)
    ).headOption
  }

  private val parser: anorm.RowParser[Generator] = {
    anorm.SqlParser.str("guid") ~
      anorm.SqlParser.str("service_guid") ~
      anorm.SqlParser.str("key") ~
      anorm.SqlParser.str("name") ~
      anorm.SqlParser.str("description").? ~
      anorm.SqlParser.str("language").? ~
      anorm.SqlParser.str("attributes") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("created_at") ~
      anorm.SqlParser.str("created_by_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("updated_at") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("deleted_at").? ~
      anorm.SqlParser.str("deleted_by_guid").? map { case guid ~ serviceGuid ~ key ~ name ~ description ~ language ~ attributes ~ createdAt ~ createdByGuid ~ updatedAt ~ deletedAt ~ deletedByGuid =>
      Generator(
        guid = java.util.UUID.fromString(guid),
        serviceGuid = java.util.UUID.fromString(serviceGuid),
        key = key,
        name = name,
        description = description,
        language = language,
        attributes = play.api.libs.json.Json.parse(attributes),
        createdAt = createdAt,
        createdByGuid = java.util.UUID.fromString(createdByGuid),
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        deletedByGuid = deletedByGuid.map { v => java.util.UUID.fromString(v) }
      )
    }
  }
}

class GeneratorsDao @javax.inject.Inject() (override val db: play.api.db.Database) extends BaseGeneratorsDao {
  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def randomPkey: java.util.UUID = {
    java.util.UUID.randomUUID
  }

  private val InsertQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | insert into generators.generators
     | (guid, service_guid, key, name, description, language, attributes, created_at, created_by_guid, updated_at)
     | values
     | ({guid}::uuid, {service_guid}::uuid, {key}, {name}, {description}, {language}, {attributes}::json, {created_at}::timestamptz, {created_by_guid}::uuid, {updated_at}::timestamptz)
    """.stripMargin)
  }

  private val UpdateQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | update generators.generators
     | set service_guid = {service_guid}::uuid,
     |     key = {key},
     |     name = {name},
     |     description = {description},
     |     language = {language},
     |     attributes = {attributes}::json,
     |     updated_at = {updated_at}::timestamptz
     | where guid = {guid}::uuid
    """.stripMargin)
  }

  private val DeleteQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("update generators.generators set deleted_at = {deleted_at}::timestamptz, deleted_by_guid = {deleted_by_guid}::uuid").isNull("deleted_at")
  }

  def insert(
    user: java.util.UUID,
    form: GeneratorForm
  ): java.util.UUID = {
    db.withConnection { c =>
      insert(c, user, form)
    }
  }

  def insert(
    c: java.sql.Connection,
    user: java.util.UUID,
    form: GeneratorForm
  ): java.util.UUID = {
    val id = randomPkey
    bindQuery(InsertQuery, user, form)
      .bind("created_at", org.joda.time.DateTime.now)
      .bind("created_by_guid", user)
      .bind("guid", id)
      .execute(c)
    id
  }

  def insertBatch(
    user: java.util.UUID,
    forms: Seq[GeneratorForm]
  ): Seq[java.util.UUID] = {
    db.withConnection { c =>
      insertBatch(c, user, forms)
    }
  }

  def insertBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[GeneratorForm]
  ): Seq[java.util.UUID] = {
    forms.map { f =>
      val guid = randomPkey
      (guid, Seq(anorm.NamedParameter("created_at", org.joda.time.DateTime.now)) ++ toNamedParameter(user, guid, f))
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
    generator: Generator,
    form: GeneratorForm
  ): Unit = {
    db.withConnection { c =>
      update(c, user, generator, form)
    }
  }

  def update(
    c: java.sql.Connection,
    user: java.util.UUID,
    generator: Generator,
    form: GeneratorForm
  ): Unit = {
    updateByGuid(
      c = c,
      user = user,
      guid = generator.guid,
      form = form
    )
  }

  def updateByGuid(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: GeneratorForm
  ): Unit = {
    db.withConnection { c =>
      updateByGuid(c, user, guid, form)
    }
  }

  def updateByGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    guid: java.util.UUID,
    form: GeneratorForm
  ): Unit = {
    bindQuery(UpdateQuery, user, form)
      .bind("guid", guid)
      .execute(c)
    ()
  }

  def updateBatch(
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, GeneratorForm)]
  ): Unit = {
    db.withConnection { c =>
      updateBatch(c, user, forms)
    }
  }

  def updateBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, GeneratorForm)]
  ): Unit = {
    forms.map { case (guid, f) => toNamedParameter(user, guid, f) }.toList match {
      case Nil => // no-op
      case first :: rest => anorm.BatchSql(UpdateQuery.sql(), first, rest*).execute()(c)
    }
  }

  def delete(
    user: java.util.UUID,
    generator: Generator
  ): Unit = {
    db.withConnection { c =>
      delete(c, user, generator)
    }
  }

  def delete(
    c: java.sql.Connection,
    user: java.util.UUID,
    generator: Generator
  ): Unit = {
    deleteByGuid(
      c = c,
      user = user,
      guid = generator.guid
    )
  }

  def deleteByGuid(
    user: java.util.UUID,
    guid: java.util.UUID
  ): Unit = {
    db.withConnection { c =>
      deleteByGuid(c, user, guid)
    }
  }

  def deleteByGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    guid: java.util.UUID
  ): Unit = {
    DeleteQuery.equals("guid", guid)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByGuids(
    user: java.util.UUID,
    guids: Seq[java.util.UUID]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByGuids(c, user, guids)
    }
  }

  def deleteAllByGuids(
    c: java.sql.Connection,
    user: java.util.UUID,
    guids: Seq[java.util.UUID]
  ): Unit = {
    DeleteQuery.in("guid", guids)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  private def bindQuery(
    query: io.flow.postgresql.Query,
    user: java.util.UUID,
    form: GeneratorForm
  ): io.flow.postgresql.Query = {
    query
      .bind("service_guid", form.serviceGuid.toString)
      .bind("key", form.key)
      .bind("name", form.name)
      .bind("description", form.description)
      .bind("language", form.language)
      .bind("attributes", play.api.libs.json.Json.toJson(form.attributes).toString)
      .bind("updated_at", org.joda.time.DateTime.now)
  }

  private def toNamedParameter(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: GeneratorForm
  ): Seq[anorm.NamedParameter] = {
    Seq(
      anorm.NamedParameter("guid", guid.toString),
      anorm.NamedParameter("service_guid", form.serviceGuid.toString),
      anorm.NamedParameter("key", form.key),
      anorm.NamedParameter("name", form.name),
      anorm.NamedParameter("description", form.description),
      anorm.NamedParameter("language", form.language),
      anorm.NamedParameter("attributes", play.api.libs.json.Json.toJson(form.attributes).toString),
      anorm.NamedParameter("updated_at", org.joda.time.DateTime.now)
    )
  }
}