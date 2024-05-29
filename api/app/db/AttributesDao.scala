package db

import io.apibuilder.api.v0.models.{Attribute, AttributeForm, User}
import io.flow.postgresql.Query
import lib.{Validation, UrlKey}
import anorm._
import javax.inject.{Inject, Singleton}
import play.api.db._
import java.util.UUID

@Singleton
class AttributesDao @Inject() (
  @NamedDatabase("default") db: Database
) {

  private[this] val dbHelpers = DbHelpers(db, "attributes")

  private[this] val BaseQuery = Query(s"""
    select attributes.guid,
           attributes.name,
           attributes.description,
           ${AuditsDao.query("attributes")}
      from attributes
  """)

  private[this] val InsertQuery = """
    insert into attributes
    (guid, name, description, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {name}, {description}, {user_guid}::uuid, {user_guid}::uuid)
  """

  def validate(
    form: AttributeForm
  ): Seq[io.apibuilder.api.v0.models.Error] = {

    val nameErrors = if (form.name.trim.isEmpty) {
      Seq("Attribute name is required")
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

    db.withConnection { implicit c =>
      SQL(InsertQuery).on(
        "guid" -> guid,
        "name" -> form.name.trim,
        "description" -> form.description.map(_.trim),
        "user_guid" -> user.guid
      ).execute()
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create attribute")
    }
  }

  def softDelete(deletedBy: User, attributes: Attribute): Unit = {
    dbHelpers.delete(deletedBy.guid, attributes.guid)
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
    db.withConnection { implicit c =>
      BaseQuery.
        equals("attributes.guid", guid).
        and(
          name.map { _ =>
            "lower(trim(attributes.name)) = lower(trim({name}))"
          }
        ).bind("name", name).
        and(isDeleted.map(Filters.isDeleted("attributes", _))).
        limit(limit).
        offset(offset).
        orderBy("lower(attributes.name)").
        as(io.apibuilder.api.v0.anorm.parsers.Attribute.parser().*)
    }
  }

}
