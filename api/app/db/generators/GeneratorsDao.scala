package db.generators

import db.Authorization
import io.apibuilder.api.v0.models._
import io.apibuilder.generator.v0.models.Generator
import io.flow.postgresql.Query
import javax.inject.{Inject, Singleton}

import anorm._
import play.api.db._
import play.api.Play.current
import java.util.UUID

import play.api.libs.json.Json

import scala.util.{Failure, Success, Try}

@Singleton
class GeneratorsDao @Inject() () {

  private[this] val BaseQuery = Query(s"""
    select generators.guid,
           generators.key,
           generators.name,
           generators.description,
           generators.language,
           generators.attributes::text as attributes,
           services.guid as service_guid,
           services.uri as service_uri,
           services.created_at as service_created_at,
           services.created_by_guid as service_created_by_guid,
           services.created_at as service_updated_at,
           services.created_by_guid as service_updated_by_guid
      from generators.generators
      join generators.services on services.guid = generators.service_guid and services.deleted_at is null
  """)

  private[this] val InsertQuery = """
    insert into generators.generators
    (guid, service_guid, key, name, description, language, attributes, created_by_guid)
    values
    ({guid}::uuid, {service_guid}::uuid, {key}, {name}, {description}, {language}, {attributes}::json, {created_by_guid}::uuid)
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
    generator.attributes != form.generator.attributes ||
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
      'attributes -> Json.toJson(
        form.generator.attributes.map(_.trim).flatMap(optionIfEmpty)
      ).toString,
      'created_by_guid -> user.guid,
      'updated_by_guid -> user.guid
    ).execute()

    guid
  }

  private[this] def optionIfEmpty(value: String): Option[String] = {
    value.trim match {
      case "" => None
      case v => Some(v)
    }
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
    attributeName: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[GeneratorWithService] = {
    DB.withConnection { implicit c =>
      authorization.generatorServicesFilter(BaseQuery).
        equals("generators.guid", guid).
        equals("generators.service_guid", serviceGuid).
        and(
          serviceUri.map { _ =>
            "lower(services.uri) = lower(trim({service_uri}))"
          }
        ).bind("service_uri", serviceUri).
        and(
          key.map { _ =>
            "lower(generators.key) = lower(trim({generator_key}))"
          }
        ).bind("generator_key", key).
        and(
          attributeName.map { _ =>
            // TODO: structure this filter
            "generators.attributes::text like '%' || lower(trim({attribute_name})) || '%'"
          }
        ).bind("attribute_name", attributeName).
        and(isDeleted.map(db.Filters.isDeleted("generators", _))).
        orderBy("lower(generators.name), lower(generators.key), generators.created_at desc").
        limit(limit).
        offset(offset).
        as(parser().*)
    }
  }

  private[this] def parser(): RowParser[GeneratorWithService] = {
    SqlParser.get[_root_.java.util.UUID]("service_guid") ~
      SqlParser.str("service_uri") ~
      io.apibuilder.common.v0.anorm.parsers.Audit.parserWithPrefix("service") ~
      io.apibuilder.generator.v0.anorm.parsers.Generator.parser() map {
      case serviceGuid ~ serviceUri ~ serviceAudit ~ generator => {
        GeneratorWithService(
          service = GeneratorService(
            guid = serviceGuid,
            uri = serviceUri,
            audit = serviceAudit
          ),
          generator = generator
        )
      }
    }
  }

}
