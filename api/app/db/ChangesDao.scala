package db

import io.apibuilder.api.v0.models.{Application, Change, ChangeVersion, User, UserSummary, Version}
import io.apibuilder.api.v0.models.{Diff, DiffBreaking, DiffNonBreaking, DiffUndefinedType}
import io.apibuilder.common.v0.models.{Reference, ReferenceGuid}
import io.apibuilder.internal.v0.models.json._
import anorm._
import anorm.JodaParameterMetaData._
import javax.inject.{Inject, Named, Singleton}
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID
import lib.VersionTag
import org.postgresql.util.PSQLException
import scala.util.{Try, Success, Failure}
import org.joda.time.DateTime

@Singleton
class ChangesDao @Inject() () {

  private[this] val BaseQuery = s"""
    select changes.guid,
           changes.type,
           changes.description,
           changes.changed_at,
           changes.changed_by_guid::uuid,
           ${AuditsDao.queryCreation("changes")},
           applications.guid as application_guid,
           applications.key as application_key,
           organizations.guid as organization_guid,
           organizations.key as organization_key,
           from_version.guid::text as from_guid,
           from_version.version as from_version,
           to_version.guid::text as to_guid,
           to_version.version as to_version,
           users.guid as user_guid,
           users.nickname as user_nickname
      from changes
      join applications on applications.guid = changes.application_guid and applications.deleted_at is null
      join organizations on organizations.guid = applications.organization_guid and organizations.deleted_at is null
      join versions from_version on from_version.guid = changes.from_version_guid
      join versions to_version on to_version.guid = changes.to_version_guid
      join users on users.guid = changes.changed_by_guid
     where true
  """

  private[this] val InsertQuery = """
    insert into changes
    (guid, application_guid, from_version_guid, to_version_guid, type, description, changed_at, changed_by_guid, created_by_guid)
    values
    ({guid}::uuid, {application_guid}::uuid, {from_version_guid}::uuid, {to_version_guid}::uuid, {type}, {description}, {changed_at}, {changed_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def upsert(
    createdBy: User,
    fromVersion: Version,
    toVersion: Version,
    differences: Seq[Diff]
  ) {
    assert(
      fromVersion.guid != toVersion.guid,
      "Versions must be different"
    )

    assert(
      fromVersion.application.guid == toVersion.application.guid,
      "Versions must belong to same application"
    )

    DB.withTransaction { implicit c =>

      differences.map { d =>
        d match {
          case DiffBreaking(desc) => ("breaking", desc)
          case DiffNonBreaking(desc) => ("non_breaking", desc)
          case DiffUndefinedType(desc) => {
            sys.error(s"Unrecognized difference type: $desc")
          }
        }
      }.distinct.foreach {
        case (differenceType, description) => {
          Try(
            SQL(InsertQuery).on(
              'guid -> UUID.randomUUID,
              'application_guid -> fromVersion.application.guid,
              'from_version_guid -> fromVersion.guid,
              'to_version_guid -> toVersion.guid,
              'type -> differenceType,
              'description -> description,
              'changed_at -> toVersion.audit.createdAt,
              'changed_by_guid -> toVersion.audit.createdBy.guid,
              'created_by_guid -> createdBy.guid
            ).execute()
          ) match {
            case Success(_) => {}
            case Failure(e) => e match {
              case e: PSQLException => {
                findAll(
                  Authorization.All,
                  fromVersionGuid = Some(fromVersion.guid),
                  toVersionGuid = Some(toVersion.guid),
                  description = Some(description)
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
    val sql = Seq(
      Some(BaseQuery.trim),
      authorization.applicationFilter().map(v => "and " + v),
      guid.map { v => "and changes.guid = {guid}::uuid" },
      organizationGuid.map { v => "and organizations.guid = {organization_guid}::uuid" },
      organizationKey.map { v => "and organizations.key = {organization_key}" },
      applicationGuid.map { v => "and applications.guid = {application_guid}::uuid" },
      applicationKey.map { v => "and applications.key = lower(trim({application_key}))" },
      fromVersionGuid.map { v => "and changes.from_version_guid = {from_version_guid}::uuid" },
      toVersionGuid.map { v => "and changes.to_version_guid = {to_version_guid}::uuid" },
      fromVersion.map { v => "and from_version.version_sort_key >= {from_version_sort_key}" },
      toVersion.map { v => "and to_version.version_sort_key <= {to_version_sort_key}" },
      `type`.map { v => "and changes.type <= {type}" },
      description.map { v => "and lower(changes.description) = lower(trim({description}))" },
      Some(s"order by changes.changed_at desc, lower(organizations.key), lower(applications.key), changes.type, lower(changes.description) limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      organizationGuid.map('organization_guid -> _.toString),
      organizationKey.map('organization_key -> _),
      applicationGuid.map('application_guid -> _.toString),
      applicationKey.map('application_key -> _),
      fromVersionGuid.map('from_version_guid -> _.toString),
      toVersionGuid.map('to_version_guid -> _.toString),
      fromVersion.map(v => 'from_version_sort_key -> VersionTag(v).sortKey),
      toVersion.map(v => 'to_version_sort_key -> VersionTag(v).sortKey),
      `type`.map('type -> _),
      description.map('description -> _)
    ).flatten ++ authorization.bindVariables

    DB.withConnection { implicit c =>
      sys.error("TODO PARSER") // SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ): Change = {
    Change(
      guid = row[UUID]("guid"),
      application = Reference(row[UUID]("application_guid"), row[String]("application_key")),
      organization = Reference(row[UUID]("organization_guid"), row[String]("organization_key")),
      fromVersion = ChangeVersion(row[UUID]("from_guid"), row[String]("from_version")),
      toVersion = ChangeVersion(row[UUID]("to_guid"), row[String]("to_version")),
      diff = row[String]("type") match {
        case "breaking" => DiffBreaking(row[String]("description"))
        case "non_breaking" => DiffNonBreaking(row[String]("description"))
        case other => DiffUndefinedType(other)
      },
      changedAt = row[DateTime]("changed_at"),
      changedBy = UserSummary(
        guid = row[UUID]("user_guid"),
        nickname = row[String]("user_nickname")
      ),
      audit = AuditsDao.fromRowCreation(row)
    )
  }

}
