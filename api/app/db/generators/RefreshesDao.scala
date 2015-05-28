package db.generators

import db.SoftDelete
import com.gilt.apidoc.api.v0.models.{ReferenceGuid, User}
import org.joda.time.DateTime
import anorm._
import play.api.db._
import play.api.Play.current
import java.util.UUID

object RefreshesDao {

  private[this] val BaseQuery = s"""
    select refreshes.guid,
           refreshes.service_guid,
           refreshes.checked_at
      from generators.refreshes
     where true
  """

  private[this] val InsertQuery = """
    insert into generators.refreshes
    (guid, service_guid, checked_at, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {service_guid}::uuid, {checked_at}, {created_by_guid}::uuid, {updated_by_guid}::uuid)
  """

  private[this] val UpdateQuery = """
    update generators.refreshes
       set checked_at = {checked_at},
           updated_by_guid = {updated_by_guid}::uuid
     where guid = {guid}::uuid
  """

  def upsert(user: User, service: Service) {
    findAll(serviceGuid = Some(service.guid), limit = 1).headOption match {
      case None => create(user, service)
      case Some(refresh) => update(user, refresh)
    }
  }

  private def create(user: User, service: Service): UUID = {
    val guid = UUID.randomUUID

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'service_guid -> service.guid,
        'checked_at -> DateTime.now,
        'created_by_guid -> user.guid,
        'updated_by_guid -> user.guid
      ).execute()
    }

    guid
  }

  private def update(user: User, refresh: Refresh) = {
    DB.withConnection { implicit c =>
      SQL(UpdateQuery).on(
        'guid -> refresh.guid,
        'checked_at -> DateTime.now,
        'updated_by_guid -> user.guid
      ).execute()
    }
  }

  def softDelete(deletedBy: User, refresh: Refresh) {
    SoftDelete.delete("generators.refreshes", deletedBy, refresh.guid)
  }

  def findAll(
    guid: Option[UUID] = None,
    serviceGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Refresh] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and refreshes.guid = {guid}::uuid" },
      serviceGuid.map { v => "and refreshes.service_guid = {service_guid}::uuid" },
      isDeleted.map(db.Filters.isDeleted("refreshes", _))
    ).flatten.mkString("\n   ") + s" order by refreshes.checked_at desc limit ${limit} offset ${offset}"

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      serviceGuid.map('service_guid -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ) = Refresh(
    guid = row[UUID]("guid"),
    service = ReferenceGuid(guid = row[UUID]("service_guid")),
    checkedAt = row[DateTime]("checked_at")
  )

}
