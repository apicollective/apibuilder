package db.generated

case class Version(
  guid: java.util.UUID,
  applicationGuid: java.util.UUID,
  version: String,
  versionSortKey: String,
  original: Option[String],
  oldJson: Option[play.api.libs.json.JsValue],
  createdAt: org.joda.time.DateTime,
  createdByGuid: java.util.UUID,
  deletedAt: Option[org.joda.time.DateTime],
  deletedByGuid: Option[java.util.UUID]
) {
  def form: VersionForm = {
    VersionForm(
      applicationGuid = applicationGuid,
      version = version,
      versionSortKey = versionSortKey,
      original = original,
      oldJson = oldJson,
    )
  }
}

case class VersionForm(
  applicationGuid: java.util.UUID,
  version: String,
  versionSortKey: String,
  original: Option[String],
  oldJson: Option[play.api.libs.json.JsValue]
)

case object VersionsTable {
  val SchemaName: String = "public"

  val TableName: String = "versions"

  val QualifiedName: String = "public.versions"

  sealed trait Column {
    def name: String
  }

  object Columns {
    case object Guid extends Column {
      override val name: String = "guid"
    }

    case object ApplicationGuid extends Column {
      override val name: String = "application_guid"
    }

    case object Version extends Column {
      override val name: String = "version"
    }

    case object VersionSortKey extends Column {
      override val name: String = "version_sort_key"
    }

    case object Original extends Column {
      override val name: String = "original"
    }

    case object OldJson extends Column {
      override val name: String = "old_json"
    }

    case object CreatedAt extends Column {
      override val name: String = "created_at"
    }

    case object CreatedByGuid extends Column {
      override val name: String = "created_by_guid"
    }

    case object DeletedAt extends Column {
      override val name: String = "deleted_at"
    }

    case object DeletedByGuid extends Column {
      override val name: String = "deleted_by_guid"
    }

    val all: List[Column] = List(Guid, ApplicationGuid, Version, VersionSortKey, Original, OldJson, CreatedAt, CreatedByGuid, DeletedAt, DeletedByGuid)
  }
}

trait BaseVersionsDao {
  import anorm.*

  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def db: play.api.db.Database

