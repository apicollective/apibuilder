package db.generators

//import com.gilt.apidoc.api.v0.models.{Source, SourceForm}
import db.SoftDelete
import com.gilt.apidoc.api.v0.models.{Error, User}
import core.Util
import lib.Validation
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

case class Source(
  guid: UUID,
  uri: String
)

case class SourceForm(
  uri: String
)

object SourcesDao {

  private[this] val BaseQuery = s"""
    select sources.guid,
           sources.uri
      from generators.sources
     where true
  """

  private[this] val InsertQuery = """
    insert into generators.sources
    (guid, uri, created_by_guid)
    values
    ({guid}::uuid, {uri}, {created_by_guid}::uuid)
  """

  def validate(
    form: SourceForm
  ): Seq[Error] = {
    val uriErrors = Util.validateUri(form.uri) match {
      case Nil => {
        SourcesDao.findAll(uri = Some(form.uri)).headOption match {
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

  def create(user: User, form: SourceForm): Source = {
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
      sys.error("Failed to create source")
    }
  }

  def softDelete(deletedBy: User, source: Source) {
    SoftDelete.delete("generators.sources", deletedBy, source.guid)
  }

  def findByGuid(guid: UUID): Option[Source] = {
    findAll(guid = Some(guid)).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    uri: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Source] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and sources.guid = {guid}::uuid" },
      uri.map { v => "and lower(sources.uri) = lower(trim({uri}))" },
      isDeleted.map(db.Filters.isDeleted("sources", _))
    ).flatten.mkString("\n   ") + s" order by lower(sources.uri) limit ${limit} offset ${offset}"

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
  ) = Source(
    guid = row[UUID]("guid"),
    uri = row[String]("uri")
  )

}
