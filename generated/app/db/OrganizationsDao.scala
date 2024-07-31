package db.generated

case class Organization(
  guid: java.util.UUID,
  name: String,
  key: String,
  namespace: String,
  visibility: String,
  createdAt: org.joda.time.DateTime,
  createdByGuid: java.util.UUID,
  updatedAt: org.joda.time.DateTime,
  updatedByGuid: java.util.UUID,
  deletedAt: Option[org.joda.time.DateTime],
  deletedByGuid: Option[java.util.UUID]
) {
  def form: OrganizationForm = {
    OrganizationForm(
      name = name,
      key = key,
      namespace = namespace,
      visibility = visibility,
    )
  }
}

case class OrganizationForm(
  name: String,
  key: String,
  namespace: String,
  visibility: String
)

case object OrganizationsTable {
  val SchemaName: String = "public"

  val TableName: String = "organizations"

  val QualifiedName: String = "public.organizations"

  sealed trait Column {
    def name: String
  }

  object Columns {
    case object Guid extends Column {
      override val name: String = "guid"
    }

    case object Name extends Column {
      override val name: String = "name"
    }

    case object Key extends Column {
      override val name: String = "key"
    }

    case object Namespace extends Column {
      override val name: String = "namespace"
    }

    case object Visibility extends Column {
      override val name: String = "visibility"
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

    case object UpdatedByGuid extends Column {
      override val name: String = "updated_by_guid"
    }

    case object DeletedAt extends Column {
      override val name: String = "deleted_at"
    }

    case object DeletedByGuid extends Column {
      override val name: String = "deleted_by_guid"
    }

    case object HashCode extends Column {
      override val name: String = "hash_code"
    }

    val all: List[Column] = List(Guid, Name, Key, Namespace, Visibility, CreatedAt, CreatedByGuid, UpdatedAt, UpdatedByGuid, DeletedAt, DeletedByGuid, HashCode)
  }
}

trait BaseOrganizationsDao {
  import anorm.*

  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def db: play.api.db.Database

  private val BaseQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | select guid::text,
     |        name,
     |        key,
     |        namespace,
     |        visibility,
     |        created_at,
     |        created_by_guid::text,
     |        updated_at,
     |        updated_by_guid::text,
     |        deleted_at,
     |        deleted_by_guid::text,
     |        hash_code
     |   from public.organizations
     |""".stripMargin.stripTrailing
    )
  }

  def findAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[Organization] = {
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
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[Organization] = {
    customQueryModifier(BaseQuery)
      .equals("organizations.guid", guid)
      .optionalIn("organizations.guid", guids)
      .optionalLimit(limit)
      .offset(offset)
      .orderBy(orderBy.flatMap(_.sql))
      .as(parser.*)(c)
  }

  def iterateAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    pageSize: Long = 1000
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Iterator[Organization] = {
    assert(pageSize > 0, "pageSize must be > 0")

    def iterate(lastValue: Option[Organization]): Iterator[Organization] = {
      val page: Seq[Organization] = db.withConnection { c =>
        customQueryModifier(BaseQuery)
          .equals("organizations.guid", guid)
          .optionalIn("organizations.guid", guids)
          .greaterThan("organizations.guid", lastValue.map(_.guid))
          .orderBy("organizations.guid")
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

  def findByGuid(guid: java.util.UUID): Option[Organization] = {
    db.withConnection { c =>
      findByGuidWithConnection(c, guid)
    }
  }

  def findByGuidWithConnection(
    c: java.sql.Connection,
    guid: java.util.UUID
  ): Option[Organization] = {
    findAllWithConnection(
      c = c,
      guid = Some(guid),
      limit = Some(1)
    ).headOption
  }

  private val parser: anorm.RowParser[Organization] = {
    anorm.SqlParser.str("guid") ~
      anorm.SqlParser.str("name") ~
      anorm.SqlParser.str("key") ~
      anorm.SqlParser.str("namespace") ~
      anorm.SqlParser.str("visibility") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("created_at") ~
      anorm.SqlParser.str("created_by_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("updated_at") ~
      anorm.SqlParser.str("updated_by_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("deleted_at").? ~
      anorm.SqlParser.str("deleted_by_guid").? ~
      anorm.SqlParser.long("hash_code") map { case guid ~ name ~ key ~ namespace ~ visibility ~ createdAt ~ createdByGuid ~ updatedAt ~ updatedByGuid ~ deletedAt ~ deletedByGuid ~ hashCode =>
      Organization(
        guid = java.util.UUID.fromString(guid),
        name = name,
        key = key,
        namespace = namespace,
        visibility = visibility,
        createdAt = createdAt,
        createdByGuid = java.util.UUID.fromString(createdByGuid),
        updatedAt = updatedAt,
        updatedByGuid = java.util.UUID.fromString(updatedByGuid),
        deletedAt = deletedAt,
        deletedByGuid = deletedByGuid.map { v => java.util.UUID.fromString(v) }
      )
    }
  }
}

class OrganizationsDao @javax.inject.Inject() (override val db: play.api.db.Database) extends BaseOrganizationsDao {
  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def randomId: java.util.UUID = {
    java.util.UUID.randomUUID
  }

  private val InsertQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | insert into public.organizations
     | (guid, name, key, namespace, visibility, created_at, created_by_guid, updated_at, updated_by_guid, hash_code)
     | values
     | ({guid}::uuid, {name}, {key}, {namespace}, {visibility}, {created_at}::timestamptz, {created_by_guid}::uuid, {updated_at}::timestamptz, {updated_by_guid}::uuid, {hash_code}::bigint)
    """.stripMargin)
  }

  private val UpdateQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | update public.organizations
     | set name = {name},
     |     key = {key},
     |     namespace = {namespace},
     |     visibility = {visibility},
     |     updated_at = {updated_at}::timestamptz,
     |     updated_by_guid = {updated_by_guid}::uuid,
     |     hash_code = {hash_code}::bigint
     | where guid = {guid}::uuid and organizations.hash_code != {hash_code}::bigint
    """.stripMargin)
  }

  private val DeleteQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("update public.organizations set deleted_at = {deleted_at}::timestamptz, deleted_by_guid = {deleted_by_guid}::uuid")
  }

  def insert(
    user: java.util.UUID,
    form: OrganizationForm
  ): java.util.UUID = {
    db.withConnection { c =>
      insert(c, user, form)
    }
  }

  def insert(
    c: java.sql.Connection,
    user: java.util.UUID,
    form: OrganizationForm
  ): java.util.UUID = {
    val id = randomId
    bindQuery(InsertQuery, user, form)
      .bind("created_at", org.joda.time.DateTime.now)
      .bind("created_by_guid", user)
      .bind("guid", id)
      .execute(c)
    id
  }

  def insertBatch(
    user: java.util.UUID,
    forms: Seq[OrganizationForm]
  ): Seq[java.util.UUID] = {
    db.withConnection { c =>
      insertBatch(c, user, forms)
    }
  }

  def insertBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[OrganizationForm]
  ): Seq[java.util.UUID] = {
    forms.map { f =>
      val guid = randomId
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
    organization: Organization,
    form: OrganizationForm
  ): Unit = {
    db.withConnection { c =>
      update(c, user, organization, form)
    }
  }

  def update(
    c: java.sql.Connection,
    user: java.util.UUID,
    organization: Organization,
    form: OrganizationForm
  ): Unit = {
    updateByGuid(
      c = c,
      user = user,
      guid = organization.guid,
      form = form
    )
  }

  def updateByGuid(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: OrganizationForm
  ): Unit = {
    db.withConnection { c =>
      updateByGuid(c, user, guid, form)
    }
  }

  def updateByGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    guid: java.util.UUID,
    form: OrganizationForm
  ): Unit = {
    bindQuery(UpdateQuery, user, form)
      .bind("guid", guid)
      .bind("updated_by_guid", user)
      .execute(c)
    ()
  }

  def updateBatch(
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, OrganizationForm)]
  ): Unit = {
    db.withConnection { c =>
      updateBatch(c, user, forms)
    }
  }

  def updateBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, OrganizationForm)]
  ): Unit = {
    forms.map { case (guid, f) => toNamedParameter(user, guid, f) }.toList match {
      case Nil => // no-op
      case first :: rest => anorm.BatchSql(UpdateQuery.sql(), first, rest*).execute()(c)
    }
  }

  def delete(
    user: java.util.UUID,
    organization: Organization
  ): Unit = {
    db.withConnection { c =>
      delete(c, user, organization)
    }
  }

  def delete(
    c: java.sql.Connection,
    user: java.util.UUID,
    organization: Organization
  ): Unit = {
    deleteByGuid(
      c = c,
      user = user,
      guid = organization.guid
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
    form: OrganizationForm
  ): io.flow.postgresql.Query = {
    query
      .bind("name", form.name)
      .bind("key", form.key)
      .bind("namespace", form.namespace)
      .bind("visibility", form.visibility)
      .bind("updated_at", org.joda.time.DateTime.now)
      .bind("updated_by_guid", user)
      .bind("hash_code", form.hashCode())
  }

  private def toNamedParameter(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: OrganizationForm
  ): Seq[anorm.NamedParameter] = {
    Seq(
      anorm.NamedParameter("guid", guid.toString),
      anorm.NamedParameter("name", form.name),
      anorm.NamedParameter("key", form.key),
      anorm.NamedParameter("namespace", form.namespace),
      anorm.NamedParameter("visibility", form.visibility),
      anorm.NamedParameter("updated_at", org.joda.time.DateTime.now),
      anorm.NamedParameter("updated_by_guid", user.toString),
      anorm.NamedParameter("hash_code", form.hashCode())
    )
  }
}