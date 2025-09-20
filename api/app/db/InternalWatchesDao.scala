package db

import cats.data.ValidatedNec
import cats.implicits.*
import db.generated.WatchesDao
import io.apibuilder.api.v0.models.{Error, WatchForm}
import io.flow.postgresql.Query
import lib.Validation
import util.OptionalQueryFilter

import java.util.UUID
import javax.inject.Inject

case class ValidatedWatchForm(
  user: InternalUser,
  application: InternalApplication,
)

case class InternalWatch(db: generated. Watch) {
  val guid: UUID = db.guid
  val applicationGuid: UUID = db.applicationGuid
  val userGuid: UUID = db.userGuid
}

class InternalWatchesDao @Inject()(
  dao: WatchesDao,
  applicationsDao: InternalApplicationsDao,
  usersDao: InternalUsersDao
) {

  private def validate(
    auth: Authorization,
    form: WatchForm
  ): ValidatedNec[Error, ValidatedWatchForm] = {
    (
      usersDao.findByGuid(form.userGuid).toValidNec(Validation.singleError("User not found")),
      applicationsDao.findByOrganizationKeyAndApplicationKey(auth, form.organizationKey, form.applicationKey).toValidNec(Validation.singleError(s"Application[${form.applicationKey}] not found"))
    ).mapN(ValidatedWatchForm(_, _))
  }

  private def findByApplicationGuidAndUserGuid(applicationGuid: UUID, userGuid: UUID): Option[InternalWatch] = {
    findAll(
      Authorization.All,
      userGuid = Some(userGuid),
      applicationGuid = Some(applicationGuid),
      limit = Some(1)
    ).headOption
  }

  def upsert(auth: Authorization, createdBy: InternalUser, form: WatchForm): ValidatedNec[Error, InternalWatch] = {
    validate(auth, form).map { vForm =>
      def find: Option[InternalWatch] = findByApplicationGuidAndUserGuid(
        applicationGuid = vForm.application.guid,
        userGuid = vForm.user.guid,
      )

      find.getOrElse {
        dao.insert(createdBy.guid, generated.WatchForm(
          applicationGuid = vForm.application.guid,
          userGuid = vForm.user.guid,
        ))

        find.getOrElse(
          sys.error("Failed to create watch")
        )
      }
    }
  }

  def softDelete(deletedBy: InternalUser, watch: InternalWatch): Unit =  {
    dao.delete(deletedBy.guid, watch.db)
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[InternalWatch] = {
    findAll(authorization, guid = Some(guid), limit = Some(1)).headOption
  }

  def findAll(
    authorization: Authorization,
    guid:  Option[UUID] = None,
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

    dao.findAll(
      guid = guid,
      userGuid = userGuid,
      applicationGuid = applicationGuid,
      limit = limit,
      offset = offset,
    )( using (q: Query) => {
      authorization.applicationFilter(
        filters.foldLeft(q) { case (q, f) => f.filter(q) },
        "application_guid"
      )
      .and(isDeleted.map(Filters.isDeleted("watches", _)))
    }).map(InternalWatch(_))
  }

}
