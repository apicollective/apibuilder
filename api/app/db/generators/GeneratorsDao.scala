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
import scala.util.{Failure, Success, Try}

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
           ${AuditsDao.queryCreationWithAlias("services", "service")}
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
     where key = lower(trim({key}))
       and service_guid = {service_guid}::uuid
       and deleted_at is null
  """

  def upsert(user: User, form: GeneratorForm): Either[Seq[String], GeneratorWithService] = {
    findByKey(form.generator.key) match {
      case None => {
        val gen = DB.withConnection { implicit c =>
          Try(create(c, user, form)) match {
            case Success(guid) => {
              findByGuid(guid).getOrElse {
                sys.error("Failed to create generator")
              }
            }
            case Failure(ex) => {
              findByKey(form.generator.key).getOrElse {
                sys.error(s"Error upserting generator[${form.generator.key}]: $ex")
              }
            }
          }
        }
        Right(gen)
      }
      case Some(existing) => {
        if (existing.service.guid == form.serviceGuid) {
          if (isDifferent(existing.generator, form)) {
            // Update to catch any updates to properties
            val generatorGuid = DB.withConnection { implicit c =>
              softDelete(c, user, existing.service.guid, existing.generator.key)
              create(c, user, form)
            }
            Right(
              findByGuid(generatorGuid).getOrElse {
                sys.error("Failed to create generator")
              }
            )
          } else {
            Right(existing)
          }
        } else {
          Left(Seq(s"Another service already has a generator with the key[${form.generator.key}]"))
        }
      }
    }
  }

  private[this] def isDifferent(generator: Generator, form: GeneratorForm): Boolean = {
    generator.name != form.generator.name ||
    generator.language != form.generator.language ||
    generator.description != form.generator.description
  }

  def findByKey(key: String): Option[GeneratorWithService] = {
    findAll(
      Authorization.All,
      key = Some(key),
      limit = 1
    ).headOption
  }

  def findByGuid(guid: UUID): Option[GeneratorWithService] = {
    findAll(
      Authorization.All,
      guid = Some(guid),
      limit = 1
    ).headOption
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
      'key -> generatorKey
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
      key.map { v => "and lower(trim(generators.key)) = lower(trim({key}))" },
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
      audit = AuditsDao.fromRowCreation(row, Some("service"))
    ),
    generator = Generator(
      key = row[String]("key"),
      name = row[String]("name"),
      description = row[Option[String]]("description"),
      language = row[Option[String]]("language")
    )
  )

}
