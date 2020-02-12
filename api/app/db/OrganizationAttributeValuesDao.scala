package db

import io.apibuilder.api.v0.models.{Attribute, AttributeSummary, AttributeValue, AttributeValueForm, Organization, User}
import io.apibuilder.common.v0.models.Audit
import io.flow.postgresql.Query
import lib.Validation
import anorm._
import javax.inject.{Inject, Singleton}
import play.api.db._
import play.api.libs.json._
import java.util.UUID

@Singleton
class OrganizationAttributeValuesDao @Inject() (
  @NamedDatabase("default") db: Database,
  attributesDao: AttributesDao
) {

  private[this] val dbHelpers = DbHelpers(db, "organization_attribute_values")

  private[this] val BaseQuery = Query(s"""
    select organization_attribute_values.guid,
           organization_attribute_values.value,
           ${AuditsDao.query("organization_attribute_values")},
           attributes.guid as attribute_guid,
           attributes.name as attribute_name
      from organization_attribute_values
      join attributes on attributes.deleted_at is null and attributes.guid = organization_attribute_values.attribute_guid
  """)

  private[this] val InsertQuery = """
    insert into organization_attribute_values
    (guid, organization_guid, attribute_guid, value, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {attribute_guid}::uuid, {value}, {user_guid}::uuid, {user_guid}::uuid)
  """

  private[this] val UpdateQuery = """
    update organization_attribute_values
       set value = {value},
           updated_by_guid = {user_guid}::uuid
     where guid = {guid}::uuid
  """

  def validate(
    organization: Organization,
    attribute: AttributeSummary,
    form: AttributeValueForm,
    existing: Option[AttributeValue]
  ): Seq[io.apibuilder.api.v0.models.Error] = {

    val attributeErrors = attributesDao.findByName(attribute.name) match {
      case None => Seq("Attribute not found")
      case Some(_) => Nil
    }

    val valueErrors = if (form.value.trim.isEmpty) {
      Seq(s"Value is required")
    } else {
      findByOrganizationGuidAndAttributeName(organization.guid, attribute.name) match {
        case None => Nil
        case Some(found) => {
          Some(found.guid) == existing.map(_.guid) match {
            case true => Nil
            case false => Seq("Value for this attribute already exists")
          }
        }
      }
    }

    Validation.errors(attributeErrors ++ valueErrors)
  }

  def upsert(user: User, organization: Organization, attribute: Attribute, form: AttributeValueForm): AttributeValue = {
    findByOrganizationGuidAndAttributeName(organization.guid, attribute.name) match {
      case None => create(user, organization, attribute, form)
      case Some(existing) => update(user, organization, existing, form)
    }
  }

  def create(user: User, organization: Organization, attribute: Attribute, form: AttributeValueForm): AttributeValue = {
    val errors = validate(organization, AttributeSummary(attribute.guid, attribute.name), form, None)
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    val guid = UUID.randomUUID()

    db.withConnection { implicit c =>
      SQL(InsertQuery).on(
        Symbol("guid") -> guid,
        Symbol("organization_guid") -> organization.guid,
        Symbol("attribute_guid") -> attribute.guid,
        Symbol("value") -> form.value.trim,
        Symbol("user_guid") -> user.guid
      ).execute()
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create attribute_value")
    }
  }

  def update(user: User, organization: Organization, existing: AttributeValue, form: AttributeValueForm): AttributeValue = {
    val errors = validate(organization, existing.attribute, form, Some(existing))
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    db.withConnection { implicit c =>
      SQL(UpdateQuery).on(
        Symbol("guid") -> existing.guid,
        Symbol("value") -> form.value.trim,
        Symbol("user_guid") -> user.guid
      ).execute()
    }

    findByGuid(existing.guid).getOrElse {
      sys.error("Failed to update attribute_value")
    }
  }

  def softDelete(deletedBy: User, org: AttributeValue): Unit = {
    dbHelpers.delete(deletedBy, org.guid)
  }

  def findByGuid(guid: UUID): Option[AttributeValue] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findByOrganizationGuidAndAttributeName(organizationGuid: UUID, name: String): Option[AttributeValue] = {
    findAll(organizationGuid = Some(organizationGuid), attributeNames = Some(Seq(name)), limit = 1).headOption
  }
  
  def findAll(
    guid: Option[UUID] = None,
    organizationGuid: Option[UUID] = None,
    attributeNames: Option[Seq[String]] = None,
    attributeGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[AttributeValue] = {
    db.withConnection { implicit c =>
      BaseQuery.
        equals("organization_attribute_values.guid", guid).
        equals("organization_attribute_values.organization_guid", organizationGuid).
        equals("organization_attribute_values.attribute_guid", attributeGuid).
        optionalIn("attributes.name", attributeNames).
        and(isDeleted.map(Filters.isDeleted("organization_attribute_values", _))).
        orderBy("lower(organization_attribute_values.value), organization_attribute_values.created_at").
        limit(limit).
        offset(offset).
        anormSql().as(
          io.apibuilder.api.v0.anorm.parsers.AttributeValue.parser().*
        )
    }
  }

}
