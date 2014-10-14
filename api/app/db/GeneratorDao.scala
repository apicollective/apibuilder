package db

import com.gilt.apidoc.models.{Visibility, Generator, User}
import com.gilt.apidoc.models.json._
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

case class GeneratorCreateForm(key: String, uri: String, visibility: Visibility)


object GeneratorCreateForm {
  implicit val generatorCreateFormReads = Json.reads[GeneratorCreateForm]
}

case class GeneratorUpdateForm(visibility: Option[Visibility], enabled: Option[Boolean])

object GeneratorUpdateForm {
  implicit val generatorUpdateFormReads = Json.reads[GeneratorUpdateForm]
}

case class GeneratorOrgUpdateForm(enabled: Boolean)

object GeneratorOrgUpdateForm {
  implicit val generatorUpdateFormReads = Json.reads[GeneratorOrgUpdateForm]
}

object GeneratorDao {


  private val BaseQuery = s"""
    select generators.guid,
           generators.key,
           generators.user_guid,
           generators.uri,
           generators.visibility,
           generators.name,
           generators.description,
           generators.language,
           memberships.organization_guid,
           generator_users.enabled
      from generators
      left join memberships on memberships.deleted_at is null
                            and memberships.user_guid = generators.user_guid
      left join generator_users on generator_users.deleted_at is null
                            and generator_users.user_guid = generators.user_guid
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
      ownerGuid = createdBy.guid,
      enabled = true
    )

    SQL("""
      insert into generators
      (guid, key, uri, user_guid, visibility, name, description, language, created_by_guid)
      values
      ({guid}::uuid, {key}, {uri}, {user_guid}::uuid, {visibility}, {name}, {description}, {language}, {created_by_guid}::uuid)
    """).on(
      'guid -> generator.guid,
      'key -> generator.key,
      'uri -> generator.uri,
      'user_guid -> generator.ownerGuid,
      'visibility -> visibility.toString,
      'name -> generator.name,
      'description -> generator.description,
      'language -> generator.language,
      'created_by_guid -> createdBy.guid
    ).execute()

    SQL("""
      insert into generator_users
      (guid, generator_guid, user_guid, enabled, created_by_guid)
      values
      ({guid}::uuid, {generator_guid}::uuid, {user_guid}::uuid, true, {created_by_guid}::uuid)
    """).on(
      'guid -> UUID.randomUUID(),
      'generator_guid -> generator.guid,
      'user_guid -> generator.ownerGuid,
      'created_by_guid -> generator.ownerGuid
    ).execute()

    generator
  }

  def update(user: User, generator: Generator, form: GeneratorUpdateForm): Generator = {
    DB.withConnection { implicit c =>
      update(c, user, generator, form)
    }
  }

  private[db] def update(implicit c: java.sql.Connection, user: User, generator: Generator, form: GeneratorUpdateForm): Generator = {
    val (queryParts: Seq[String], bind: Seq[NamedParameter]) = Seq(
      form.visibility.map(v => "visibility = {visibility}" -> (('visibility -> v.toString): NamedParameter))
    ).flatten.unzip

    if (queryParts.size > 0) {
      val updateQuery = s"update generators set ${queryParts.mkString(", ")} where guid = {guid}::uuid"

      SQL(updateQuery).on(bind ++ (Seq('guid -> generator.guid): Seq[NamedParameter]): _*).execute()
    }

    form.enabled.foreach { enable =>
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
        'enabled -> enable,
        'created_by_guid -> user.guid
      ).execute()
    }

    generator.copy(visibility = form.visibility.getOrElse(generator.visibility),
                   enabled = form.enabled.getOrElse(generator.enabled))
  }

  def orgUpdate(user: User, generatorGuid: UUID, orgGuid: UUID, enable: Boolean) = {
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
    SoftDelete.delete("generators", deletedBy, generator.guid)
  }

  def findAll(
    user: User,
    guid: Option[UUID] = None,
    keyAndUri: Option[(String, String)] = None
  ): Seq[Generator] = {

   // get generator enabled choice for all orgs of the user
    val orgEnabledGenerators: Set[UUID] = DB.withConnection { implicit c =>
      SQL(OrgGeneratorsQuery).on('user_guid -> user.guid)().toList.map { row =>
        row[UUID]("generator_guid")
      }.toSet
    }

    // Query generators
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map(_ => "and guid = {guid}"),
      keyAndUri.map(_ => "and key = {key} and uri = {uri}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      Some('user_guid -> user.guid),
      Some('user_guid -> user.guid),
      guid.map('guid -> _),
      keyAndUri.map('key -> _._1),
      keyAndUri.map('uri -> _._2)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        val genGuid = row[UUID]("guid")
        Generator(
          guid = genGuid,
          key = row[String]("key"),
          uri = row[String]("uri"),
          name = row[String]("name"),
          description = row[Option[String]]("description"),
          language = row[Option[String]]("language"),
          visibility = Visibility(row[String]("visibility")),
          ownerGuid = row[UUID]("user_guid"),
          enabled = row[Option[Boolean]]("enabled").getOrElse(orgEnabledGenerators.contains(genGuid))
        )
      }.toSeq.distinct
    }
  }

}
