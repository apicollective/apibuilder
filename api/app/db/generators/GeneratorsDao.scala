package db.generators

import anorm._
import cats.data.ValidatedNec
import cats.implicits._
import db.{Authorization, Filters}
import io.apibuilder.api.v0.models._
import io.apibuilder.generator.v0.models.Generator
import io.flow.postgresql.Query
import play.api.db._
import play.api.libs.json.Json

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.util.{Failure, Success, Try}

case class InternalGenerator(
                            guid: UUID,
                            key: String,
                            name: String,
                            description: Option[String],
                            language: Option[String],
                            attributes: Seq[String],
                            serviceGuid: UUID
                            ) {
  def model: Generator = Generator(
    key = key,
    name = name,
    language = language,
    description = description,
    attributes = attributes
  )
}

@Singleton
class GeneratorsDao @Inject() (
  @NamedDatabase("default") db: Database,
  servicesDao: ServicesDao,
) {

  private val BaseQuery = Query(
    s"""
    select generators.guid,
           generators.key,
           generators.name,
           generators.description,
           generators.language,
           generators.attributes::text as attributes,
           generators.service_guid,
      from generators.generators
  """)

  private val InsertQuery =
    """
    insert into generators.generators
    (guid, service_guid, key, name, description, language, attributes, created_by_guid)
    values
    ({guid}::uuid, {service_guid}::uuid, {key}, {name}, {description}, {language}, {attributes}::json, {created_by_guid}::uuid)
  """

  private val SoftDeleteByKeyQuery =
    """
    update generators.generators
       set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now()
     where key = lower(trim({key}))
       and service_guid = {service_guid}::uuid
       and deleted_at is null
  """

  def upsert(user: User, form: GeneratorForm): ValidatedNec[String, GeneratorWithService] = {
    findByKey(form.generator.key) match {
      case None => {
        val gen = db.withConnection { implicit c =>
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
        gen.validNec
      }
      case Some(existing) => {
        if (existing.service.guid == form.serviceGuid) {
          if (isDifferent(existing.generator, form)) {
            // Update to catch any updates to properties
            val generatorGuid = db.withConnection { implicit c =>
              softDelete(c, user, existing.service.guid, existing.generator.key)
              create(c, user, form)
            }
            findByGuid(generatorGuid).getOrElse {
              sys.error("Failed to create generator")
            }.validNec
          } else {
            existing.validNec
          }
        } else {
          s"Another service already has a generator with the key[${form.generator.key}]".invalidNec
        }
      }
    }
  }

  private def isDifferent(generator: Generator, form: GeneratorForm): Boolean = {
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
      "guid" -> guid,
      "service_guid" -> form.serviceGuid,
      "key" -> form.generator.key.trim,
      "name" -> form.generator.name.trim,
      "description" -> form.generator.description.map(_.trim),
      "language" -> form.generator.language.map(_.trim),
      "attributes" -> Json.toJson(
        form.generator.attributes.map(_.trim).flatMap(optionIfEmpty)
      ).toString,
      "created_by_guid" -> user.guid,
      "updated_by_guid" -> user.guid
    ).execute()

    guid
  }

  private def optionIfEmpty(value: String): Option[String] = {
    value.trim match {
      case "" => None
      case v => Some(v)
    }
  }

  def softDelete(deletedBy: User, gws: GeneratorWithService): Unit = {
    db.withConnection { implicit c =>
      softDelete(c, deletedBy, gws.service.guid, gws.generator.key)
    }
  }

  private def softDelete(implicit c: java.sql.Connection, deletedBy: User, serviceGuid: UUID, generatorKey: String): Unit = {
    SQL(SoftDeleteByKeyQuery).on(
      "deleted_by_guid" -> deletedBy.guid,
      "service_guid" -> serviceGuid,
      "key" -> generatorKey
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
    addService(
      db.withConnection { implicit c =>
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
          and(isDeleted.map(Filters.isDeleted("generators", _))).
          orderBy("lower(generators.name), lower(generators.key), generators.created_at desc").
          limit(limit).
          offset(offset).
          as(parser.*)
      }
    )
  }

  private def addService(generators: List[InternalGenerator]): Seq[GeneratorWithService] = {
    val services = servicesDao.findAll(
      Authorization.All,
      guids = Some(generators.map(_.serviceGuid).distinct),
      limit = None,
    ).map { s => s.guid -> s }.toMap

    generators.flatMap { g =>
      services.get(g.serviceGuid).map { s =>
        GeneratorWithService(s, g.model)
      }
    }

  }

  private def parser: RowParser[InternalGenerator] = {
    SqlParser.get[UUID]("guid") ~
      SqlParser.str("key") ~
      SqlParser.str("name") ~
      SqlParser.str("description").? ~
      SqlParser.str("language").? ~
      SqlParser.str("attributes") ~
      SqlParser.get[UUID]("service_guid") map {
      case guid ~ key ~ name ~ description ~ language ~ attributes ~ serviceGuid => {
        InternalGenerator(
          guid = guid,
          key = key,
          name = name,
          description = description,
          language = language,
          attributes = Json.parse(attributes).as[Seq[String]],
          serviceGuid = serviceGuid
        )
      }
    }
  }
}
