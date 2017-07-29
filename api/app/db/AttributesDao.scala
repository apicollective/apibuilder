package db

import io.apibuilder.api.v0.models.{Attribute, AttributeForm, User}
import io.apibuilder.common.v0.models.Audit
import lib.{Validation, UrlKey}
import anorm._
import javax.inject.{Inject, Singleton}
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

@Singleton
class AttributesDao @Inject() () {

  private[db] val BaseQuery = s"""
    select attributes.guid,
           attributes.name,
           attributes.description,
           ${AuditsDao.query("attributes")}
      from attributes
     where true
  """

  private[this] val InsertQuery = """
    insert into attributes
    (guid, name, description, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {name}, {description}, {user_guid}::uuid, {user_guid}::uuid)
  """

  private[this] val UpdateQuery = """
    update attributes
       set name = {name},
           description = {description},
           updated_by_guid = {user_guid}::uuid
     where guid = {guid}::uuid
  """

  def validate(
    form: AttributeForm
  ): Seq[io.apibuilder.api.v0.models.Error] = {

    val nameErrors = if (form.name.trim.isEmpty) {
      Seq(s"Attribute name is required")
    } else {
      UrlKey.validate(form.name.trim, "Name") match {
        case Nil => {
          findByName(form.name) match {
            case None => Nil
            case Some(_) => {
              Seq("Attribute with this name already exists")
            }
          }
        }
        case errors => {
          errors
        }
      }
    }

    Validation.errors(nameErrors)
  }

  def create(user: User, form: AttributeForm): Attribute = {
    val errors = validate(form)
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    val guid = UUID.randomUUID()

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'name -> form.name.trim,
        'description -> form.description.map(_.trim),
        'user_guid -> user.guid
      ).execute()
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create attribute")
    }
  }

  def softDelete(deletedBy: User, org: Attribute) {
    SoftDelete.delete("attributes", deletedBy, org.guid)
  }

  def findByGuid(guid: UUID): Option[Attribute] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findByName(name: String): Option[Attribute] = {
    findAll(name = Some(name), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    name: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Attribute] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and attributes.guid = {guid}::uuid" },
      name.map { v => "and attributes.name = lower(trim({name}))" },
      isDeleted.map(Filters.isDeleted("attributes", _)),
      Some(s"order by lower(attributes.name) limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      name.map('name ->_)
    ).flatten

    DB.withConnection { implicit c =>
      sys.error("TODO PARSER") // SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row,
    prefix: Option[String] = None
  ): Attribute = {
    val p = prefix.map( _ + "_").getOrElse("")

    Attribute(
      guid = row[UUID](s"${p}guid"),
      name = row[String](s"${p}name"),
      description = row[Option[String]](s"${p}description"),
      audit = AuditsDao.fromRow(row, prefix)
    )
  }

}
