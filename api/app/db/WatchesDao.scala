package db

import com.gilt.apidoc.models.{Error, Organization, Service, Watch, WatchForm, User}
import anorm._
import lib.Validation
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID
import org.postgresql.util.PSQLException
import scala.util.{Try, Success, Failure}

case class FullWatchForm(
  createdBy: User,
  form: WatchForm
) {

  private val auth = Authorization(Some(createdBy))
  val org: Option[Organization] = OrganizationsDao.findByKey(auth, form.organizationKey)
  val service: Option[Service] = org.flatMap { o =>
    ServicesDao.findByOrganizationKeyAndServiceKey(auth, o.key, form.serviceKey)
  }

  val user = UsersDao.findByGuid(form.userGuid)

  lazy val validate: Seq[Error] = {
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

object WatchesDao {

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

  def upsert(createdBy: User, fullForm: FullWatchForm): Watch = {
    val errors = fullForm.validate
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    val service = fullForm.service.getOrElse {
      sys.error(s"Cannot find service[${fullForm.form.organizationKey}/${fullForm.form.serviceKey}]")
    }

    val guid = UUID.randomUUID

    Try(
      DB.withConnection { implicit c =>
        SQL(InsertQuery).on(
          'guid -> guid,
          'user_guid -> fullForm.form.userGuid,
          'service_guid -> service.guid,
          'created_by_guid -> createdBy.guid
        ).execute()
      }
    ) match {
      case Success(_) => {
        findByGuid(Authorization.All, guid).getOrElse {
          sys.error("Failed to create watch")
        }
      }
      case Failure(e) => e match {
        case e: PSQLException => {
          findAll(
            Authorization.All,
            userGuid = Some(fullForm.form.userGuid),
            organizationKey = Some(fullForm.org.get.key),
            service = Some(service),
            limit = 1
          ).headOption.getOrElse {
            sys.error(s"Failed to create watch")
          }
        }
      }
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
    organizationKey: Option[String] = None,
    service: Option[Service] = None,
    serviceKey: Option[String] = None,
    userGuid: Option[UUID] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Watch] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      authorization.organizationFilter("organizations.guid").map(v => "and " + v),
      guid.map { v => "and watches.guid = {guid}::uuid" },
      userGuid.map { v => "and watches.user_guid = {user_guid}::uuid" },
      organizationKey.map { v => "and organizations.key = lower(trim({organization_key}))" },
      service.map { v => "and watches.service_guid = {service_guid}::uuid" },
      serviceKey.map { v => "and watches.service_guid = (select guid from services where deleted_at is null and key = lower(trim({service_key})))" },
      Some(s"order by services.key, watches.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")


    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      userGuid.map('user_guid -> _.toString),
      organizationKey.map('organization_key -> _),
      service.map('service_guid -> _.guid.toString),
      serviceKey.map('service_key -> _)
    ).flatten ++ authorization.bindVariables

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ): Watch = {
    Watch(
      guid = row[UUID]("guid"),
      user = UsersDao.fromRow(row, Some("user")),
      organization = OrganizationsDao.summaryFromRow(row, Some("organization")),
      service = ServicesDao.fromRow(row, Some("service"))
    )
  }

}
