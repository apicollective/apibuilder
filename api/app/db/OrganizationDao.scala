package db


import core.{ Role, UrlKey }
import anorm._
import lib.{ Validation, ValidationError }
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object Organization {
  implicit val organizationWrites = Json.writes[Organization]
}

case class Organization(guid: String, name: String, key: String) {

  // temporary as we roll out authentication
  val emailDomains: Set[String] = {
    if (key == OrganizationDao.GiltKey) {
      OrganizationDao.GiltComDomains
    } else {
      Set.empty
    }
  }

}


object OrganizationDao {

  val MinNameLength = 4

  private[db] val GiltComDomains = Set("gilt.com", "giltcity.com")
  private[db] val GiltKey = "gilt"

  private val BaseQuery = """
    select guid::varchar, name, key
      from organizations
     where deleted_at is null
  """

  def validate(name: String): Seq[ValidationError] = {
    if (name.length < MinNameLength) {
      Validation.error(s"name must be at least $MinNameLength characters")
    } else {
      OrganizationDao.findAll(name = Some(name), limit = 1).headOption match {
        case None => Seq.empty
        case Some(org: Organization) => Validation.error("Org with this name already exists")
      }
    }
  }

  /**
   * Creates the org and assigns the user as its administrator.
   */
  def createWithAdministrator(user: User, name: String): Organization = {
    DB.withTransaction { implicit c =>
      val org = create(user, name)
      Membership.upsert(user, org, user, Role.Admin)
      OrganizationLog.create(user, org, s"Created organization and joined as ${Role.Admin.name}")
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
      if (GiltComDomains.contains(domain.toLowerCase)) {
        findAll(key = Some(GiltKey), limit = 1).headOption
      } else {
        None
      }
    }
  }

  private def create(createdBy: User, name: String): Organization = {
    require(name.length >= MinNameLength, "Name too short")

    val org = Organization(guid = UUID.randomUUID.toString,
                           key = UrlKey.generate(name),
                           name = name)

    DB.withConnection { implicit c =>
      SQL("""
          insert into organizations
          (guid, name, key, created_by_guid)
          values
          ({guid}::uuid, {name}, {key}, {created_by_guid}::uuid)
          """).on('guid -> org.guid,
                  'name -> org.name,
                  'key -> org.key,
                  'created_by_guid -> createdBy.guid).execute()
    }

    org
  }

  def softDelete(deletedBy: User, org: Organization) {
    SoftDelete.delete("organizations", deletedBy, org.guid)
  }

  def findByGuid(guid: UUID): Option[Organization] = {
    findByGuid(guid.toString)
  }

  def findByGuid(guid: String): Option[Organization] = {
    findAll(guid = Some(guid)).headOption
  }

  def findByUserAndKey(user: User, orgKey: String): Option[Organization] = {
    OrganizationDao.findAll(userGuid = Some(user.guid), key = Some(orgKey), limit = 1).headOption
  }

  def findAll(guid: Option[String] = None,
              userGuid: Option[String] = None,
              key: Option[String] = None,
              name: Option[String] = None,
              limit: Int = 50,
              offset: Int = 0): Seq[Organization] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      userGuid.map { v => "and guid in (select organization_guid from memberships where deleted_at is null and user_guid = {user_guid}::uuid)" },
      guid.map { v => "and guid = {guid}::uuid" },
      key.map { v => "and key = lower(trim({key}))" },
      name.map { v => "and lower(name) = lower(trim({name}))" },
      Some(s"order by lower(name) limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _),
      userGuid.map('user_guid -> _),
      key.map('key -> _),
      name.map('name ->_)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        Organization(guid = row[String]("guid"),
                     name = row[String]("name"),
                     key = row[String]("key"))
      }.toSeq
    }
  }

}
