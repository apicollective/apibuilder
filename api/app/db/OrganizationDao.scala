package db

import com.gilt.apidoc.models.{Domain, Error, Organization, OrganizationForm, OrganizationMetadata, OrganizationMetadataForm, User, Version, Visibility}
import com.gilt.apidoc.models.json._
import lib.{Role, Validation, UrlKey}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object OrganizationDao {

  private val MinNameLength = 4
  val MinKeyLength = 4
  val ReservedKeys = Seq(
    "_internal_", "account", "accounts", "admin", "assets", "doc", "docs", "documentation", "generator", "generators",
    "login", "logout", "org", "orgs", "organizations", "private", "session", "subaccount", "subaccounts",
    "subscription", "subscriptions", "team", "teams", "user", "users"
  )

  private val EmptyOrganizationMetadataForm = OrganizationMetadataForm()

  private[db] val BaseQuery = """
    select organizations.guid, organizations.name, organizations.key,
           organization_metadata.visibility as metadata_visibility,
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

  def validate(form: OrganizationForm): Seq[Error] = {
    val nameErrors = if (form.name.length < MinNameLength) {
      Seq(s"name must be at least $MinNameLength characters")
    } else {
      OrganizationDao.findAll(Authorization.All, name = Some(form.name), limit = 1).headOption match {
        case None => Seq.empty
        case Some(org: Organization) => Seq("Org with this name already exists")
      }
    }

    val keyErrors = form.key match {
      case None => {
        nameErrors match {
          case Nil => {
            val generated = UrlKey.generate(form.name)
            if (ReservedKeys.contains(generated)) {
              Seq(s"Key $generated is a reserved word and cannot be used for the name of an organization")
            } else {
              Seq.empty
            }
          }
          case errors => Seq.empty
        }
      }

      case Some(key) => {
        val generated = UrlKey.generate(key)
        if (key.length < MinKeyLength) {
          Seq(s"Key must be at least $MinKeyLength characters")
        } else if (key != generated) {
          Seq(s"Key must be in all lower case and contain alphanumerics only. A valid key would be: $generated")
        } else if (ReservedKeys.contains(generated)) {
          Seq(s"Key $generated is a reserved word and cannot be used for the key of an organization")
        } else {
          OrganizationDao.findByKey(Authorization.All, key) match {
            case None => Seq.empty
            case Some(existing) => Seq("Org with this key already exists")
          }
        }
      }
    }

    val domainErrors = form.domains.filter(!isDomainValid(_)).map(d => s"Domain $d is not valid. Expected a domain name like apidoc.me")

    Validation.errors(nameErrors ++ keyErrors ++ domainErrors)
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

    val defaultPackageName = form.domains.headOption.map(reverseDomain(_))

    val initialMetadataForm = form.metadata.getOrElse(EmptyOrganizationMetadataForm)
    val metadataForm = initialMetadataForm.packageName match {
      case None => initialMetadataForm.copy(packageName = defaultPackageName)
      case Some(_) => initialMetadataForm
    }

    val org = Organization(
      guid = UUID.randomUUID,
      key = form.key.getOrElse(UrlKey.generate(form.name)).trim,
      name = form.name.trim,
      domains = form.domains.map(Domain(_)),
      metadata = Some(
        OrganizationMetadata(
          packageName = metadataForm.packageName.map(_.trim)
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

    if (metadataForm != EmptyOrganizationMetadataForm) {
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
    findByGuid(Authorization.User(user.guid), guid)
  }

  def findByUserAndKey(user: User, orgKey: String): Option[Organization] = {
    findByKey(Authorization.User(user.guid), orgKey)
  }

  def findByKey(authorization: Authorization, orgKey: String): Option[Organization] = {
    findAll(authorization, key = Some(orgKey), limit = 1).headOption
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
        case Authorization.PublicOnly => Some(s"and organization_metadata.visibility = '${Visibility.Public}'")
        case Authorization.User(userGuid) => {
          Some(
            s"and (organization_metadata.visibility = '${Visibility.Public}'" +
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
      name.map { v => "and lower(trim(organizations.name)) = lower(trim({name}))" },
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
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ): Organization = {
    summaryFromRow(row).copy(
      domains = row[Option[String]]("domains").fold(Seq.empty[String])(_.split(" ")).sorted.map(Domain(_)),
      metadata = Some(
        OrganizationMetadata(
          visibility = row[Option[String]]("metadata_visibility").map(Visibility(_)),
          packageName = row[Option[String]]("metadata_package_name")
        )
      )
    )
  }

  private[db] def summaryFromRow(
    row: anorm.Row,
    prefix: Option[String] = None
  ): Organization = {
    val p = prefix.map( _ + "_").getOrElse("")

    Organization(
      guid = row[UUID](s"${p}guid"),
      key = row[String](s"${p}key"),
      name = row[String](s"${p}name")
    )
  }

}
