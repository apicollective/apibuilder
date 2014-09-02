package db

import com.gilt.apidoc.models.{Domain, Organization, OrganizationMetadata, User, Version, Visibility}
import com.gilt.apidoc.models.json._
import core.{Role, UrlKey}
import anorm._
import lib.{Validation, ValidationError}
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

case class OrganizationForm(
  name: String,
  domains: Option[Seq[String]] = None,
  metadata: Option[OrganizationMetadataForm] = None
)

object OrganizationForm {
  implicit val organizationFormReads = Json.reads[OrganizationForm]
}

object OrganizationDao {

  private val MinNameLength = 4

  private val BaseQuery = """
    select organizations.guid, organizations.name, organizations.key,
           organization_metadata.package_name as metadata_package_name,
           (select array_to_string(array_agg(domain), ' ') 
              from organization_domains
             where deleted_at is null
               and organization_guid = organizations.guid) as domains
      from organizations
      left join organization_metadata on organization_metadata.deleted_at is null
                                     and organization_metadata.organization_guid = organizations.guid
     where organizations.deleted_at is null
  """

  def validate(form: OrganizationForm): Seq[ValidationError] = {
    val nameErrors = if (form.name.length < MinNameLength) {
      Seq(s"name must be at least $MinNameLength characters")
    } else {
      OrganizationDao.findAll(Authorization.All, name = Some(form.name), limit = 1).headOption match {
        case None => Seq.empty
        case Some(org: Organization) => Seq("Org with this name already exists")
      }
    }

    val domainErrors = form.domains.getOrElse(Seq.empty).flatMap { domain =>
      if (isDomainValid(domain)) {
        None
      } else {
        Some(s"Domain $domain is not valid. Expected a domain name like gilt.com")
      }
    }

    Validation.errors(nameErrors ++ domainErrors)
  }


  // We just care that the domain does not have a space in it
  private val DomainRx = """^[^\s]+$""".r
  private[db] def isDomainValid(domain: String): Boolean = {
    domain match {
      case DomainRx() => true
      case _ => false
    }
  }

  /**
   * Creates the org and assigns the user as its administrator.
   */
  def createWithAdministrator(user: User, form: OrganizationForm): Organization = {
    DB.withTransaction { implicit c =>
      val org = create(c, user, form)
      Membership.create(c, user, org, user, Role.Admin)
      OrganizationLog.create(c, user, org, s"Created organization and joined as ${Role.Admin.name}")
      org
    }
  }

  private[db] def emailDomain(email: String): Option[String] = {
    val parts = email.split("@")
    if (parts.length == 2) {
      Some(parts(1).toLowerCase)
    } else {
      None
    }
  }

  private[db] def findByEmailDomain(email: String): Option[Organization] = {
    emailDomain(email).flatMap { domain =>
      OrganizationDomainDao.findAll(domain = Some(domain)).headOption.flatMap { domain =>
        findByGuid(Authorization.All, domain.organization_guid)
      }
    }
  }

  private[db] def reverseDomain(name: String): String = {
    name.split("\\.").reverse.mkString(".")
  }

  private def create(implicit c: java.sql.Connection, createdBy: User, form: OrganizationForm): Organization = {
    require(form.name.length >= MinNameLength, "Name too short")

    val defaultPackageName = form.domains.getOrElse(Seq.empty).headOption.map(reverseDomain(_))

    val initialMetadataForm = form.metadata.getOrElse(OrganizationMetadataForm.Empty)
    val metadataForm = initialMetadataForm.package_name match {
      case None => initialMetadataForm.copy(package_name = defaultPackageName)
      case Some(_) => initialMetadataForm
    }

    val org = Organization(
      guid = UUID.randomUUID,
      key = UrlKey.generate(form.name),
      name = form.name,
      domains = form.domains.getOrElse(Seq.empty).map(Domain(_)),
      metadata = Some(
        OrganizationMetadata(
          packageName = metadataForm.package_name
        )
      )
    )

    SQL("""
      insert into organizations
      (guid, name, key, created_by_guid)
      values
      ({guid}::uuid, {name}, {key}, {created_by_guid}::uuid)
    """).on(
      'guid -> org.guid,
      'name -> org.name,
      'key -> org.key,
      'created_by_guid -> createdBy.guid
    ).execute()

    org.domains.foreach { domain =>
      OrganizationDomainDao.create(c, createdBy, org, domain.name)
    }

    if (metadataForm != OrganizationMetadataForm.Empty) {
      OrganizationMetadataDao.create(c, createdBy, org, metadataForm)
    }

    org
  }

  def softDelete(deletedBy: User, org: Organization) {
    SoftDelete.delete("organizations", deletedBy, org.guid)
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[Organization] = {
    findAll(authorization, guid = Some(guid), limit = 1).headOption
  }

  def findByUserAndGuid(user: User, guid: UUID): Option[Organization] = {
    findAll(Authorization.User(user.guid), guid = Some(guid), limit = 1).headOption
  }

  def findByUserAndKey(user: User, orgKey: String): Option[Organization] = {
    findAll(Authorization.User(user.guid), key = Some(orgKey), limit = 1).headOption
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    userGuid: Option[UUID] = None,
    key: Option[String] = None,
    name: Option[String] = None,
    limit: Int = 50,
    offset: Int = 0
  ): Seq[Organization] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      authorization match {
        case Authorization.All => None
        case Authorization.PublicOnly => Some(s"and organization_metadata.visibility = '${Visibility.Public.toString}'")
        case Authorization.User(userGuid) => {
          Some(
            s"and (organization_metadata.visibility = '${Visibility.Public.toString}'" +
            "      or organizations.guid in (" +
            "select organization_guid from memberships where deleted_at is null and user_guid = {authorization_user_guid}::uuid" +
            "))"
          )
        }
      },
      userGuid.map { v =>
        "and organizations.guid in (" +
        "select organization_guid from memberships where deleted_at is null and user_guid = {user_guid}::uuid" +
        ")"
      },
      guid.map { v => "and organizations.guid = {guid}::uuid" },
      key.map { v => "and organizations.key = lower(trim({key}))" },
      name.map { v => "and lower(organizations.name) = lower(trim({name}))" },
      Some(s"order by lower(organizations.name) limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val authorizationUserGuid = authorization match {
      case Authorization.User(guid) => Some(guid)
      case _ => None
    }

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      authorizationUserGuid.map('authorization_user_guid -> _.toString),
      userGuid.map('user_guid -> _.toString),
      key.map('key -> _),
      name.map('name ->_)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        Organization(
          guid = row[UUID]("guid"),
          name = row[String]("name"),
          key = row[String]("key"),
          domains = row[Option[String]]("domains").fold(Seq.empty[String])(_.split(" ")).sorted.map(Domain(_)),
          metadata = Some(
            OrganizationMetadata(
              packageName = row[Option[String]]("metadata_package_name")
            )
          )
        )
      }.toSeq
    }
  }

}
