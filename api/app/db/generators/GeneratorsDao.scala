package db.generators

import db.{AuditsDao, Authorization, SoftDelete}
import com.bryzek.apidoc.api.v0.models.{GeneratorForm, Error, GeneratorService, GeneratorWithService, User}
import com.bryzek.apidoc.generator.v0.models.Generator
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
           ${AuditsDao.queryWithAlias("services", "service")}
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

  private[this] val SoftDeleteByKeyQuery = """
    update generators.generators
       set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now()
     where service_guid = {service_guid}
       and key = {key}
  """

  def upsert(user: User, form: GeneratorForm) {
    findAll(
      Authorization.User(user.guid),
      serviceGuid = Some(form.serviceGuid),
      key = Some(form.generator.key.trim),
      limit = 1
    ).headOption match {
      case None => {
        DB.withConnection { implicit c =>
          create(c, user, form)
        }
      }
      case Some(existing) => {
        if (existing.generator != form.generator) {
          DB.withConnection { implicit c =>
            softDelete(c, user, form.serviceGuid, form.generator.key)
            create(c, user, form)
          }
        }
      }
    }
  }

  private def create(implicit c: java.sql.Connection, user: User, form: GeneratorForm): UUID = {
    val guid = UUID.randomUUID

    SQL(InsertQuery).on(
      'guid -> guid,
      'service_guid -> form.serviceGuid,
      'key -> form.generator.key.trim,
      'name -> form.generator.name.trim,
      'description -> form.generator.description.map(_.trim),
      'language -> form.generator.language.map(_.trim),
      'created_by_guid -> user.guid,
      'updated_by_guid -> user.guid
    ).execute()

    guid
  }

  def softDelete(deletedBy: User, gws: GeneratorWithService) {
    DB.withConnection { implicit c =>
      softDelete(c, deletedBy, gws.service.guid, gws.generator.key)
    }
  }

  private[this] def softDelete(implicit c: java.sql.Connection, deletedBy: User, serviceGuid: UUID, generatorKey: String) {
    SQL(SoftDeleteByKeyQuery).on(
      'deleted_by_guid -> deletedBy.guid,
      'service_guid -> serviceGuid,
      'key -> generatorKey.trim
    ).execute()
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
  ): Seq[GeneratorWithService] = {
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
  ) = GeneratorWithService(
    service = GeneratorService(
      guid = row[UUID]("service_guid"),
      uri = row[String]("service_uri"),
      audit = AuditsDao.fromRow(row, Some("service"))
    ),
    generator = Generator(
      key = row[String]("key"),
      name = row[String]("name"),
      description = row[Option[String]]("description"),
      language = row[Option[String]]("language")
    )
  )

}
