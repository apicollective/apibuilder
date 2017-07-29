package db

import anorm._
import io.apibuilder.api.v0.models.{Application, Error, Organization, User, Watch, WatchForm}
import io.flow.postgresql.Query
import lib.Validation
import play.api.db._
import play.api.Play.current
import javax.inject.{Inject, Singleton}
import java.util.UUID
import org.postgresql.util.PSQLException
import scala.util.{Failure, Success, Try}

case class FullWatchForm(
  createdBy: User,
  form: WatchForm
) {

  // TODO: Inject directly
  private[this] def applicationsDao = play.api.Play.current.injector.instanceOf[ApplicationsDao]
  private[this] def organizationsDao = play.api.Play.current.injector.instanceOf[OrganizationsDao]
  private[this] def usersDao = play.api.Play.current.injector.instanceOf[UsersDao]

  private[this] val auth = Authorization.User(createdBy.guid)

  val org: Option[Organization] = organizationsDao.findByKey(auth, form.organizationKey)
  val application: Option[Application] = org.flatMap { o =>
    applicationsDao.findByOrganizationKeyAndApplicationKey(auth, o.key, form.applicationKey)
  }

  private[this] lazy val user = usersDao.findByGuid(form.userGuid)

  lazy val validate: Seq[Error] = {
    val applicationKeyErrors = application match {
      case None => Seq(s"Application[${form.applicationKey}] not found")
      case Some(_) => Nil
    }

    val userErrors = user match {
        case None => Seq("User not found")
        case Some(_) => Nil
    }

    Validation.errors(applicationKeyErrors ++ userErrors)
  }

}

@Singleton
class WatchesDao @Inject() (
  applicationsDao: ApplicationsDao,
  organizationsDao: OrganizationsDao,
  usersDao: UsersDao
) {

  private[this] val BaseQuery = Query(s"""
    select watches.guid,
           ${AuditsParserDao.queryCreationDefaultingUpdatedAt("watches")},
           users.guid as user_guid,
           users.email as user_email,
           users.nickname as user_nickname,
           users.name as user_name,
           ${AuditsParserDao.queryWithAlias("users", "user")},
           applications.guid as application_guid,
           applications.name as application_name,
           applications.key as application_key,
           applications.visibility as application_visibility,
           applications.description as application_description,
           ${AuditsParserDao.queryWithAlias("applications", "application")},
           organizations.guid as organization_guid,
           organizations.key as organization_key,
           organizations.name as organization_name,
           organizations.namespace as organization_namespace,
           organizations.visibility as organization_visibility,
           '[]' as organization_domains;
           ${AuditsParserDao.queryWithAlias("organizations", "organization")}
      from watches
      join users on users.guid = watches.user_guid and users.deleted_at is null
      join applications on applications.guid = watches.application_guid and applications.deleted_at is null
      join organizations on organizations.guid = applications.organization_guid and organizations.deleted_at is null
  """)

  private[this] val InsertQuery = """
    insert into watches
    (guid, user_guid, application_guid, created_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {application_guid}::uuid, {created_by_guid}::uuid)
  """

  def upsert(createdBy: User, fullForm: FullWatchForm): Watch = {
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
          sys.error("Failed to create watch")
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
    findByGuid(Authorization.User(user.guid), guid)
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    organizationKey: Option[String] = None,
    application: Option[Application] = None,
    applicationKey: Option[String] = None,
    userGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Watch] = {
    DB.withConnection { implicit c =>
      Authorization2(authorization).applicationFilter(BaseQuery).
        equals("watches.guid::uuid", guid).
        equals("organizations.key", organizationKey).
        equals("watches.application_guid::uuid", application.map(_.guid)).
        equals("applications.key", applicationKey).
        equals("watches.user_guid::uuid", userGuid).
        and(isDeleted.map(Filters2.isDeleted("watches", _))).
        orderBy("applications.key, watches.created_at").
        limit(limit).
        offset(offset).
        as(io.apibuilder.api.v0.anorm.parsers.Watch.parser().*)
    }
  }

}
