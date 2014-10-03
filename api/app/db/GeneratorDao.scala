package db

import com.gilt.apidoc.models.{Generator, Organization, User}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

case class GeneratorForm(
                             name: String,
                             uri: String
                             )

object GeneratorForm {
  implicit val generatorFormReads = Json.reads[GeneratorForm]
}


object GeneratorDao {

  private val BaseQuery = """
    select guid, organization_guid, name, uri
      from generators
     where deleted_at is null
     and organization_guid organization_guid = {organization_guid}::uuid
  """

  def create(createdBy: User, org: Organization, name: String, uri: String): Generator = {
    DB.withConnection { implicit c =>
      create(c, createdBy, org, name, uri)
    }
  }

  private[db] def create(implicit c: java.sql.Connection, createdBy: User, org: Organization, name: String, uri: String): Generator = {
    val generator = Generator(
      guid = UUID.randomUUID,
      name = name,
      uri = uri
    )

    SQL("""
      insert into generators
      (guid, organization_guid, name, uri, created_by_guid)
      values
      ({guid}::uuid, {organization_guid}::uuid, {name}, {uri}, {created_by_guid}::uuid)
    """).on(
      'guid -> generator.guid,
      'organization_guid -> org.guid,
      'name -> generator.name,
      'uri -> generator.uri,
      'created_by_guid -> createdBy.guid
    ).execute()

    generator
  }

  def softDelete(deletedBy: User, generator: Generator) {
    SoftDelete.delete("generators", deletedBy, generator.guid)
  }

  def findAll(
    orgGuid: UUID,
    guid: Option[UUID] = None,
    uri: Option[String] = None
  ): Seq[Generator] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map(_ => "and guid = {guid}::uuid"),
      uri.map(_ => "and uri = {uri}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      Some('organization_guid -> orgGuid),
      guid.map('guid -> _),
      uri.map('uri -> _)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        Generator(
          guid = row[UUID]("guid"),
          name = row[String]("name"),
          uri = row[String]("uri")
        )
      }.toSeq
    }
  }

}
