package db.generators

//import com.gilt.apidoc.api.v0.models.{Service, ServiceForm}
import db.SoftDelete
import com.gilt.apidoc.api.v0.models.{Error, User}
import core.Util
import lib.Validation
import anorm._
import play.api.db._
import play.api.Play.current
import java.util.UUID

object ServicesDao {

  private[this] val BaseQuery = s"""
    select services.guid,
           services.uri
      from generators.services
     where true
  """

  private[this] val InsertQuery = """
    insert into generators.services
    (guid, uri, created_by_guid)
    values
    ({guid}::uuid, {uri}, {created_by_guid}::uuid)
  """

  def validate(
    form: ServiceForm
  ): Seq[Error] = {
    val uriErrors = Util.validateUri(form.uri.trim) match {
      case Nil => {
        ServicesDao.findAll(uri = Some(form.uri.trim)).headOption match {
          case None => Nil
          case Some(uri) => {
            Seq(s"URI[${form.uri.trim}] already exists")
          }
        }
      }
      case errors => errors
    }

    Validation.errors(uriErrors)
  }

  def create(user: User, form: ServiceForm): Service = {
    val errors = validate(form)
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    val guid = UUID.randomUUID

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'uri -> form.uri.trim,
        'created_by_guid -> user.guid
      ).execute()
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create service")
    }
  }

  def softDelete(deletedBy: User, service: Service) {
    SoftDelete.delete("generators.services", deletedBy, service.guid)
  }

  def findByGuid(guid: UUID): Option[Service] = {
    findAll(guid = Some(guid)).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    uri: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Service] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and services.guid = {guid}::uuid" },
      uri.map { v => "and lower(services.uri) = lower(trim({uri}))" },
      isDeleted.map(db.Filters.isDeleted("services", _))
    ).flatten.mkString("\n   ") + s" order by lower(services.uri) limit ${limit} offset ${offset}"

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      uri.map('uri ->_)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ) = Service(
    guid = row[UUID]("guid"),
    uri = row[String]("uri")
  )

}
