package db

import com.gilt.apidoc.api.v0.models.{Error, Generator, GeneratorCreateForm, User, Visibility}
import anorm._
import play.api.db._
import play.api.Play.current
import lib.{UrlKey, Validation}
import java.net.{MalformedURLException, URL}
import java.util.UUID
import scala.util.{Failure, Success, Try}

object GeneratorsDao {

  private val BaseQuery = s"""
    select generators.guid,
           generators.key,
           generators.created_at,
           generators.uri,
           generators.visibility
      from generators
     where true
  """

  private val InsertQuery = """
    insert into generators
    (guid, key, uri, user_guid, visibility, created_by_guid)
    values
    ({guid}::uuid, {key}, {uri}, {user_guid}::uuid, {visibility}, {created_by_guid}::uuid)
  """

  def validate(form: GeneratorCreateForm): Seq[Error] = {
    val urlErrors = Try(new URL(form.uri)) match {
      case Success(url) => {
        if (form.uri.toLowerCase.startsWith("http")) {
          Seq.empty
        } else {
          Seq("URL must start with http")
        }
      }
      case Failure(e) => e match {
        case e: MalformedURLException => Seq(s"URL is not valid: ${e.getMessage}")
      }
    }

    val keyErrors = UrlKey.validate(form.key)

    Validation.errors(urlErrors ++ keyErrors)
  }

  def create(createdBy: User,
             key: String,
             uri: String,
             visibility: Visibility,
             name: String,
             description: Option[String],
             language: Option[String]): Generator = {
    DB.withConnection { implicit c =>
      create(c, createdBy, key, uri, visibility, name, description, language)
    }
  }

  private[db] def create(implicit c: java.sql.Connection,
                         createdBy: User,
                         key: String,
                         uri: String,
                         visibility: Visibility,
                         name: String,
                         description: Option[String],
                         language: Option[String]): Generator = {
    val generator = Generator(
      guid = UUID.randomUUID(),
      key = key.trim.toLowerCase,
      uri = uri,
      visibility = visibility,
      name = name.trim,
      description = description.map(_.trim),
      language = language.map(_.trim)
    )

    SQL(InsertQuery).on(
      'guid -> generator.guid,
      'key -> generator.key,
      'uri -> generator.uri,
      'user_guid -> createdBy.guid,
      'visibility -> visibility.toString,
      'created_by_guid -> createdBy.guid
    ).execute()

    generator
  }

  def visibilityUpdate(user: User, generator: Generator, visibility: Visibility): Generator = {
    DB.withConnection { implicit c =>
      val updateQuery = s"update generators set visibility = {visibility} where guid = {guid}::uuid"
      SQL(updateQuery).on(
        'visibility -> visibility.toString,
        'guid -> generator.guid
      ).execute()

      generator.copy(visibility = visibility)
    }
  }

  def orgEnabledUpdate(user: User, generatorGuid: UUID, orgGuid: UUID, enable: Boolean) = {
    DB.withConnection { implicit c =>
      if (enable) {
        val insertQuery = s"insert into generator_organizations (guid, generator_guid, organization_guid, created_by_guid) values({guid}::uuid, {generator_guid}::uuid, {organization_guid}::uuid, {created_by_guid}::uuid)"
        SQL(insertQuery).on(
          'guid -> UUID.randomUUID(),
          'generator_guid -> generatorGuid,
          'organization_guid -> orgGuid,
          'created_by_guid -> user.guid
        ).execute()
      } else {
        val updateQuery = "update generators set deleted_at = now(), deleted_by_guid = {deleted_by_guid}::uuid where generator_guid = {generator_guid}::uuid and organization_guid = {organization_guid}::uuid"
        SQL(updateQuery).on(
          'deleted_by_guid -> user.guid,
          'generator_guid -> generatorGuid,
          'organization_guid -> orgGuid
        ).execute()
      }
    }
  }

  def softDelete(deletedBy: User, generator: Generator) {
    SoftDelete.delete("generator_organizations", deletedBy, ("generator_guid", Some("::uuid"), generator.guid.toString))
    SoftDelete.delete("generator_users", deletedBy, ("generator_guid", Some("::uuid"), generator.guid.toString))
    SoftDelete.delete("generators", deletedBy, generator.guid)
  }

  def findAll(
    auth: Authorization,
    guid: Option[UUID] = None,
    key: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Generator] = {
    // TODO: Implement Authorization

    // Query generators
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map(_ => "and generators.guid = {guid}::uuid"),
      key.map(_ => "and generators.key = {key}"),
      isDeleted.map(Filters.isDeleted("generators", _)),
      Some(s" order by lower(generators.key)"),
      Some(s" limit $limit offset $offset")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _),
      key.map('key -> _)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        val genGuid = row[UUID]("guid")
        val uri = row[String]("uri")
        val visibility = Visibility(row[String]("visibility"))

        Generator(
          guid = genGuid,
          key = row[String]("key"),
          uri = uri,
          name = "",
          description = None,
          language = None,
          visibility = visibility
        )
      }.toSeq
    }
  }

}
