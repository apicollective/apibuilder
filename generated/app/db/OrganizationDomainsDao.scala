package db.generated

case class OrganizationDomain(
  guid: java.util.UUID,
  organizationGuid: java.util.UUID,
  domain: String,
  createdAt: org.joda.time.DateTime,
  createdByGuid: java.util.UUID,
  deletedAt: Option[org.joda.time.DateTime],
  deletedByGuid: Option[java.util.UUID]
) {
  def form: OrganizationDomainForm = {
    OrganizationDomainForm(
      organizationGuid = organizationGuid,
      domain = domain,
    )
  }
}

case class OrganizationDomainForm(
  organizationGuid: java.util.UUID,
  domain: String
)

case object OrganizationDomainsTable {
  val SchemaName: String = "public"

  val TableName: String = "organization_domains"

  val QualifiedName: String = "public.organization_domains"

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

    case object Domain extends Column {
      override val name: String = "domain"
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

    val all: List[Column] = List(Guid, OrganizationGuid, Domain, CreatedAt, CreatedByGuid, DeletedAt, DeletedByGuid)
  }
}

trait BaseOrganizationDomainsDao {
  import anorm.*

  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def db: play.api.db.Database

  private val BaseQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | select guid::text,
     |        organization_guid::text,
     |        domain,
     |        created_at,
     |        created_by_guid::text,
     |        deleted_at,
     |        deleted_by_guid::text
     |   from public.organization_domains
     |""".stripMargin.stripTrailing
    )
  }

  def findAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    organizationGuid: Option[java.util.UUID] = None,
    organizationGuids: Option[Seq[java.util.UUID]] = None,
    domain: Option[String] = None,
    domains: Option[Seq[String]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[OrganizationDomain] = {
    db.withConnection { c =>
      findAllWithConnection(c, guid, guids, organizationGuid, organizationGuids, domain, domains, limit, offset, orderBy)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    organizationGuid: Option[java.util.UUID] = None,
    organizationGuids: Option[Seq[java.util.UUID]] = None,
    domain: Option[String] = None,
    domains: Option[Seq[String]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[OrganizationDomain] = {
    customQueryModifier(BaseQuery)
      .equals("organization_domains.guid", guid)
      .optionalIn("organization_domains.guid", guids)
      .equals("organization_domains.organization_guid", organizationGuid)
      .optionalIn("organization_domains.organization_guid", organizationGuids)
      .equals("organization_domains.domain", domain)
      .optionalIn("organization_domains.domain", domains)
      .optionalLimit(limit)
      .offset(offset)
      .orderBy(orderBy.flatMap(_.sql))
      .as(parser.*)(c)
  }

  def iterateAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    organizationGuid: Option[java.util.UUID] = None,
    organizationGuids: Option[Seq[java.util.UUID]] = None,
    domain: Option[String] = None,
    domains: Option[Seq[String]] = None,
    pageSize: Long = 1000
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Iterator[OrganizationDomain] = {
    assert(pageSize > 0, "pageSize must be > 0")

    def iterate(lastValue: Option[OrganizationDomain]): Iterator[OrganizationDomain] = {
      val page: Seq[OrganizationDomain] = db.withConnection { c =>
        customQueryModifier(BaseQuery)
          .equals("organization_domains.guid", guid)
          .optionalIn("organization_domains.guid", guids)
          .equals("organization_domains.organization_guid", organizationGuid)
          .optionalIn("organization_domains.organization_guid", organizationGuids)
          .equals("organization_domains.domain", domain)
          .optionalIn("organization_domains.domain", domains)
          .greaterThan("organization_domains.guid", lastValue.map(_.guid))
          .orderBy("organization_domains.guid")
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

  def findByGuid(guid: java.util.UUID): Option[OrganizationDomain] = {
    db.withConnection { c =>
      findByGuidWithConnection(c, guid)
    }
  }

  def findByGuidWithConnection(
    c: java.sql.Connection,
    guid: java.util.UUID
  ): Option[OrganizationDomain] = {
    findAllWithConnection(
      c = c,
      guid = Some(guid),
      limit = Some(1)
    ).headOption
  }

  def findAllByOrganizationGuid(organizationGuid: java.util.UUID): Seq[OrganizationDomain] = {
    db.withConnection { c =>
      findAllByOrganizationGuidWithConnection(c, organizationGuid)
    }
  }

  def findAllByOrganizationGuidWithConnection(
    c: java.sql.Connection,
    organizationGuid: java.util.UUID
  ): Seq[OrganizationDomain] = {
    findAllWithConnection(
      c = c,
      organizationGuid = Some(organizationGuid),
      limit = None
    )
  }

  def findAllByDomain(domain: String): Seq[OrganizationDomain] = {
    db.withConnection { c =>
      findAllByDomainWithConnection(c, domain)
    }
  }

  def findAllByDomainWithConnection(
    c: java.sql.Connection,
    domain: String
  ): Seq[OrganizationDomain] = {
    findAllWithConnection(
      c = c,
      domain = Some(domain),
      limit = None
    )
  }

  private val parser: anorm.RowParser[OrganizationDomain] = {
    anorm.SqlParser.str("guid") ~
      anorm.SqlParser.str("organization_guid") ~
      anorm.SqlParser.str("domain") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("created_at") ~
      anorm.SqlParser.str("created_by_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("deleted_at").? ~
      anorm.SqlParser.str("deleted_by_guid").? map { case guid ~ organizationGuid ~ domain ~ createdAt ~ createdByGuid ~ deletedAt ~ deletedByGuid =>
      OrganizationDomain(
        guid = java.util.UUID.fromString(guid),
        organizationGuid = java.util.UUID.fromString(organizationGuid),
        domain = domain,
        createdAt = createdAt,
        createdByGuid = java.util.UUID.fromString(createdByGuid),
        deletedAt = deletedAt,
        deletedByGuid = deletedByGuid.map { v => java.util.UUID.fromString(v) }
      )
    }
  }
}

class OrganizationDomainsDao @javax.inject.Inject() (override val db: play.api.db.Database) extends BaseOrganizationDomainsDao {
  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def randomPkey: java.util.UUID = {
    java.util.UUID.randomUUID
  }

  private val InsertQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | insert into public.organization_domains
     | (guid, organization_guid, domain, created_at, created_by_guid)
     | values
     | ({guid}::uuid, {organization_guid}::uuid, {domain}, {created_at}::timestamptz, {created_by_guid}::uuid)
    """.stripMargin)
  }

  private val UpdateQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | update public.organization_domains
     | set organization_guid = {organization_guid}::uuid,
     |     domain = {domain}
     | where guid = {guid}::uuid
    """.stripMargin)
  }

  private val DeleteQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("update public.organization_domains set deleted_at = {deleted_at}::timestamptz, deleted_by_guid = {deleted_by_guid}::uuid").isNull("deleted_at")
  }

  def insert(
    user: java.util.UUID,
    form: OrganizationDomainForm
  ): java.util.UUID = {
    db.withConnection { c =>
      insert(c, user, form)
    }
  }

  def insert(
    c: java.sql.Connection,
    user: java.util.UUID,
    form: OrganizationDomainForm
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
    forms: Seq[OrganizationDomainForm]
  ): Seq[java.util.UUID] = {
    db.withConnection { c =>
      insertBatch(c, user, forms)
    }
  }

  def insertBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[OrganizationDomainForm]
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
    organizationDomain: OrganizationDomain,
    form: OrganizationDomainForm
  ): Unit = {
    db.withConnection { c =>
      update(c, user, organizationDomain, form)
    }
  }

  def update(
    c: java.sql.Connection,
    user: java.util.UUID,
    organizationDomain: OrganizationDomain,
    form: OrganizationDomainForm
  ): Unit = {
    updateByGuid(
      c = c,
      user = user,
      guid = organizationDomain.guid,
      form = form
    )
  }

  def updateByGuid(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: OrganizationDomainForm
  ): Unit = {
    db.withConnection { c =>
      updateByGuid(c, user, guid, form)
    }
  }

  def updateByGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    guid: java.util.UUID,
    form: OrganizationDomainForm
  ): Unit = {
    bindQuery(UpdateQuery, user, form)
      .bind("guid", guid)
      .execute(c)
    ()
  }

  def updateBatch(
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, OrganizationDomainForm)]
  ): Unit = {
    db.withConnection { c =>
      updateBatch(c, user, forms)
    }
  }

  def updateBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, OrganizationDomainForm)]
  ): Unit = {
    forms.map { case (guid, f) => toNamedParameter(user, guid, f) }.toList match {
      case Nil => // no-op
      case first :: rest => anorm.BatchSql(UpdateQuery.sql(), first, rest*).execute()(c)
    }
  }

  def delete(
    user: java.util.UUID,
    organizationDomain: OrganizationDomain
  ): Unit = {
    db.withConnection { c =>
      delete(c, user, organizationDomain)
    }
  }

  def delete(
    c: java.sql.Connection,
    user: java.util.UUID,
    organizationDomain: OrganizationDomain
  ): Unit = {
    deleteByGuid(
      c = c,
      user = user,
      guid = organizationDomain.guid
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

  def deleteAllByDomain(
    user: java.util.UUID,
    domain: String
  ): Unit = {
    db.withConnection { c =>
      deleteAllByDomain(c, user, domain)
    }
  }

  def deleteAllByDomain(
    c: java.sql.Connection,
    user: java.util.UUID,
    domain: String
  ): Unit = {
    DeleteQuery.equals("domain", domain)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByDomains(
    user: java.util.UUID,
    domains: Seq[String]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByDomains(c, user, domains)
    }
  }

  def deleteAllByDomains(
    c: java.sql.Connection,
    user: java.util.UUID,
    domains: Seq[String]
  ): Unit = {
    DeleteQuery.in("domain", domains)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  private def bindQuery(
    query: io.flow.postgresql.Query,
    user: java.util.UUID,
    form: OrganizationDomainForm
  ): io.flow.postgresql.Query = {
    query
      .bind("organization_guid", form.organizationGuid.toString)
      .bind("domain", form.domain)
  }

  private def toNamedParameter(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: OrganizationDomainForm
  ): Seq[anorm.NamedParameter] = {
    Seq(
      anorm.NamedParameter("guid", guid.toString),
      anorm.NamedParameter("organization_guid", form.organizationGuid.toString),
      anorm.NamedParameter("domain", form.domain)
    )
  }
}