  private val BaseQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | select guid::text,
     |        application_guid::text,
     |        version,
     |        version_sort_key,
     |        original,
     |        old_json::text,
     |        created_at,
     |        created_by_guid::text,
     |        deleted_at,
     |        deleted_by_guid::text
     |   from public.versions
     |""".stripMargin.stripTrailing
    )
  }

  def findAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    applicationGuid: Option[java.util.UUID] = None,
    applicationGuids: Option[Seq[java.util.UUID]] = None,
    guidAndApplicationGuid: Option[(java.util.UUID, java.util.UUID)] = None,
    guidsAndApplicationGuids: Option[Seq[(java.util.UUID, java.util.UUID)]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[Version] = {
    db.withConnection { c =>
      findAllWithConnection(c, guid, guids, applicationGuid, applicationGuids, guidAndApplicationGuid, guidsAndApplicationGuids, limit, offset, orderBy)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    applicationGuid: Option[java.util.UUID] = None,
    applicationGuids: Option[Seq[java.util.UUID]] = None,
    guidAndApplicationGuid: Option[(java.util.UUID, java.util.UUID)] = None,
    guidsAndApplicationGuids: Option[Seq[(java.util.UUID, java.util.UUID)]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[Version] = {
    customQueryModifier(BaseQuery)
      .equals("versions.guid", guid)
      .optionalIn("versions.guid", guids)
      .equals("versions.application_guid", applicationGuid)
      .optionalIn("versions.application_guid", applicationGuids)
      .optionalIn2(("versions.guid", "versions.application_guid"), guidAndApplicationGuid.map(Seq(_)))
      .optionalIn2(("versions.guid", "versions.application_guid"), guidsAndApplicationGuids)
      .optionalLimit(limit)
      .offset(offset)
      .orderBy(orderBy.flatMap(_.sql))
      .as(parser.*)(c)
  }

  def iterateAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    applicationGuid: Option[java.util.UUID] = None,
    applicationGuids: Option[Seq[java.util.UUID]] = None,
    guidAndApplicationGuid: Option[(java.util.UUID, java.util.UUID)] = None,
    guidsAndApplicationGuids: Option[Seq[(java.util.UUID, java.util.UUID)]] = None,
    pageSize: Long = 1000
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Iterator[Version] = {
    assert(pageSize > 0, "pageSize must be > 0")

    def iterate(lastValue: Option[Version]): Iterator[Version] = {
      val page: Seq[Version] = db.withConnection { c =>
        customQueryModifier(BaseQuery)
          .equals("versions.guid", guid)
          .optionalIn("versions.guid", guids)
          .equals("versions.application_guid", applicationGuid)
          .optionalIn("versions.application_guid", applicationGuids)
          .optionalIn2(("versions.guid", "versions.application_guid"), guidAndApplicationGuid.map(Seq(_)))
          .optionalIn2(("versions.guid", "versions.application_guid"), guidsAndApplicationGuids)
          .greaterThan("versions.guid", lastValue.map(_.guid))
          .orderBy("versions.guid")
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

  def findByGuid(guid: java.util.UUID): Option[Version] = {
    db.withConnection { c =>
      findByGuidWithConnection(c, guid)
    }
  }

  def findByGuidWithConnection(
    c: java.sql.Connection,
    guid: java.util.UUID
  ): Option[Version] = {
    findAllWithConnection(
      c = c,
      guid = Some(guid),
      limit = Some(1)
    ).headOption
  }

  def findAllByApplicationGuid(applicationGuid: java.util.UUID): Seq[Version] = {
    db.withConnection { c =>
      findAllByApplicationGuidWithConnection(c, applicationGuid)
    }
  }

  def findAllByApplicationGuidWithConnection(
    c: java.sql.Connection,
    applicationGuid: java.util.UUID
  ): Seq[Version] = {
    findAllWithConnection(
      c = c,
      applicationGuid = Some(applicationGuid),
      limit = None
    )
  }

  def findByGuidAndApplicationGuid(guidAndApplicationGuid: (java.util.UUID, java.util.UUID)): Option[Version] = {
    db.withConnection { c =>
      findByGuidAndApplicationGuidWithConnection(c, guidAndApplicationGuid)
    }
  }

  def findByGuidAndApplicationGuidWithConnection(
    c: java.sql.Connection,
    guidAndApplicationGuid: (java.util.UUID, java.util.UUID)
  ): Option[Version] = {
    findAllWithConnection(
      c = c,
      guidAndApplicationGuid = Some(guidAndApplicationGuid),
      limit = Some(1)
    ).headOption
  }

  private val parser: anorm.RowParser[Version] = {
    anorm.SqlParser.str("guid") ~
      anorm.SqlParser.str("application_guid") ~
      anorm.SqlParser.str("version") ~
      anorm.SqlParser.str("version_sort_key") ~
      anorm.SqlParser.str("original").? ~
      anorm.SqlParser.str("old_json").? ~
      anorm.SqlParser.get[org.joda.time.DateTime]("created_at") ~
      anorm.SqlParser.str("created_by_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("deleted_at").? ~
      anorm.SqlParser.str("deleted_by_guid").? map { case guid ~ applicationGuid ~ version ~ versionSortKey ~ original ~ oldJson ~ createdAt ~ createdByGuid ~ deletedAt ~ deletedByGuid =>
      Version(
        guid = java.util.UUID.fromString(guid),
        applicationGuid = java.util.UUID.fromString(applicationGuid),
        version = version,
        versionSortKey = versionSortKey,
        original = original,
        oldJson = oldJson.map { v => play.api.libs.json.Json.parse(v) },
        createdAt = createdAt,
        createdByGuid = java.util.UUID.fromString(createdByGuid),
        deletedAt = deletedAt,
        deletedByGuid = deletedByGuid.map { v => java.util.UUID.fromString(v) }
      )
    }
  }
}

class VersionsDao @javax.inject.Inject() (override val db: play.api.db.Database) extends BaseVersionsDao {
  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def randomPkey: java.util.UUID = {
    java.util.UUID.randomUUID
  }

  private val InsertQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | insert into public.versions
     | (guid, application_guid, version, version_sort_key, original, old_json, created_at, created_by_guid)
     | values
     | ({guid}::uuid, {application_guid}::uuid, {version}, {version_sort_key}, {original}, {old_json}::json, {created_at}::timestamptz, {created_by_guid}::uuid)
    """.stripMargin)
  }

  private val UpdateQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | update public.versions
     | set application_guid = {application_guid}::uuid,
     |     version = {version},
     |     version_sort_key = {version_sort_key},
     |     original = {original},
     |     old_json = {old_json}::json
     | where guid = {guid}::uuid
    """.stripMargin)
  }

  private val DeleteQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("update public.versions set deleted_at = {deleted_at}::timestamptz, deleted_by_guid = {deleted_by_guid}::uuid").isNull("deleted_at")
  }

  def insert(
    user: java.util.UUID,
    form: VersionForm
  ): java.util.UUID = {
    db.withConnection { c =>
      insert(c, user, form)
    }
  }

  def insert(
    c: java.sql.Connection,
    user: java.util.UUID,
    form: VersionForm
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
    forms: Seq[VersionForm]
  ): Seq[java.util.UUID] = {
    db.withConnection { c =>
      insertBatch(c, user, forms)
    }
  }

  def insertBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[VersionForm]
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
    version: Version,
    form: VersionForm
  ): Unit = {
    db.withConnection { c =>
      update(c, user, version, form)
    }
  }

  def update(
    c: java.sql.Connection,
    user: java.util.UUID,
    version: Version,
    form: VersionForm
  ): Unit = {
    updateByGuid(
      c = c,
      user = user,
      guid = version.guid,
      form = form
    )
  }

  def updateByGuid(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: VersionForm
  ): Unit = {
    db.withConnection { c =>
      updateByGuid(c, user, guid, form)
    }
  }

  def updateByGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    guid: java.util.UUID,
    form: VersionForm
  ): Unit = {
    bindQuery(UpdateQuery, user, form)
      .bind("guid", guid)
      .execute(c)
    ()
  }

  def updateBatch(
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, VersionForm)]
  ): Unit = {
    db.withConnection { c =>
      updateBatch(c, user, forms)
    }
  }

  def updateBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, VersionForm)]
  ): Unit = {
    forms.map { case (guid, f) => toNamedParameter(user, guid, f) }.toList match {
      case Nil => // no-op
      case first :: rest => anorm.BatchSql(UpdateQuery.sql(), first, rest*).execute()(c)
    }
  }

  def delete(
    user: java.util.UUID,
    version: Version
  ): Unit = {
    db.withConnection { c =>
      delete(c, user, version)
    }
  }

  def delete(
    c: java.sql.Connection,
    user: java.util.UUID,
    version: Version
  ): Unit = {
    deleteByGuid(
      c = c,
      user = user,
      guid = version.guid
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

  def deleteAllByApplicationGuid(
    user: java.util.UUID,
    applicationGuid: java.util.UUID
  ): Unit = {
    db.withConnection { c =>
      deleteAllByApplicationGuid(c, user, applicationGuid)
    }
  }

  def deleteAllByApplicationGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    applicationGuid: java.util.UUID
  ): Unit = {
    DeleteQuery.equals("application_guid", applicationGuid)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByApplicationGuids(
    user: java.util.UUID,
    applicationGuids: Seq[java.util.UUID]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByApplicationGuids(c, user, applicationGuids)
    }
  }

  def deleteAllByApplicationGuids(
    c: java.sql.Connection,
    user: java.util.UUID,
    applicationGuids: Seq[java.util.UUID]
  ): Unit = {
    DeleteQuery.in("application_guid", applicationGuids)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteByGuidAndApplicationGuid(
    user: java.util.UUID,
    guidAndApplicationGuid: (java.util.UUID, java.util.UUID)
  ): Unit = {
    db.withConnection { c =>
      deleteByGuidAndApplicationGuid(c, user, guidAndApplicationGuid)
    }
  }

  def deleteByGuidAndApplicationGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    guidAndApplicationGuid: (java.util.UUID, java.util.UUID)
  ): Unit = {
    DeleteQuery.in2(("guid", "application_guid"), Seq(guidAndApplicationGuid))
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByGuidsAndApplicationGuids(
    user: java.util.UUID,
    guidsAndApplicationGuids: Seq[(java.util.UUID, java.util.UUID)]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByGuidsAndApplicationGuids(c, user, guidsAndApplicationGuids)
    }
  }

  def deleteAllByGuidsAndApplicationGuids(
    c: java.sql.Connection,
    user: java.util.UUID,
    guidsAndApplicationGuids: Seq[(java.util.UUID, java.util.UUID)]
  ): Unit = {
    DeleteQuery.in2(("guid", "application_guid"), guidsAndApplicationGuids)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  private def bindQuery(
    query: io.flow.postgresql.Query,
    user: java.util.UUID,
    form: VersionForm
  ): io.flow.postgresql.Query = {
    query
      .bind("application_guid", form.applicationGuid.toString)
      .bind("version", form.version)
      .bind("version_sort_key", form.versionSortKey)
      .bind("original", form.original)
      .bind("old_json", form.oldJson.map { v => play.api.libs.json.Json.toJson(v).toString })
  }

  private def toNamedParameter(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: VersionForm
  ): Seq[anorm.NamedParameter] = {
    Seq(
      anorm.NamedParameter("guid", guid.toString),
      anorm.NamedParameter("application_guid", form.applicationGuid.toString),
      anorm.NamedParameter("version", form.version),
      anorm.NamedParameter("version_sort_key", form.versionSortKey),
      anorm.NamedParameter("original", form.original),
      anorm.NamedParameter("old_json", form.oldJson.map { v => play.api.libs.json.Json.toJson(v).toString })
    )
  }
}