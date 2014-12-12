package db

import com.gilt.apidoc.models.{Error, Generator, GeneratorCreateForm, User, Visibility}
import anorm._
import play.api.db._
import play.api.Play.current
import lib.Validation
import java.net.{MalformedURLException, URL}
import java.util.{Date, UUID}
import scala.util.{Failure, Success, Try}

object GeneratorDao {

  private val BaseQuery = s"""
    select generators.guid,
           generators.key,
           generators.created_at,
           generators.uri,
           generators.visibility,
           memberships.organization_guid,
           generator_users.enabled,
           users.guid as user_guid,
           users.email as user_email,
           users.name as user_name
      from generators
      join users on users.guid = generators.user_guid
      left join memberships on memberships.deleted_at is null
                            and memberships.user_guid = generators.user_guid
      left join generator_users on generator_users.deleted_at is null
                            and generator_users.user_guid = {user_guid}::uuid
                            and generator_users.generator_guid = generators.guid
     where generators.deleted_at is null
     and (
       (generators.visibility = '${Visibility.Public}')
       or
       (generators.user_guid = {user_guid}::uuid)
       or
       (generators.visibility = '${Visibility.Organization}' and memberships.organization_guid in (
         select organization_guid from memberships
           where memberships.deleted_at is null
           and user_guid = {user_guid}::uuid
         )
       )
     )
  """

  private val OrgGeneratorsQuery = """
    select generator_guid from generator_organizations
      where deleted_at is null
      and organization_guid in (
        select organization_guid from memberships
          where deleted_at is null
          and user_guid = {user_guid}::uuid
      )""".stripMargin

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
        case e: MalformedURLException => Seq("URL is not valid")
      }
    }

    Validation.errors(urlErrors)
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
      key = key,
      uri = uri,
      visibility = visibility,
      name = name,
      description = description,
      language = language,
      owner = createdBy,
      enabled = true
    )

    SQL("""
      insert into generators
      (guid, key, uri, user_guid, visibility, created_by_guid)
      values
      ({guid}::uuid, {key}, {uri}, {user_guid}::uuid, {visibility}, {created_by_guid}::uuid)
    """).on(
      'guid -> generator.guid,
      'key -> generator.key,
      'uri -> generator.uri,
      'user_guid -> generator.owner.guid,
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

  def userEnabledUpdate(user: User, generator: Generator, enabled: Boolean): Generator = {
    DB.withConnection { implicit c =>
        val updateQuery = "update generator_users set deleted_at = now(), deleted_by_guid = {deleted_by_guid}::uuid where generator_guid = {generator_guid}::uuid and user_guid = {user_guid}::uuid"
        SQL(updateQuery).on(
          'deleted_by_guid -> user.guid,
          'generator_guid -> generator.guid,
          'user_guid -> user.guid
        ).execute()
        val insertQuery = s"insert into generator_users (guid, generator_guid, user_guid, enabled, created_by_guid) values ({guid}::uuid, {generator_guid}::uuid, {user_guid}::uuid, {enabled}, {created_by_guid}::uuid)"
        SQL(insertQuery).on(
          'guid -> UUID.randomUUID(),
          'generator_guid -> generator.guid,
          'user_guid -> user.guid,
          'enabled -> enabled,
          'created_by_guid -> user.guid
        ).execute()

      generator.copy(enabled = enabled)
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
    user: Option[User] = None,
    guid: Option[UUID] = None,
    key: Option[String] = None,
    keyAndUri: Option[(String, String)] = None,
    limit: Int = 50,
    offset: Int = 0
  ): Seq[Generator] = {
    require(
      key.isEmpty || keyAndUri.isEmpty,
      "Cannot specify key and keyAndUri"
    )

   // get generator enabled choice for all orgs of the user
    val orgEnabledGenerators: Set[UUID] = user match {
      case None => Set.empty
      case Some(u) => {
        DB.withConnection { implicit c =>
          SQL(OrgGeneratorsQuery).on('user_guid -> u.guid)().toList.map { row =>
            row[UUID]("generator_guid")
          }.toSet
        }
      }
    }

    // Query generators
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map(_ => "and generators.guid = {guid}::uuid"),
      key.map(_ => "and generators.key = {key}"),
      keyAndUri.map(_ => "and generators.key = {key} and generators.uri = {uri}"),
      Some(s"limit $limit offset $offset")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      Some('user_guid -> user.map(_.guid).getOrElse(UUID.randomUUID)), // TODO Avoid needing the random guid
      guid.map('guid -> _),
      key.map('key -> _),
      keyAndUri.map('key -> _._1),
      keyAndUri.map('uri -> _._2)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        val genGuid = row[UUID]("guid")
        val uri = row[String]("uri")
        val visibility = Visibility(row[String]("visibility"))
        val isEnabled = row[Option[Boolean]]("enabled")
        val owner = User(
          guid = row[UUID]("user_guid"),
          email = row[String]("user_email"),
          name = row[Option[String]]("user_name")
        )
        Generator(
          guid = genGuid,
          key = row[String]("key"),
          uri = uri,
          name = "",
          description = None,
          language = None,
          visibility = visibility,
          owner = owner,
          enabled = isEnabled.getOrElse(false) || (!user.isEmpty && isOwner(user.get.guid, owner)) || isTrusted(uri) || isOrgEnabled(visibility, genGuid, orgEnabledGenerators)
        ) -> row[Date]("created_at")
      }.toSeq.sortBy(_._2.getTime).map(_._1).distinct
    }
  }

  def isOwner(userGuid: UUID, owner: User): Boolean = userGuid == owner.guid

  val trustedUris = Seq("http://generator.apidoc.me", "http://generator.origin.apidoc.me", "http://localhost:9003")

  def isTrusted(uri: String): Boolean = trustedUris.contains(uri)

  def isOrgEnabled(visibility: Visibility, generatorGuid: UUID, orgEnabledGenerators: Set[UUID]): Boolean = {
    visibility == Visibility.Organization && orgEnabledGenerators.contains(generatorGuid)
  }

}
