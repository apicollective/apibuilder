package db

import anorm._
import io.apibuilder.api.v0.models.{Error, User, WatchForm}
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}
import io.flow.postgresql.Query
import lib.Validation
import play.api.db._
import util.OptionalQueryFilter

import java.util.UUID
import javax.inject.{Inject, Singleton}

case class ValidatedWatchForm(
  application: InternalApplication,
  form: WatchForm
)

case class InternalWatch(
                               guid: UUID,
                               audit: Audit,
                               applicationGuid: UUID,
                               userGuid: UUID
                             )
@Singleton
class WatchesDao @Inject() (
  @NamedDatabase("default") db: Database,
  applicationsDao: ApplicationsDao,
  usersDao: UsersDao
) {

  private val dbHelpers = DbHelpers(db, "watches")

  private val BaseQuery = Query(s"""
    select guid, user_guid, application_guid,
           ${AuditsDao.queryCreationDefaultingUpdatedAt("watches")}
      from watches
  """)

  private val InsertQuery = """
    insert into watches
    (guid, user_guid, application_guid, created_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {application_guid}::uuid, {created_by_guid}::uuid)
  """

  def validate(
    auth: Authorization,
    form: WatchForm
  ): Either[Seq[Error], ValidatedWatchForm] = {
    val userErrors = usersDao.findByGuid(form.userGuid) match {
      case None => Seq("User not found")
      case Some(_) => Nil
    }

    val application = applicationsDao.findByOrganizationKeyAndApplicationKey(auth, form.organizationKey, form.applicationKey)
    val applicationKeyErrors = application match {
      case None => Seq(s"Application[${form.applicationKey}] not found")
      case Some(_) => Nil
    }

    (applicationKeyErrors ++ userErrors).toList match {
      case Nil => {
        Right(
          ValidatedWatchForm(
            application = application.get,
            form = form
          )
        )
      }
      case errors => {
        Left(Validation.errors(errors))
      }
    }
  }

  private def findByApplicationGuidAndUserGuid(applicationGuid: UUID, userGuid: UUID) = {
    findAll(
      Authorization.All,
      userGuid = Some(userGuid),
      applicationGuid = Some(applicationGuid),
      limit = Some(1)
    ).headOption
  }

  def upsert(createdBy: User, form: ValidatedWatchForm): InternalWatch = {
    def find: Option[InternalWatch] = findByApplicationGuidAndUserGuid(
      applicationGuid = form.application.guid,
      userGuid = form.form.userGuid
    )

    find.getOrElse {
      db.withConnection { implicit c =>
        SQL(InsertQuery).on(
          "guid" -> UUID.randomUUID(),
          "user_guid" -> form.form.userGuid,
          "application_guid" -> form.application.guid,
          "created_by_guid" -> createdBy.guid
        ).execute()
      }
      find.getOrElse(
        sys.error("Failed to create watch")
      )
    }
  }

  def softDelete(deletedBy: User, watch: InternalWatch): Unit =  {
    dbHelpers.delete(deletedBy, watch.guid)
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[InternalWatch] = {
    findAll(authorization, guid = Some(guid), limit = Some(1)).headOption
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    organizationKey: Option[String] = None,
    applicationGuid: Option[UUID] = None,
    applicationKey: Option[String] = None,
    userGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Option[Long],
    offset: Long = 0
  ): Seq[InternalWatch] = {
    val filters = List(
      new OptionalQueryFilter(organizationKey) {
        override def filter(q: Query, value: String): Query = {
          q.in("application_guid", Query(
            """
              |select a.guid
              |  from applications a
              |  join organizations o on o.guid = a.organization_guid
              |""".stripMargin
          ).equals("o.key", value))
        }
      },
      new OptionalQueryFilter(applicationKey) {
        override def filter(q: Query, value: String): Query = {
          q.in("application_guid", Query("select guid from applications").equals("key", value))
        }
      }
    )

    db.withConnection { implicit c =>
      authorization.applicationFilter(
          filters.foldLeft(BaseQuery) { case (q, f) => f.filter(q) },
          "application_guid"
        ).
        equals("guid", guid).
        equals("application_guid", applicationGuid).
        equals("user_guid", userGuid).
        and(isDeleted.map(Filters.isDeleted("watches", _))).
        orderBy("created_at").
        optionalLimit(limit).
        offset(offset).
        as(parser.*)
    }
  }

  private val parser: RowParser[InternalWatch] = {
    import org.joda.time.DateTime

    SqlParser.get[UUID]("guid") ~
      SqlParser.get[DateTime]("created_at") ~
      SqlParser.get[UUID]("created_by_guid") ~
      SqlParser.get[DateTime]("updated_at") ~
      SqlParser.get[UUID]("updated_by_guid") ~
      SqlParser.get[UUID]("application_guid") ~
      SqlParser.get[UUID]("user_guid") map {
      case guid ~ createdAt ~ createdByGuid ~ updatedAt ~ updatedByGuid ~ applicationGuid ~ userGuid => {
        InternalWatch(
          guid = guid,
          applicationGuid = applicationGuid,
          userGuid = userGuid,
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
