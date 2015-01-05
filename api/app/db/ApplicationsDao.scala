package db

import com.gilt.apidoc.models.{Error, Organization, Application, ApplicationForm, User, Version, Visibility}
import lib.{UrlKey, Validation}
import anorm._
import play.api.db._
import play.api.libs.json._
import play.api.Play.current
import java.util.UUID

object ApplicationsDao {

  private val BaseQuery = """
    select applications.guid, applications.name, applications.key, applications.description, applications.visibility
      from applications
      join organizations on organizations.guid = applications.organization_guid and organizations.deleted_at is null
     where applications.deleted_at is null
  """

  private val InsertQuery = """
    insert into applications
    (guid, organization_guid, name, description, key, visibility, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {name}, {description}, {key}, {visibility}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private val UpdateQuery = """
    update applications
       set name = {name},
           visibility = {visibility},
           description = {description},
           updated_by_guid = {updated_by_guid}::uuid
     where guid = {guid}::uuid
  """

  private val UpdateVisibilityQuery = """
    update applications
       set visibility = {visibility},
           updated_by_guid = {updated_by_guid}::uuid
     where guid = {guid}::uuid
  """

  def validate(
    org: Organization,
    form: ApplicationForm,
    existing: Option[Application] = None
  ): Seq[Error] = {
    val nameErrors = findByOrganizationAndName(Authorization.All, org, form.name) match {
      case None => Seq.empty
      case Some(application: Application) => {
        if (existing.map(_.guid) == Some(application.guid)) {
          Seq.empty
        } else {
          Seq("Application with this name already exists")
        }
      }
    }

    val keyErrors = form.key match {
      case None => Seq.empty
      case Some(key) => {
        findByOrganizationKeyAndApplicationKey(Authorization.All, org.key, key) match {
          case None => Seq.empty
          case Some(application: Application) => {
            if (existing.map(_.guid) == Some(application.guid)) {
              Seq.empty
            } else {
              Seq("Application with this key already exists")
            }
          }
        }
      }
    }

    val visibilityErrors = Visibility.fromString(form.visibility.toString) match {
      case Some(_) => Seq.empty
      case None => Seq(s"Visibility[${form.visibility}] not recognized")
    }

    Validation.errors(nameErrors ++ keyErrors ++ visibilityErrors)
  }

  def update(
    updatedBy: User,
    app: Application,
    form: ApplicationForm
  ): Application = {
    DB.withConnection { implicit c =>
      SQL(UpdateQuery).on(
        'guid -> app.guid,
        'name -> form.name.trim,
        'visibility -> form.visibility.toString,
        'description -> form.description.map(_.trim),
        'updated_by_guid -> updatedBy.guid
      ).execute()
    }
    findByGuid(Authorization.All, app.guid).getOrElse {
      sys.error("Error updating application")
    }
  }

  def setVisibility(
    updatedBy: User,
    app: Application,
    visibility: Visibility
  ): Application = {
    DB.withConnection { implicit c =>
      SQL(UpdateVisibilityQuery).on(
        'guid -> app.guid,
        'visibility -> visibility.toString,
        'updated_by_guid -> updatedBy.guid
      ).execute()
    }
    findByGuid(Authorization.All, app.guid).getOrElse {
      sys.error("Error updating visibility")
    }
  }

  def create(
    createdBy: User,
    org: Organization,
    form: ApplicationForm,
    keyOption: Option[String] = None
  ): Application = {
    val guid = UUID.randomUUID
    val key = keyOption.getOrElse(UrlKey.generate(form.name))
    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'organization_guid -> org.guid,
        'name -> form.name,
        'description -> form.description,
        'key -> key,
        'visibility -> form.visibility.toString,
        'created_by_guid -> createdBy.guid,
        'updated_by_guid -> createdBy.guid
      ).execute()
    }

    global.Actors.mainActor ! actors.MainActor.Messages.ApplicationCreated(guid)

    findAll(Authorization.All, orgKey = Some(org.key), key = Some(key)).headOption.getOrElse {
      sys.error("Failed to create application")
    }
  }

  def softDelete(deletedBy: User, application: Application) {
    SoftDelete.delete("applications", deletedBy, application.guid)
  }

  private[db] def findByOrganizationAndName(authorization: Authorization, org: Organization, name: String): Option[Application] = {
    findAll(authorization, orgKey = Some(org.key), name = Some(name), limit = 1).headOption
  }

  def findByOrganizationKeyAndApplicationKey(authorization: Authorization, orgKey: String, applicationKey: String): Option[Application] = {
    findAll(authorization, orgKey = Some(orgKey), key = Some(applicationKey), limit = 1).headOption
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[Application] = {
    findAll(authorization, guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    authorization: Authorization,
    orgKey: Option[String] = None,
    guid: Option[UUID] = None,
    name: Option[String] = None,
    key: Option[String] = None,
    version: Option[Version] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Application] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      authorization.applicationFilter().map(v => "and " + v),
      guid.map { v => "and applications.guid = {guid}::uuid" },
      orgKey.map { v => "and organizations.key = {organization_key}" },
      name.map { v => "and lower(trim(applications.name)) = lower(trim({name}))" },
      key.map { v => "and applications.key = lower(trim({key}))" },
      version.map { v => "and applications.guid = (select application_guid from versions where deleted_at is null and versions.guid = {version_guid}::uuid)" },
      Some(s"order by lower(applications.name) limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val authorizationUserGuid = authorization match {
      case Authorization.User(guid) => Some(guid)
      case _ => None
    }

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      orgKey.map('organization_key -> _),
      name.map('name -> _),
      key.map('key -> _),
      version.map('version_guid -> _.guid.toString)
    ).flatten ++ authorization.bindVariables

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row,
    prefix: Option[String] = None
  ): Application = {
    val p = prefix.map( _ + "_").getOrElse("")
    Application(
      guid = row[UUID](s"${p}guid"),
      name = row[String](s"${p}name"),
      key = row[String](s"${p}key"),
      visibility = Visibility(row[String](s"${p}visibility")),
      description = row[Option[String]](s"${p}description")
    )
  }

}
