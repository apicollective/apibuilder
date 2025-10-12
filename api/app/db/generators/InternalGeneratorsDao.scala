package db.generators

import cats.data.ValidatedNec
import cats.implicits.*
import db.generated.generators.GeneratorsDao
import db.{Filters, InternalUser}
import io.apibuilder.api.v0.models.*
import io.flow.postgresql.Query

import java.util.UUID
import javax.inject.Inject

case class InternalGenerator(db: _root_.db.generated.generators.Generator) {
  val guid: UUID = db.guid
  val key: String = db.key
  val name: String = db.name
  val description: Option[String] = db.description
  val language: Option[String] = db.language
  val attributes: Seq[String] = db.attributes
  val serviceGuid: UUID = db.serviceGuid
}

class InternalGeneratorsDao @Inject()(
  dao: GeneratorsDao,
) {

  def upsert(user: InternalUser, form: GeneratorForm): ValidatedNec[String, InternalGenerator] = {
    findByKey(form.generator.key) match {
      case None => {
        dao.db.withConnection { c =>
          create(user, form)(using c).validNec
        }
      }

      case Some(existing) if existing.serviceGuid == form.serviceGuid => {
        if (isDifferent(existing, form)) {
          // Update to catch any updates to properties
          dao.db.withTransaction { implicit c =>
            dao.delete(c, user.guid, existing.db)
            create(user, form)(using c).validNec
          }
        } else {
          existing.validNec
        }
      }
      case Some(existing) => {
        s"Another service already has a generator with the key[${form.generator.key}]".invalidNec
      }
    }
  }

  private def isDifferent(generator: InternalGenerator, form: GeneratorForm): Boolean = {
    generator.name != form.generator.name ||
      generator.language != form.generator.language ||
      generator.attributes != form.generator.attributes ||
      generator.description != form.generator.description
  }

  def findByKey(key: String): Option[InternalGenerator] = {
    findAll(
      key = Some(key),
      limit = Some(1)
    ).headOption
  }

  def findByGuid(guid: UUID): Option[InternalGenerator] = {
    findAll(
      guid = Some(guid),
      limit = Some(1)
    ).headOption
  }

  def softDeleteAllByServiceGuid(c: java.sql.Connection, deletedBy: InternalUser, serviceGuid: UUID): Unit = {
    val all = dao.findAllWithConnection(
      c,
      limit = None
    )( using (q: Query) => {
      q.equals("service_guid", serviceGuid).isNull("deleted_at")
    })
    if (all.nonEmpty) {
      dao.deleteAllByGuids(c, deletedBy.guid, all.map(_.guid))
    }
  }

  private def create(user: InternalUser, form: GeneratorForm)(implicit c: java.sql.Connection): InternalGenerator = {
    val guid = dao.insert(c, user.guid, _root_.db.generated.generators.GeneratorForm(
      serviceGuid = form.serviceGuid,
      key = form.generator.key.trim,
      name = form.generator.name.trim,
      description = form.generator.description.map(_.trim).filterNot(_.isEmpty),
      language = form.generator.language.map(_.trim),
      attributes = form.generator.attributes.map(_.trim).flatMap(optionIfEmpty)
    ))

    InternalGenerator(
      dao.findByGuidWithConnection(c, guid).getOrElse {
        sys.error("Failed to create generator")
      }
    )
  }

  private def optionIfEmpty(value: String): Option[String] = {
    value.trim match {
      case "" => None
      case v => Some(v)
    }
  }

  def softDelete(deletedBy: InternalUser, generator: InternalGenerator): Unit = {
    dao.delete(deletedBy.guid, generator.db)
  }

  def findAll(
               guid: Option[UUID] = None,
               serviceGuid: Option[UUID] = None,
               serviceUri: Option[String] = None,
               key: Option[String] = None,
               attributeName: Option[String] = None,
               isDeleted: Option[Boolean] = Some(false),
               limit: Option[Long],
               offset: Long = 0
             ): Seq[InternalGenerator] = {
    dao.findAll(
      guid = guid,
      limit = limit,
      offset = offset
    )( using (q: Query) => {
      q.equals("service_guid", serviceGuid)
        .and(
          serviceUri.map { _ =>
            "lower(services.uri) = lower(trim({service_uri}))"
          }
        ).bind("service_uri", serviceUri)
        .and(
          key.map { _ =>
            "lower(key) = lower(trim({generator_key}))"
          }
        ).bind("generator_key", key)
        .and(
          attributeName.map { _ =>
            // TODO: structure this filter
            "attributes::text like '%' || lower(trim({attribute_name})) || '%'"
          }
        ).bind("attribute_name", attributeName)
        .and(isDeleted.map(Filters.isDeleted("generators", _)))
        .orderBy("lower(name), lower(key), created_at desc")
    }).map(InternalGenerator(_))
  }
}
