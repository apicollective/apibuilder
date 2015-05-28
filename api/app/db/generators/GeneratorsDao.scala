package db.generators

import db.SoftDelete
import com.gilt.apidoc.api.v0.models.{Error, User}
import core.Util
import lib.Validation
import anorm._
import play.api.db._
import play.api.Play.current
import java.util.UUID

object GeneratorsDao {

  private[this] val BaseQuery = s"""
    select generators.guid,
           generators.source_guid,
           generators.key,
           generators.name,
           generators.description,
           generators.language,
           sources.guid as source_guid,
           sources.uri as source_uri
      from generators.generators
      join generators.sources on sources.guid = generators.source_guid and sources.deleted_at is null
     where true
  """

  private[this] val InsertQuery = """
    insert into generators.generators
    (guid, source_guid, key, name, description, language, created_by_guid)
    values
    ({guid}::uuid, {source_guid}::uuid, {key}, {name}, {description}, {language}, {created_by_guid}::uuid)
  """

  def upsert(user: User, source: Source, form: GeneratorForm) {
    findAll(
      sourceGuid = Some(source.guid),
      key = Some(form.key.trim),
      limit = 1
    ).headOption match {
      case None => {
        DB.withConnection { implicit c =>
          create(c, user, source, form)
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
            create(c, user, source, form)
          }
        }
      }
    }
  }

  private def create(implicit c: java.sql.Connection, user: User, source: Source, form: GeneratorForm): UUID = {
    val guid = UUID.randomUUID

    SQL(InsertQuery).on(
      'guid -> guid,
      'source_guid -> source.guid,
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
    guid: Option[UUID] = None,
    sourceGuid: Option[UUID] = None,
    key: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Generator] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and generators.guid = {guid}::uuid" },
      sourceGuid.map { v => "and generators.source_guid = {source_guid}::uuid" },
      key.map { v => "and generators.key = {key}" },
      isDeleted.map(db.Filters.isDeleted("generators", _))
    ).flatten.mkString("\n   ") + s" order by lower(generators.name), lower(generators.key), generators.created_at desc limit ${limit} offset ${offset}"

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      sourceGuid.map('source_guid -> _.toString),
      key.map('key -> _)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ) = Generator(
    guid = row[UUID]("guid"),
    source = Source(
      guid = row[UUID]("source_guid"),
      uri = row[String]("source_uri")
    ),
    key = row[String]("key"),
    name = row[String]("name"),
    description = row[Option[String]]("description"),
    language = row[Option[String]]("language")
  )

}
