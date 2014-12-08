package db

import com.gilt.apidoc.models.{Error, Organization, Service, Watch, WatchForm, User}
import anorm._
import lib.{Validation, ValidationError}
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

case class FullWatchForm(
  createdBy: User,
  form: WatchForm
) {

  private val auth = Authorization(Some(createdBy))
  val org: Option[Organization] = OrganizationDao.findByKey(auth, form.organizationKey)
  val service: Option[Service] = org.flatMap { o =>
    ServiceDao.findByOrganizationKeyAndServiceKey(auth, o.key, form.serviceKey)
  }

  val user = UserDao.findByGuid(form.userGuid)

  lazy val validate: Seq[ValidationError] = {
    val serviceKeyErrors = service match {
      case None => Seq(s"Service[${form.serviceKey}] not found")
      case Some(service) => Seq.empty
    }

    val userErrors = user match {
        case None => Seq("User not found")
        case Some(_) => Seq.empty
    }

    Validation.errors(serviceKeyErrors ++ userErrors)
  }

}

object WatchDao {

  private val BaseQuery = """
    select watches.guid,
           users.guid as user_guid,
           users.email as user_email,
           users.name as user_name,
           services.guid as service_guid,
           services.name as service_name,
           services.key as service_key,
           services.visibility as service_visibility,
           services.description as service_description,
           organizations.guid as organization_guid,
           organizations.key as organization_key,
           organizations.name as organization_name
      from watches
      join users on users.guid = watches.user_guid and users.deleted_at is null
      join services on services.guid = watches.service_guid and services.deleted_at is null
      join organizations on organizations.guid = services.organization_guid and organizations.deleted_at is null
     where watches.deleted_at is null
  """

  private val InsertQuery = """
    insert into watches
    (guid, user_guid, service_guid, created_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {service_guid}::uuid, {created_by_guid}::uuid)
  """

  def create(createdBy: User, fullForm: FullWatchForm): Watch = {
    val errors = fullForm.validate
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    val service = fullForm.service.getOrElse {
      sys.error(s"Cannot find service[${fullForm.form.organizationKey}/${fullForm.form.serviceKey}]")
    }

    val guid = UUID.randomUUID

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'user_guid -> fullForm.form.userGuid,
        'service_guid -> service.guid,
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    findByGuid(Authorization.All, guid).getOrElse {
      sys.error("Failed to create watch")
    }
  }

  def softDelete(deletedBy: User, watch: Watch) {
    SoftDelete.delete("watches", deletedBy, watch.guid)
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[Watch] = {
    findAll(authorization, guid = Some(guid), limit = 1).headOption
  }

  def findByUserAndGuid(user: User, guid: UUID): Option[Watch] = {
    findByGuid(Authorization(Some(user)), guid)
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    service: Option[Service] = None,
    serviceKey: Option[String] = None,
    userGuid: Option[UUID] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Watch] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and watches.guid = {guid}::uuid" },
      userGuid.map { v => "and watches.user_guid = {user_guid}::uuid" },
      service.map { v => "and watches.service_guid = {service_guid}::uuid" },
      serviceKey.map { v => "and watches.service_guid = (select guid from services where deleted_at is null and key = lower(trim({service_key})))" },
      Some(s"order by services.key, watches.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")


    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      userGuid.map('user_guid -> _.toString),
      service.map('service_guid -> _.guid.toString),
      serviceKey.map('service_key -> _)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ): Watch = {
    Watch(
      guid = row[UUID]("guid"),
      user = UserDao.fromRow(row, Some("user")),
      organization = OrganizationDao.summaryFromRow(row, Some("organization")),
      service = ServiceDao.fromRow(row, Some("service"))
    )
  }

}
