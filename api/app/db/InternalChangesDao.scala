package db

import anorm.JodaParameterMetaData.*
import anorm.*
import db.generated.{ChangeForm, ChangesDao}
import io.apibuilder.api.v0.models.*
import io.flow.postgresql.{OrderBy, Query}
import lib.VersionTag
import org.postgresql.util.PSQLException
import util.OptionalQueryFilter

import java.util.UUID
import javax.inject.Inject
import scala.util.{Failure, Success, Try}

case class InternalChange(db: generated.Change) {
  val guid: UUID = db.guid
  val diff: Diff = db.`type` match {
    case "breaking" => DiffBreaking(description = db.description, isMaterial = db.isMaterial)
    case "non_breaking" => DiffNonBreaking(description = db.description, isMaterial = db.isMaterial)
    case other => sys.error(s"Invalid diff type '$other'")
  }
}

class InternalChangesDao @Inject()(
  dao: ChangesDao
) {
  private object DiffType {
    val Breaking = "breaking"
    val NonBreaking = "non_breaking"
  }

  def upsert(
    createdBy: InternalUser,
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

    dao.db.withTransaction { implicit c =>
      differences.map {
        case d: DiffBreaking => (DiffType.Breaking, d)
        case d: DiffNonBreaking => (DiffType.NonBreaking, d)
        case DiffUndefinedType(desc) => sys.error(s"Unrecognized difference type: $desc")
      }.distinct.foreach {
        case (differenceType, diff) => {
          val form = ChangeForm(
            applicationGuid = fromVersion.application.guid,
            fromVersionGuid = fromVersion.guid,
            toVersionGuid = toVersion.guid,
            `type` = differenceType,
            description = diff.description,
            isMaterial = diff.isMaterial,
            changedAt = toVersion.audit.createdAt,
            changedByGuid = toVersion.audit.createdBy.guid,
          )
          Try {
            dao.insert(createdBy.guid, form)
          } match {
            case Success(_) => // no-op
            case Failure(e) => e match {
              case e: PSQLException if exists(form) => // no-op as already exists
              case t: Throwable => throw t
            }
          }
        }
      }
    }
  }

  private def exists(form: ChangeForm): Boolean = {
    findAll(
      Authorization.All,
      fromVersionGuid = Some(form.fromVersionGuid),
      toVersionGuid = Some(form.toVersionGuid),
      description = Some(form.description),
      limit = Some(1),
    ).nonEmpty
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[InternalChange] = {
    findAll(authorization, guid = Some(guid), limit = Some(1)).headOption
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
    isDeleted: Option[Boolean] = None,
    limit: Option[Long],
    offset: Long = 0
  ): Seq[InternalChange] = {
    val filters = List(
      new OptionalQueryFilter(organizationGuid) {
        override def filter(q: Query, orgGuid: UUID): Query = {
          q.in("application_guid", Query("select guid from applications").equals("organization_guid", orgGuid))
        }
      },
      new OptionalQueryFilter(organizationKey) {
        override def filter(q: Query, key: String): Query = {
          q.in("application_guid", Query(
            """
              |select app.guid
              |  from applications app
              |  join organizations org on org.guid = app.organization_guid
              |""".stripMargin
          ).equals("org.key", key))
        }
      },
      new OptionalQueryFilter(applicationKey) {
        override def filter(q: Query, key: String): Query = {
          q.in("application_guid", Query("select guid from applications").equals("key", key))
        }
      },
      new OptionalQueryFilter(fromVersion) {
        override def filter(q: Query, v: String): Query = {
          q.in(
            "from_version_guid",
            Query("select guid from versions")
              .greaterThanOrEquals("version_sort_key", VersionTag(v).sortKey)
          )
        }
      },
      new OptionalQueryFilter(toVersion) {
        override def filter(q: Query, v: String): Query = {
          q.in(
            "from_version_guid",
            Query("select guid from versions")
              .lessThanOrEquals("version_sort_key", VersionTag(v).sortKey)
          )
        }
      },
    )

    dao.findAll(
      guid = guid,
      applicationGuid = applicationGuid,
      toVersionGuid = toVersionGuid,
      limit = limit,
      offset = offset,
      orderBy = Some(OrderBy("-changed_at, type, -description")),
    ) { q =>
      authorization.applicationFilter(
          filters.foldLeft(q) { case (q, f) => f.filter(q) },
          "application_guid"
        )
        .equals("from_version_guid", fromVersionGuid)
        .equals("type", `type`)
        .and(isDeleted.map(Filters.isDeleted("changes", _)))
        .and(
          description.map { _ =>
            "lower(description) = lower(trim({description}))"
          }
        ).bind("description", description)
    }.map(InternalChange(_))
  }
}