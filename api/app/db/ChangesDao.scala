package db

import anorm.JodaParameterMetaData._
import anorm._
import io.apibuilder.api.v0.models._
import io.apibuilder.common.v0.models.{Audit, Reference, ReferenceGuid}
import io.flow.postgresql.Query
import lib.VersionTag
import org.postgresql.util.PSQLException
import play.api.db._

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.util.{Failure, Success, Try}

@Singleton
class ChangesDao @Inject() (
  @NamedDatabase("default") db: Database
) {

  private val BaseQuery = Query(
    s"""
    select changes.guid,
           changes.type,
           changes.description,
           changes.is_material,
           changes.changed_at,
           changes.changed_by_guid::uuid,
           ${AuditsDao.queryCreationDefaultingUpdatedAt("changes")},
           applications.guid as application_guid,
           applications.key as application_key,
           organizations.guid as organization_guid,
           organizations.key as organization_key,
           from_version.guid::text as from_version_guid,
           from_version.version as from_version_version,
           to_version.guid::text as to_version_guid,
           to_version.version as to_version_version,
           users.nickname as changed_by_nickname
      from changes
      join applications on applications.guid = changes.application_guid and applications.deleted_at is null
      join organizations on organizations.guid = applications.organization_guid and organizations.deleted_at is null
      join versions from_version on from_version.guid = changes.from_version_guid
      join versions to_version on to_version.guid = changes.to_version_guid
      join users on users.guid = changes.changed_by_guid
  """)

  private val InsertQuery =
    """
    insert into changes
    (guid, application_guid, from_version_guid, to_version_guid, type, description, is_material, changed_at, changed_by_guid, created_by_guid)
    values
    ({guid}::uuid, {application_guid}::uuid, {from_version_guid}::uuid, {to_version_guid}::uuid, {type}, {description}, {is_material}::boolean, {changed_at}, {changed_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def upsert(
    createdBy: User,
    fromVersion: Version,
    toVersion: Version,
    differences: Seq[Diff]
  ): Unit = {
    assert(
      fromVersion.guid != toVersion.guid,
      "Versions must be different"
    )

    assert(
      fromVersion.application.guid == toVersion.application.guid,
      "Versions must belong to same application"
    )

    db.withTransaction { implicit c =>
      differences.map {
        case d: DiffBreaking => ("breaking", d)
        case d: DiffNonBreaking => ("non_breaking", d)
        case DiffUndefinedType(desc) => sys.error(s"Unrecognized difference type: $desc")
      }.distinct.foreach {
        case (differenceType, diff) => {
          Try(
            SQL(InsertQuery).on(
              "guid" -> UUID.randomUUID,
              "application_guid" -> fromVersion.application.guid,
              "from_version_guid" -> fromVersion.guid,
              "to_version_guid" -> toVersion.guid,
              "type" -> differenceType,
              "description" -> diff.description,
              "is_material" -> diff.isMaterial,
              "changed_at" -> toVersion.audit.createdAt,
              "changed_by_guid" -> toVersion.audit.createdBy.guid,
              "created_by_guid" -> createdBy.guid
            ).execute()
          ) match {
            case Success(_) => {}
            case Failure(e) => e match {
              case e: PSQLException => {
                findAll(
                  Authorization.All,
                  fromVersionGuid = Some(fromVersion.guid),
                  toVersionGuid = Some(toVersion.guid),
                  description = Some(diff.description)
                ).headOption.getOrElse {
                  sys.error("Failed to create change: " + e)
                }
              }
            }
          }
        }
      }
    }
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[Change] = {
    findAll(authorization, guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    organizationGuid: Option[UUID] = None,
    organizationKey: Option[String] = None,
    applicationKey: Option[String] = None,
    applicationGuid: Option[UUID] = None,
    fromVersionGuid: Option[UUID] = None,
    toVersionGuid: Option[UUID] = None,
    fromVersion: Option[String] = None,
    toVersion: Option[String] = None,
    `type`: Option[String] = None,
    description: Option[String] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Change] = {
    db.withConnection { implicit c =>
      authorization.applicationFilter(BaseQuery).
        equals("changes.guid", guid).
        equals("organizations.guid", organizationGuid).
        equals("organizations.key", organizationKey).
        equals("applications.guid", applicationGuid).
        equals("applications.key", applicationKey).
        equals("changes.from_version_guid", fromVersionGuid).
        equals("changes.to_version_guid", toVersionGuid).
        greaterThanOrEquals("from_version.version_sort_key", fromVersion.map(VersionTag(_).sortKey)).
        lessThanOrEquals("to_version.version_sort_key", toVersion.map(VersionTag(_).sortKey)).
        equals("changes.type", `type`).
        and(
          description.map { _ =>
            "lower(changes.description) = lower(trim({description}))"
          }
        ).bind("description", description).
        orderBy("changes.changed_at desc, lower(organizations.key), lower(applications.key), changes.type, lower(changes.description)").
        limit(limit).
        offset(offset).
        as(parser.*)
    }
  }


  private val parser: RowParser[Change] = {
    import org.joda.time.DateTime

    SqlParser.get[UUID]("guid") ~
      SqlParser.get[UUID]("organization_guid") ~
      SqlParser.str("organization_key") ~
      SqlParser.get[UUID]("application_guid") ~
      SqlParser.str("application_key") ~
      SqlParser.get[UUID]("from_version_guid") ~
      SqlParser.str("from_version_version") ~
      SqlParser.get[UUID]("to_version_guid") ~
      SqlParser.str("to_version_version") ~
      SqlParser.str("type") ~
      SqlParser.str("description") ~
      SqlParser.bool("is_material") ~
      SqlParser.get[DateTime]("changed_at") ~
      SqlParser.get[UUID]("changed_by_guid") ~
      SqlParser.str("changed_by_nickname") ~
      SqlParser.get[DateTime]("created_at") ~
      SqlParser.get[UUID]("created_by_guid") ~
      SqlParser.get[DateTime]("updated_at") ~
      SqlParser.get[UUID]("updated_by_guid") map {
      case guid ~ organizationGuid ~ organizationKey ~ applicationGuid ~ applicationKey ~ fromVersionGuid ~ fromVersionVersion ~ toVersionGuid ~ toVersionVersion ~ typ ~ description ~ isMaterial ~ changedAt ~ changedByGuid ~ changedByNickname ~ createdAt ~ createdByGuid ~ updatedAt ~ updatedByGuid => {
        Change(
          guid = guid,
          organization = Reference(guid = organizationGuid, key = organizationKey),
          application = Reference(guid = applicationGuid, key = applicationKey),
          fromVersion = ChangeVersion(guid = fromVersionGuid, version = fromVersionVersion),
          toVersion = ChangeVersion(guid = toVersionGuid, version = toVersionVersion),
          diff = DiffType.apply(typ) match {
            case DiffType.DiffBreaking => DiffBreaking(description = description, isMaterial = isMaterial)
            case DiffType.DiffNonBreaking => DiffNonBreaking(description = description, isMaterial = isMaterial)
            case DiffType.UNDEFINED(other) => sys.error(s"Invalid diff type '$other'")
          },
          changedAt = changedAt,
          changedBy = UserSummary(guid = changedByGuid, nickname = changedByNickname),
          audit = Audit(
            createdAt = createdAt,
            createdBy = ReferenceGuid(createdByGuid),
            updatedAt = updatedAt,
            updatedBy = ReferenceGuid(updatedByGuid),
          )
        )
      }
    }
  }
}