package db.generated

case class OrganizationAttributeValue(
  guid: java.util.UUID,
  organizationGuid: java.util.UUID,
  attributeGuid: java.util.UUID,
  value: String,
  createdAt: org.joda.time.DateTime,
  createdByGuid: java.util.UUID,
  updatedAt: org.joda.time.DateTime,
  updatedByGuid: java.util.UUID,
  deletedAt: Option[org.joda.time.DateTime],
  deletedByGuid: Option[java.util.UUID]
) {
  def form: OrganizationAttributeValueForm = {
    OrganizationAttributeValueForm(
      organizationGuid = organizationGuid,
      attributeGuid = attributeGuid,
      value = value,
    )
  }
}

case class OrganizationAttributeValueForm(
  organizationGuid: java.util.UUID,
  attributeGuid: java.util.UUID,
  value: String
)

case object OrganizationAttributeValuesTable {
  val SchemaName: String = "public"

  val TableName: String = "organization_attribute_values"

  val QualifiedName: String = "public.organization_attribute_values"

  sealed trait Column {
    def name: String
  }

  object Columns {
    case object Guid extends Column {
      override val name: String = "guid"
    }

    case object OrganizationGuid extends Column {
      override val name: String = "organization_guid"
    }

    case object AttributeGuid extends Column {
      override val name: String = "attribute_guid"
    }

    case object Value extends Column {
      override val name: String = "value"
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

    val all: List[Column] = List(Guid, OrganizationGuid, AttributeGuid, Value, CreatedAt, CreatedByGuid, UpdatedAt, UpdatedByGuid, DeletedAt, DeletedByGuid)
  }
}

trait BaseOrganizationAttributeValuesDao {
  import anorm.*

  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def db: play.api.db.Database

  private val BaseQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | select guid::text,
     |        organization_guid::text,
     |        attribute_guid::text,
     |        value,
     |        created_at,
     |        created_by_guid::text,
     |        updated_at,
     |        updated_by_guid::text,
     |        deleted_at,
     |        deleted_by_guid::text
     |   from public.organization_attribute_values
     |""".stripMargin.stripTrailing
    )
  }

  def findAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    attributeGuid: Option[java.util.UUID] = None,
    attributeGuids: Option[Seq[java.util.UUID]] = None,
    organizationGuid: Option[java.util.UUID] = None,
    organizationGuids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[OrganizationAttributeValue] = {
    db.withConnection { c =>
      findAllWithConnection(c, guid, guids, attributeGuid, attributeGuids, organizationGuid, organizationGuids, limit, offset, orderBy)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    attributeGuid: Option[java.util.UUID] = None,
    attributeGuids: Option[Seq[java.util.UUID]] = None,
    organizationGuid: Option[java.util.UUID] = None,
    organizationGuids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[OrganizationAttributeValue] = {
    customQueryModifier(BaseQuery)
      .equals("organization_attribute_values.guid", guid)
      .optionalIn("organization_attribute_values.guid", guids)
      .equals("organization_attribute_values.attribute_guid", attributeGuid)
      .optionalIn("organization_attribute_values.attribute_guid", attributeGuids)
      .equals("organization_attribute_values.organization_guid", organizationGuid)
      .optionalIn("organization_attribute_values.organization_guid", organizationGuids)
      .optionalLimit(limit)
      .offset(offset)
      .orderBy(orderBy.flatMap(_.sql))
      .as(parser.*)(c)
  }

  def iterateAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    attributeGuid: Option[java.util.UUID] = None,
    attributeGuids: Option[Seq[java.util.UUID]] = None,
    organizationGuid: Option[java.util.UUID] = None,
    organizationGuids: Option[Seq[java.util.UUID]] = None,
    pageSize: Long = 1000
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Iterator[OrganizationAttributeValue] = {
    assert(pageSize > 0, "pageSize must be > 0")

    def iterate(lastValue: Option[OrganizationAttributeValue]): Iterator[OrganizationAttributeValue] = {
      val page: Seq[OrganizationAttributeValue] = db.withConnection { c =>
        customQueryModifier(BaseQuery)
          .equals("organization_attribute_values.guid", guid)
          .optionalIn("organization_attribute_values.guid", guids)
          .equals("organization_attribute_values.attribute_guid", attributeGuid)
          .optionalIn("organization_attribute_values.attribute_guid", attributeGuids)
          .equals("organization_attribute_values.organization_guid", organizationGuid)
          .optionalIn("organization_attribute_values.organization_guid", organizationGuids)
          .greaterThan("organization_attribute_values.guid", lastValue.map(_.guid))
          .orderBy("organization_attribute_values.guid")
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

  def findByGuid(guid: java.util.UUID): Option[OrganizationAttributeValue] = {
    db.withConnection { c =>
      findByGuidWithConnection(c, guid)
    }
  }

  def findByGuidWithConnection(
    c: java.sql.Connection,
    guid: java.util.UUID
  ): Option[OrganizationAttributeValue] = {
    findAllWithConnection(
      c = c,
      guid = Some(guid),
      limit = Some(1)
    ).headOption
  }

  def findAllByAttributeGuid(attributeGuid: java.util.UUID): Seq[OrganizationAttributeValue] = {
    db.withConnection { c =>
      findAllByAttributeGuidWithConnection(c, attributeGuid)
    }
  }

  def findAllByAttributeGuidWithConnection(
    c: java.sql.Connection,
    attributeGuid: java.util.UUID
  ): Seq[OrganizationAttributeValue] = {
    findAllWithConnection(
      c = c,
      attributeGuid = Some(attributeGuid),
      limit = None
    )
  }

  def findAllByOrganizationGuid(organizationGuid: java.util.UUID): Seq[OrganizationAttributeValue] = {
    db.withConnection { c =>
      findAllByOrganizationGuidWithConnection(c, organizationGuid)
    }
  }

  def findAllByOrganizationGuidWithConnection(
    c: java.sql.Connection,
    organizationGuid: java.util.UUID
  ): Seq[OrganizationAttributeValue] = {
    findAllWithConnection(
      c = c,
      organizationGuid = Some(organizationGuid),
      limit = None
    )
  }

  private val parser: anorm.RowParser[OrganizationAttributeValue] = {
    anorm.SqlParser.str("guid") ~
      anorm.SqlParser.str("organization_guid") ~
      anorm.SqlParser.str("attribute_guid") ~
      anorm.SqlParser.str("value") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("created_at") ~
      anorm.SqlParser.str("created_by_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("updated_at") ~
      anorm.SqlParser.str("updated_by_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("deleted_at").? ~
      anorm.SqlParser.str("deleted_by_guid").? map { case guid ~ organizationGuid ~ attributeGuid ~ value ~ createdAt ~ createdByGuid ~ updatedAt ~ updatedByGuid ~ deletedAt ~ deletedByGuid =>
      OrganizationAttributeValue(
        guid = java.util.UUID.fromString(guid),
        organizationGuid = java.util.UUID.fromString(organizationGuid),
        attributeGuid = java.util.UUID.fromString(attributeGuid),
        value = value,
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

class OrganizationAttributeValuesDao @javax.inject.Inject() (override val db: play.api.db.Database) extends BaseOrganizationAttributeValuesDao {
  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def randomPkey: java.util.UUID = {
    java.util.UUID.randomUUID
  }

  private val InsertQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | insert into public.organization_attribute_values
     | (guid, organization_guid, attribute_guid, value, created_at, created_by_guid, updated_at, updated_by_guid)
     | values
     | ({guid}::uuid, {organization_guid}::uuid, {attribute_guid}::uuid, {value}, {created_at}::timestamptz, {created_by_guid}::uuid, {updated_at}::timestamptz, {updated_by_guid}::uuid)
    """.stripMargin)
  }

  private val UpdateQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | update public.organization_attribute_values
     | set organization_guid = {organization_guid}::uuid,
     |     attribute_guid = {attribute_guid}::uuid,
     |     value = {value},
     |     updated_at = {updated_at}::timestamptz,
     |     updated_by_guid = {updated_by_guid}::uuid
     | where guid = {guid}::uuid
    """.stripMargin)
  }

  private val DeleteQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("update public.organization_attribute_values set deleted_at = {deleted_at}::timestamptz, deleted_by_guid = {deleted_by_guid}::uuid").isNull("deleted_at")
  }

  def insert(
    user: java.util.UUID,
    form: OrganizationAttributeValueForm
  ): java.util.UUID = {
    db.withConnection { c =>
      insert(c, user, form)
    }
  }

  def insert(
    c: java.sql.Connection,
    user: java.util.UUID,
    form: OrganizationAttributeValueForm
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
    forms: Seq[OrganizationAttributeValueForm]
  ): Seq[java.util.UUID] = {
    db.withConnection { c =>
      insertBatch(c, user, forms)
    }
  }

  def insertBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[OrganizationAttributeValueForm]
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
    organizationAttributeValue: OrganizationAttributeValue,
    form: OrganizationAttributeValueForm
  ): Unit = {
    db.withConnection { c =>
      update(c, user, organizationAttributeValue, form)
    }
  }

  def update(
    c: java.sql.Connection,
    user: java.util.UUID,
    organizationAttributeValue: OrganizationAttributeValue,
    form: OrganizationAttributeValueForm
  ): Unit = {
    updateByGuid(
      c = c,
      user = user,
      guid = organizationAttributeValue.guid,
      form = form
    )
  }

  def updateByGuid(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: OrganizationAttributeValueForm
  ): Unit = {
    db.withConnection { c =>
      updateByGuid(c, user, guid, form)
    }
  }

  def updateByGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    guid: java.util.UUID,
    form: OrganizationAttributeValueForm
  ): Unit = {
    bindQuery(UpdateQuery, user, form)
      .bind("guid", guid)
      .bind("updated_by_guid", user)
      .execute(c)
    ()
  }

  def updateBatch(
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, OrganizationAttributeValueForm)]
  ): Unit = {
    db.withConnection { c =>
      updateBatch(c, user, forms)
    }
  }

  def updateBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, OrganizationAttributeValueForm)]
  ): Unit = {
    forms.map { case (guid, f) => toNamedParameter(user, guid, f) }.toList match {
      case Nil => // no-op
      case first :: rest => anorm.BatchSql(UpdateQuery.sql(), first, rest*).execute()(c)
    }
  }

  def delete(
    user: java.util.UUID,
    organizationAttributeValue: OrganizationAttributeValue
  ): Unit = {
    db.withConnection { c =>
      delete(c, user, organizationAttributeValue)
    }
  }

  def delete(
    c: java.sql.Connection,
    user: java.util.UUID,
    organizationAttributeValue: OrganizationAttributeValue
  ): Unit = {
    deleteByGuid(
      c = c,
      user = user,
      guid = organizationAttributeValue.guid
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

  def deleteAllByAttributeGuid(
    user: java.util.UUID,
    attributeGuid: java.util.UUID
  ): Unit = {
    db.withConnection { c =>
      deleteAllByAttributeGuid(c, user, attributeGuid)
    }
  }

  def deleteAllByAttributeGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    attributeGuid: java.util.UUID
  ): Unit = {
    DeleteQuery.equals("attribute_guid", attributeGuid)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByAttributeGuids(
    user: java.util.UUID,
    attributeGuids: Seq[java.util.UUID]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByAttributeGuids(c, user, attributeGuids)
    }
  }

  def deleteAllByAttributeGuids(
    c: java.sql.Connection,
    user: java.util.UUID,
    attributeGuids: Seq[java.util.UUID]
  ): Unit = {
    DeleteQuery.in("attribute_guid", attributeGuids)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByOrganizationGuid(
    user: java.util.UUID,
    organizationGuid: java.util.UUID
  ): Unit = {
    db.withConnection { c =>
      deleteAllByOrganizationGuid(c, user, organizationGuid)
    }
  }

  def deleteAllByOrganizationGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    organizationGuid: java.util.UUID
  ): Unit = {
    DeleteQuery.equals("organization_guid", organizationGuid)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByOrganizationGuids(
    user: java.util.UUID,
    organizationGuids: Seq[java.util.UUID]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByOrganizationGuids(c, user, organizationGuids)
    }
  }

  def deleteAllByOrganizationGuids(
    c: java.sql.Connection,
    user: java.util.UUID,
    organizationGuids: Seq[java.util.UUID]
  ): Unit = {
    DeleteQuery.in("organization_guid", organizationGuids)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  private def bindQuery(
    query: io.flow.postgresql.Query,
    user: java.util.UUID,
    form: OrganizationAttributeValueForm
  ): io.flow.postgresql.Query = {
    query
      .bind("organization_guid", form.organizationGuid.toString)
      .bind("attribute_guid", form.attributeGuid.toString)
      .bind("value", form.value)
      .bind("updated_at", org.joda.time.DateTime.now)
      .bind("updated_by_guid", user)
  }

  private def toNamedParameter(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: OrganizationAttributeValueForm
  ): Seq[anorm.NamedParameter] = {
    Seq(
      anorm.NamedParameter("guid", guid.toString),
      anorm.NamedParameter("organization_guid", form.organizationGuid.toString),
      anorm.NamedParameter("attribute_guid", form.attributeGuid.toString),
      anorm.NamedParameter("value", form.value),
      anorm.NamedParameter("updated_at", org.joda.time.DateTime.now),
      anorm.NamedParameter("updated_by_guid", user.toString)
    )
  }
}