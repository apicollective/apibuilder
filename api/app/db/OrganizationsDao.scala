package db

import io.apibuilder.api.v0.models._
import io.apibuilder.api.v0.models.json._
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}
import io.apibuilder.common.v0.models.json._
import lib.{Misc, Role, Validation, UrlKey}
import anorm._
import javax.inject.{Inject, Singleton}
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID
import org.joda.time.DateTime

@Singleton
class OrganizationsDao @Inject() (
  organizationDomainsDao: OrganizationDomainsDao,
  organizationLogsDao: OrganizationLogsDao
) {
  // TODO: resolve cicrular dependency
  private[this] def membershipsDao = play.api.Play.current.injector.instanceOf[MembershipsDao]

  private[this] val DefaultVisibility = Visibility.Organization

  private[this] val MinNameLength = 3

  private[db] val BaseQuery = s"""
    select organizations.guid,
           organizations.name,
           organizations.key,
           organizations.visibility,
           organizations.namespace,
           ${AuditsDao.query("organizations")},
           (select array_to_string(array_agg(domain), ' ') 
              from organization_domains
             where deleted_at is null
               and organization_guid = organizations.guid) as domains
      from organizations
     where true
  """

  private[this] val InsertQuery = """
    insert into organizations
    (guid, name, key, namespace, visibility, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {name}, {key}, {namespace}, {visibility}, {user_guid}::uuid, {user_guid}::uuid)
  """

  private[this] val UpdateQuery = """
    update organizations
       set name = {name},
           key = {key},
           namespace = {namespace},
           visibility = {visibility},
           updated_by_guid = {user_guid}::uuid
     where guid = {guid}::uuid
  """

  def validate(
    form: OrganizationForm,
    existing: Option[Organization] = None
  ): Seq[io.apibuilder.api.v0.models.Error] = {

    val nameErrors = if (form.name.length < MinNameLength) {
      Seq(s"name must be at least $MinNameLength characters")
    } else {
      findAll(Authorization.All, name = Some(form.name), limit = 1).headOption match {
        case None => Seq.empty
        case Some(org: Organization) => {
          if (existing.map(_.guid) == Some(org.guid)) {
            Seq.empty
          } else {
            Seq("Org with this name already exists")
          }
        }
      }
    }

    val keyErrors = form.key match {
      case None => {
        Nil
      }

      case Some(key) => {
        UrlKey.validate(key) match {
          case Nil => {
            findByKey(Authorization.All, key) match {
              case None => Seq.empty
              case Some(found) => {
                if (existing.map(_.guid) == Some(found.guid)) {
                  Seq.empty
                } else {
                  Seq("Org with this key already exists")
                }
              }
            }
          }
          case errors => errors
        }
      }
    }

    val namespaceErrors = findAll(Authorization.All, namespace = Some(form.namespace.trim), limit = 1).headOption match {
      case None => {
        isDomainValid(form.namespace.trim) match {
          case true => Seq.empty
          case false => Seq("Namespace is not valid. Expected a name like io.apibuilder")
        }
      }
      case Some(org: Organization) => {
        Seq.empty
      }
    }

    val visibilityErrors =  Visibility.fromString(form.visibility.toString) match {
      case Some(_) => Seq.empty
      case None => Seq(s"Invalid visibility[${form.visibility.toString}]")
    }

    val domainErrors = form.domains.getOrElse(Nil).filter(!isDomainValid(_)).map(d => s"Domain $d is not valid. Expected a domain name like apicollective.io.me")

    Validation.errors(nameErrors ++ keyErrors ++ namespaceErrors ++ visibilityErrors ++ domainErrors)
  }


  // We just care that the domain does not have a space in it
  private[this] val DomainRx = """^[^\s]+$""".r
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
      membershipsDao.create(c, user, org, user, Role.Admin)
      organizationLogsDao.create(c, user, org, s"Created organization and joined as ${Role.Admin.name}")
      org
    }
  }

  def findByEmailDomain(email: String): Option[Organization] = {
    Misc.emailDomain(email).flatMap { domain =>
      organizationDomainsDao.findAll(domain = Some(domain)).headOption.flatMap { domain =>
        findByGuid(Authorization.All, domain.organization_guid)
      }
    }
  }

  def update(user: User, existing: Organization, form: OrganizationForm): Organization = {
    val errors = validate(form, Some(existing))
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    DB.withConnection { implicit c =>
      SQL(UpdateQuery).on(
        'guid -> existing.guid,
        'name -> form.name.trim,
        'key -> form.key.getOrElse(UrlKey.generate(form.name)).trim,
        'namespace -> form.namespace.trim,
        'visibility -> form.visibility.toString,
        'user_guid -> user.guid
      ).execute()
    }

    // TODO: Figure out how we want to handle domains. Best option
    // might be to remove domains from organization_form

    findByGuid(Authorization.All, existing.guid).getOrElse {
      sys.error("Failed to update org")
    }
  }

  private[this] def create(implicit c: java.sql.Connection, user: User, form: OrganizationForm): Organization = {
    val errors = validate(form)
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    val org = Organization(
      guid = UUID.randomUUID,
      key = form.key.getOrElse(UrlKey.generate(form.name)).trim,
      name = form.name.trim,
      namespace = form.namespace.trim,
      visibility = form.visibility,
      domains = form.domains.getOrElse(Nil).map(Domain(_)),
      audit = Audit(
        createdAt = DateTime.now,
        createdBy = ReferenceGuid(user.guid),
        updatedAt = DateTime.now,
        updatedBy = ReferenceGuid(user.guid)
      )
    )

    SQL(InsertQuery).on(
      'guid -> org.guid,
      'name -> org.name,
      'namespace -> org.namespace,
      'visibility -> org.visibility.toString,
      'key -> org.key,
      'user_guid -> user.guid
    ).execute()

    org.domains.foreach { domain =>
      organizationDomainsDao.create(c, user, org, domain.name)
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
    application: Option[Application] = None,
    key: Option[String] = None,
    name: Option[String] = None,
    namespace: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Organization] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      authorization.organizationFilter().map(v => "and " + v),
      userGuid.map { v =>
        "and organizations.guid in (" +
        "select organization_guid from memberships where deleted_at is null and user_guid = {user_guid}::uuid" +
        ")"
      },
      application.map { v =>
        "and organizations.guid in (" +
        "select organization_guid from applications where deleted_at is null and guid = {application_guid}::uuid" +
        ")"
      },
      guid.map { v => "and organizations.guid = {guid}::uuid" },
      key.map { v => "and organizations.key = lower(trim({key}))" },
      name.map { v => "and lower(trim(organizations.name)) = lower(trim({name}))" },
      namespace.map { v => "and organizations.namespace = lower(trim({namespace}))" },
      isDeleted.map(Filters.isDeleted("organizations", _)),
      Some(s"order by lower(organizations.name) limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      userGuid.map('user_guid -> _.toString),
      application.map('application_guid -> _.guid.toString),
      key.map('key -> _),
      name.map('name ->_),
      namespace.map('namespace ->_)
    ).flatten ++ authorization.bindVariables

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ): Organization = {
    summaryFromRow(row).copy(
      domains = row[Option[String]]("domains").fold(Seq.empty[String])(_.split(" ")).sorted.map(Domain(_))
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
      name = row[String](s"${p}name"),
      namespace = row[String](s"${p}namespace"),
      visibility = Visibility(row[String](s"${p}visibility")),
      audit = AuditsDao.fromRow(row, prefix)
    )
  }

}
