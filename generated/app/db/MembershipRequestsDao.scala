package db.generated

case class MembershipRequest(
  guid: java.util.UUID,
  userGuid: java.util.UUID,
  organizationGuid: java.util.UUID,
  role: String,
  createdAt: org.joda.time.DateTime,
  createdByGuid: java.util.UUID,
  deletedAt: Option[org.joda.time.DateTime],
  deletedByGuid: Option[java.util.UUID]
) {
  def form: MembershipRequestForm = {
    MembershipRequestForm(
      userGuid = userGuid,
      organizationGuid = organizationGuid,
      role = role,
    )
  }
}

case class MembershipRequestForm(
  userGuid: java.util.UUID,
  organizationGuid: java.util.UUID,
  role: String
)

case object MembershipRequestsTable {
  val SchemaName: String = "public"

  val TableName: String = "membership_requests"

  val QualifiedName: String = "public.membership_requests"

  sealed trait Column {
    def name: String
  }

  object Columns {
    case object Guid extends Column {
      override val name: String = "guid"
    }

    case object UserGuid extends Column {
      override val name: String = "user_guid"
    }

    case object OrganizationGuid extends Column {
      override val name: String = "organization_guid"
    }

    case object Role extends Column {
      override val name: String = "role"
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

    val all: List[Column] = List(Guid, UserGuid, OrganizationGuid, Role, CreatedAt, CreatedByGuid, DeletedAt, DeletedByGuid)
  }
}

trait BaseMembershipRequestsDao {
  import anorm.*

  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def db: play.api.db.Database

  private val BaseQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | select guid::text,
     |        user_guid::text,
     |        organization_guid::text,
     |        role,
     |        created_at,
     |        created_by_guid::text,
     |        deleted_at,
     |        deleted_by_guid::text
     |   from public.membership_requests
     |""".stripMargin.stripTrailing
    )
  }

  def findAll(
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    organizationGuid: Option[java.util.UUID] = None,
    organizationGuids: Option[Seq[java.util.UUID]] = None,
    userGuid: Option[java.util.UUID] = None,
    userGuids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[MembershipRequest] = {
    db.withConnection { c =>
      findAllWithConnection(c, guid, guids, organizationGuid, organizationGuids, userGuid, userGuids, limit, offset, orderBy)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    guid: Option[java.util.UUID] = None,
    guids: Option[Seq[java.util.UUID]] = None,
    organizationGuid: Option[java.util.UUID] = None,
    organizationGuids: Option[Seq[java.util.UUID]] = None,
    userGuid: Option[java.util.UUID] = None,
    userGuids: Option[Seq[java.util.UUID]] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: Option[io.flow.postgresql.OrderBy] = None
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Seq[MembershipRequest] = {
    customQueryModifier(BaseQuery)
      .equals("membership_requests.guid", guid)
      .optionalIn("membership_requests.guid", guids)
      .equals("membership_requests.organization_guid", organizationGuid)
      .optionalIn("membership_requests.organization_guid", organizationGuids)
      .equals("membership_requests.user_guid", userGuid)
      .optionalIn("membership_requests.user_guid", userGuids)
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
    userGuid: Option[java.util.UUID] = None,
    userGuids: Option[Seq[java.util.UUID]] = None,
    pageSize: Long = 1000
  )(implicit customQueryModifier: io.flow.postgresql.Query => io.flow.postgresql.Query = identity): Iterator[MembershipRequest] = {
    assert(pageSize > 0, "pageSize must be > 0")

    def iterate(lastValue: Option[MembershipRequest]): Iterator[MembershipRequest] = {
      val page: Seq[MembershipRequest] = db.withConnection { c =>
        customQueryModifier(BaseQuery)
          .equals("membership_requests.guid", guid)
          .optionalIn("membership_requests.guid", guids)
          .equals("membership_requests.organization_guid", organizationGuid)
          .optionalIn("membership_requests.organization_guid", organizationGuids)
          .equals("membership_requests.user_guid", userGuid)
          .optionalIn("membership_requests.user_guid", userGuids)
          .greaterThan("membership_requests.guid", lastValue.map(_.guid))
          .orderBy("membership_requests.guid")
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

  def findByGuid(guid: java.util.UUID): Option[MembershipRequest] = {
    db.withConnection { c =>
      findByGuidWithConnection(c, guid)
    }
  }

  def findByGuidWithConnection(
    c: java.sql.Connection,
    guid: java.util.UUID
  ): Option[MembershipRequest] = {
    findAllWithConnection(
      c = c,
      guid = Some(guid),
      limit = Some(1)
    ).headOption
  }

  def findAllByOrganizationGuid(organizationGuid: java.util.UUID): Seq[MembershipRequest] = {
    db.withConnection { c =>
      findAllByOrganizationGuidWithConnection(c, organizationGuid)
    }
  }

  def findAllByOrganizationGuidWithConnection(
    c: java.sql.Connection,
    organizationGuid: java.util.UUID
  ): Seq[MembershipRequest] = {
    findAllWithConnection(
      c = c,
      organizationGuid = Some(organizationGuid),
      limit = None
    )
  }

  def findAllByUserGuid(userGuid: java.util.UUID): Seq[MembershipRequest] = {
    db.withConnection { c =>
      findAllByUserGuidWithConnection(c, userGuid)
    }
  }

  def findAllByUserGuidWithConnection(
    c: java.sql.Connection,
    userGuid: java.util.UUID
  ): Seq[MembershipRequest] = {
    findAllWithConnection(
      c = c,
      userGuid = Some(userGuid),
      limit = None
    )
  }

  private val parser: anorm.RowParser[MembershipRequest] = {
    anorm.SqlParser.str("guid") ~
      anorm.SqlParser.str("user_guid") ~
      anorm.SqlParser.str("organization_guid") ~
      anorm.SqlParser.str("role") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("created_at") ~
      anorm.SqlParser.str("created_by_guid") ~
      anorm.SqlParser.get[org.joda.time.DateTime]("deleted_at").? ~
      anorm.SqlParser.str("deleted_by_guid").? map { case guid ~ userGuid ~ organizationGuid ~ role ~ createdAt ~ createdByGuid ~ deletedAt ~ deletedByGuid =>
      MembershipRequest(
        guid = java.util.UUID.fromString(guid),
        userGuid = java.util.UUID.fromString(userGuid),
        organizationGuid = java.util.UUID.fromString(organizationGuid),
        role = role,
        createdAt = createdAt,
        createdByGuid = java.util.UUID.fromString(createdByGuid),
        deletedAt = deletedAt,
        deletedByGuid = deletedByGuid.map { v => java.util.UUID.fromString(v) }
      )
    }
  }
}

class MembershipRequestsDao @javax.inject.Inject() (override val db: play.api.db.Database) extends BaseMembershipRequestsDao {
  import anorm.JodaParameterMetaData.*

  import anorm.postgresql.*

  def randomPkey: java.util.UUID = {
    java.util.UUID.randomUUID
  }

  private val InsertQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | insert into public.membership_requests
     | (guid, user_guid, organization_guid, role, created_at, created_by_guid)
     | values
     | ({guid}::uuid, {user_guid}::uuid, {organization_guid}::uuid, {role}, {created_at}::timestamptz, {created_by_guid}::uuid)
    """.stripMargin)
  }

  private val UpdateQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("""
     | update public.membership_requests
     | set user_guid = {user_guid}::uuid,
     |     organization_guid = {organization_guid}::uuid,
     |     role = {role}
     | where guid = {guid}::uuid
    """.stripMargin)
  }

  private val DeleteQuery: io.flow.postgresql.Query = {
    io.flow.postgresql.Query("update public.membership_requests set deleted_at = {deleted_at}::timestamptz, deleted_by_guid = {deleted_by_guid}::uuid").isNull("deleted_at")
  }

  def insert(
    user: java.util.UUID,
    form: MembershipRequestForm
  ): java.util.UUID = {
    db.withConnection { c =>
      insert(c, user, form)
    }
  }

  def insert(
    c: java.sql.Connection,
    user: java.util.UUID,
    form: MembershipRequestForm
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
    forms: Seq[MembershipRequestForm]
  ): Seq[java.util.UUID] = {
    db.withConnection { c =>
      insertBatch(c, user, forms)
    }
  }

  def insertBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[MembershipRequestForm]
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
    membershipRequest: MembershipRequest,
    form: MembershipRequestForm
  ): Unit = {
    db.withConnection { c =>
      update(c, user, membershipRequest, form)
    }
  }

  def update(
    c: java.sql.Connection,
    user: java.util.UUID,
    membershipRequest: MembershipRequest,
    form: MembershipRequestForm
  ): Unit = {
    updateByGuid(
      c = c,
      user = user,
      guid = membershipRequest.guid,
      form = form
    )
  }

  def updateByGuid(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: MembershipRequestForm
  ): Unit = {
    db.withConnection { c =>
      updateByGuid(c, user, guid, form)
    }
  }

  def updateByGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    guid: java.util.UUID,
    form: MembershipRequestForm
  ): Unit = {
    bindQuery(UpdateQuery, user, form)
      .bind("guid", guid)
      .execute(c)
    ()
  }

  def updateBatch(
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, MembershipRequestForm)]
  ): Unit = {
    db.withConnection { c =>
      updateBatch(c, user, forms)
    }
  }

  def updateBatch(
    c: java.sql.Connection,
    user: java.util.UUID,
    forms: Seq[(java.util.UUID, MembershipRequestForm)]
  ): Unit = {
    forms.map { case (guid, f) => toNamedParameter(user, guid, f) }.toList match {
      case Nil => // no-op
      case first :: rest => anorm.BatchSql(UpdateQuery.sql(), first, rest*).execute()(c)
    }
  }

  def delete(
    user: java.util.UUID,
    membershipRequest: MembershipRequest
  ): Unit = {
    db.withConnection { c =>
      delete(c, user, membershipRequest)
    }
  }

  def delete(
    c: java.sql.Connection,
    user: java.util.UUID,
    membershipRequest: MembershipRequest
  ): Unit = {
    deleteByGuid(
      c = c,
      user = user,
      guid = membershipRequest.guid
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

  def deleteAllByUserGuid(
    user: java.util.UUID,
    userGuid: java.util.UUID
  ): Unit = {
    db.withConnection { c =>
      deleteAllByUserGuid(c, user, userGuid)
    }
  }

  def deleteAllByUserGuid(
    c: java.sql.Connection,
    user: java.util.UUID,
    userGuid: java.util.UUID
  ): Unit = {
    DeleteQuery.equals("user_guid", userGuid)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  def deleteAllByUserGuids(
    user: java.util.UUID,
    userGuids: Seq[java.util.UUID]
  ): Unit = {
    db.withConnection { c =>
      deleteAllByUserGuids(c, user, userGuids)
    }
  }

  def deleteAllByUserGuids(
    c: java.sql.Connection,
    user: java.util.UUID,
    userGuids: Seq[java.util.UUID]
  ): Unit = {
    DeleteQuery.in("user_guid", userGuids)
      .bind("deleted_at", org.joda.time.DateTime.now)
      .bind("deleted_by_guid", user)
      .execute(c)
  }

  private def bindQuery(
    query: io.flow.postgresql.Query,
    user: java.util.UUID,
    form: MembershipRequestForm
  ): io.flow.postgresql.Query = {
    query
      .bind("user_guid", form.userGuid.toString)
      .bind("organization_guid", form.organizationGuid.toString)
      .bind("role", form.role)
  }

  private def toNamedParameter(
    user: java.util.UUID,
    guid: java.util.UUID,
    form: MembershipRequestForm
  ): Seq[anorm.NamedParameter] = {
    Seq(
      anorm.NamedParameter("guid", guid.toString),
      anorm.NamedParameter("user_guid", form.userGuid.toString),
      anorm.NamedParameter("organization_guid", form.organizationGuid.toString),
      anorm.NamedParameter("role", form.role)
    )
  }
}