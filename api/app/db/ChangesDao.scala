package db

import com.gilt.apidoc.api.v0.models.{Error, Organization, Application, Change, User}
import anorm._
import lib.Validation
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object ChangeesDao {

  private val BaseQuery = """
    select changes.guid,
           users.guid as user_guid,
           users.email as user_email,
           users.nickname as user_nickname,
           users.name as user_name,
           applications.guid as application_guid,
           applications.name as application_name,
           applications.key as application_key,
           applications.visibility as application_visibility,
           applications.description as application_description
           organizations.guid as organization_guid,
           organizations.key as organization_key,
           organizations.name as organization_name,
           organizations.namespace as organization_namespace,
           organizations.visibility as organization_visibility
      from changees
      join users on users.guid = changees.user_guid and users.deleted_at is null
      join applications on applications.guid = changees.application_guid and applications.deleted_at is null
      join organizations on organizations.guid = applications.organization_guid and organizations.deleted_at is null
     where changees.deleted_at is null
  """

  private val InsertQuery = """
    insert into changees
    (guid, user_guid, application_guid, created_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {application_guid}::uuid, {created_by_guid}::uuid)
  """

  def upsert(createdBy: User, fullForm: FullChangeForm): Change = {
    val errors = fullForm.validate
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    val application = fullForm.application.getOrElse {
      sys.error(s"Cannot find application[${fullForm.form.organizationKey}/${fullForm.form.applicationKey}]")
    }

    val guid = UUID.randomUUID

    Try(
      DB.withConnection { implicit c =>
        SQL(InsertQuery).on(
          'guid -> guid,
          'user_guid -> fullForm.form.userGuid,
          'application_guid -> application.guid,
          'created_by_guid -> createdBy.guid
        ).execute()
      }
    ) match {
      case Success(_) => {
        findByGuid(Authorization.All, guid).getOrElse {
          sys.error("Failed to create change")
        }
      }
      case Failure(e) => e match {
        case e: PSQLException => {
          findAll(
            Authorization.All,
            userGuid = Some(fullForm.form.userGuid),
            organizationKey = Some(fullForm.org.get.key),
            application = Some(application),
            limit = 1
          ).headOption.getOrElse {
            sys.error(s"Failed to create change")
          }
        }
      }
    }
  }

  def softDelete(deletedBy: User, change: Change) {
    SoftDelete.delete("changees", deletedBy, change.guid)
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[Change] = {
    findAll(authorization, guid = Some(guid), limit = 1).headOption
  }

  def findByUserAndGuid(user: User, guid: UUID): Option[Change] = {
    findByGuid(Authorization.User(user.guid), guid)
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    organizationKey: Option[String] = None,
    application: Option[Application] = None,
    applicationKey: Option[String] = None,
    userGuid: Option[UUID] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Change] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      authorization.organizationFilter().map(v => "and " + v),
      guid.map { v => "and changees.guid = {guid}::uuid" },
      userGuid.map { v => "and changees.user_guid = {user_guid}::uuid" },
      organizationKey.map { v => "and organizations.key = lower(trim({organization_key}))" },
      application.map { v => "and changees.application_guid = {application_guid}::uuid" },
      applicationKey.map { v => "and applications.key = lower(trim({application_key}))" },
      Some(s"order by applications.key, changees.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")


    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      userGuid.map('user_guid -> _.toString),
      organizationKey.map('organization_key -> _),
      application.map('application_guid -> _.guid.toString),
      applicationKey.map('application_key -> _)
    ).flatten ++ authorization.bindVariables

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ): Change = {
    Change(
      guid = row[UUID]("guid"),
      user = UsersDao.fromRow(row, Some("user")),
      organization = OrganizationsDao.summaryFromRow(row, Some("organization")),
      application = ApplicationsDao.fromRow(row, Some("application"))
    )
  }

}
