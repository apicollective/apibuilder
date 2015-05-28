package db.generators

import db.{Authorization, SoftDelete}
import com.gilt.apidoc.api.v0.models.{Error, User, Generator, GeneratorService}
import core.Util
import lib.Validation
import anorm._
import play.api.db._
import play.api.Play.current
import java.util.UUID

object GeneratorsDao {

  private[this] val BaseQuery = s"""
    select generators.guid,
           generators.service_guid,
           generators.key,
           generators.name,
           generators.description,
           generators.language,
           services.guid as service_guid,
           services.uri as service_uri,
           services.visibility as service_visibility
      from generators.generators
      join generators.services on services.guid = generators.service_guid and services.deleted_at is null
     where true
  """

  private[this] val InsertQuery = """
    insert into generators.generators
    (guid, service_guid, key, name, description, language, created_by_guid)
    values
    ({guid}::uuid, {service_guid}::uuid, {key}, {name}, {description}, {language}, {created_by_guid}::uuid)
  """

  def upsert(user: User, service: GeneratorService, form: GeneratorForm) {
    findAll(
      Authorization.User(user.guid),
      serviceGuid = Some(service.guid),
      key = Some(form.key.trim),
      limit = 1
    ).headOption match {
      case None => {
        DB.withConnection { implicit c =>
          create(c, user, service, form)
        }
      }
      case Some(generator) => {
        val existing = GeneratorForm(
          key = generator.key,
          name = generator.name,
          description = generator.description,
          language = generator.language
        )

        if (existing != form) {
          DB.withConnection { implicit c =>
            softDelete(c, user, generator)
            create(c, user, service, form)
          }
        }
      }
    }
  }

  private def create(implicit c: java.sql.Connection, user: User, service: GeneratorService, form: GeneratorForm): UUID = {
    val guid = UUID.randomUUID

    SQL(InsertQuery).on(
      'guid -> guid,
      'service_guid -> service.guid,
      'key -> form.key.trim,
      'name -> form.name.trim,
      'description -> form.description.map(_.trim),
      'language -> form.language.map(_.trim),
      'created_by_guid -> user.guid,
      'updated_by_guid -> user.guid
    ).execute()

    guid
  }

  def softDelete(deletedBy: User, generator: Generator) {
    DB.withConnection { implicit c =>
      softDelete(c, deletedBy: User, generator: Generator)
    }
  }

  private[this] def softDelete(implicit c: java.sql.Connection, deletedBy: User, generator: Generator) {
    SoftDelete.delete(c, "generators.generators", deletedBy, generator.guid)
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    serviceGuid: Option[UUID] = None,
    serviceUri: Option[String] = None,
    key: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Generator] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      authorization.generatorServicesFilter().map { v => s"and $v" },
      guid.map { v => "and generators.guid = {guid}::uuid" },
      serviceGuid.map { v => "and generators.service_guid = {service_guid}::uuid" },
      serviceUri.map { v => "and lower(services.uri) = lower(trim({uri}))" },
      key.map { v => "and generators.key = {key}" },
      isDeleted.map(db.Filters.isDeleted("generators", _))
    ).flatten.mkString("\n   ") + s" order by lower(generators.name), lower(generators.key), generators.created_at desc limit ${limit} offset ${offset}"

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      serviceGuid.map('service_guid -> _.toString),
      serviceUri.map('service_uri -> _),
      key.map('key -> _)
    ).flatten ++ authorization.bindVariables

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ) = Generator(
    guid = row[UUID]("guid"),
    service = ServicesDao.fromRow(row, Some("service")),
    key = row[String]("key"),
    name = row[String]("name"),
    description = row[Option[String]]("description"),
    language = row[Option[String]]("language")
  )

}
