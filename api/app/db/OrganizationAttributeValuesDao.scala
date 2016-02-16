package db

import com.bryzek.apidoc.api.v0.models.{AttributeSummary, AttributeValue, AttributeValueForm, Organization, User}
import com.bryzek.apidoc.common.v0.models.Audit
import lib.Validation
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object OrganizationAttributeValuesDao {

  private[db] val BaseQuery = s"""
    select organization_attribute_values.guid,
           organization_attribute_values.value,
           ${AuditsDao.query("organization_attribute_values")},
           attributes.guid as attribute_guid,
           attributes.name as attribute_name
      from organization_attribute_values
      join attributes on attributes.guid = organization_attribute_values.attribute_guid
     where true
  """

  private[this] val InsertQuery = """
    insert into organization_attribute_values
    (guid, organization_guid, attribute_guid, value, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {attribute_guid}::uuid, {value}, {user_guid}::uuid, {user_guid}::uuid)
  """

  def validate(
    organization: Organization,
    form: AttributeValueForm
  ): Seq[com.bryzek.apidoc.api.v0.models.Error] = {

    val attributeErrors = AttributesDao.findByGuid(form.attributeGuid) match {
      case None => Seq("Attribute not found")
      case Some(_) => Nil
    }

    val valueErrors = if (form.value.trim.isEmpty) {
      Seq(s"Value is required")
    } else {
      OrganizationAttributeValuesDao.findByOrganizationGuidAndAttributeGuid(organization.guid, form.attributeGuid) match {
        case None => Seq.empty
        case Some(_) => {
          Seq("Value for this attribute already exists")
        }
      }
    }

    Validation.errors(attributeErrors ++ valueErrors)
  }

  def create(user: User, organization: Organization, form: AttributeValueForm): AttributeValue = {
    val errors = validate(organization, form)
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    val guid = UUID.randomUUID()

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'organization_guid -> organization.guid,
        'attribute_guid -> form.attributeGuid,
        'value -> form.value.trim,
        'user_guid -> user.guid
      ).execute()
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create attribute_value")
    }
  }

  def softDelete(deletedBy: User, org: AttributeValue) {
    SoftDelete.delete("organization_attribute_values", deletedBy, org.guid)
  }

  def findByGuid(guid: UUID): Option[AttributeValue] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findByOrganizationGuidAndAttributeGuid(organizationGuid: UUID, attributeGuid: UUID): Option[AttributeValue] = {
    findAll(organizationGuid = Some(organizationGuid), attributeGuid = Some(attributeGuid), limit = 1).headOption
  }

  def findByOrganizationGuidAndAttributeName(organizationGuid: UUID, name: String): Option[AttributeValue] = {
    findAll(organizationGuid = Some(organizationGuid), attributeName = Some(name), limit = 1).headOption
  }
  
  def findAll(
    guid: Option[UUID] = None,
    organizationGuid: Option[UUID] = None,
    attributeName: Option[String] = None,
    attributeGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[AttributeValue] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and organization_attribute_values.guid = {guid}::uuid" },
      organizationGuid.map { v => "and organization_attribute_values.organization_guid = {organization_guid}::uuid" },
      attributeName.map { v => "and organization_attribute_values.attribute_guid = (select guid from attributes where deleted_at is null and name = lower(trim({attribute_name})))" },
      attributeGuid.map { v => "and organization_attribute_values.attribute_guid = {attribute_guid}::uuid" },
      isDeleted.map(Filters.isDeleted("organization_attribute_values", _)),
      Some(s"order by lower(organization_attribute_values.value), organization_attribute_values.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      organizationGuid.map('organization_guid -> _.toString),
      attributeName.map('attribute_name -> _),
      attributeGuid.map('attribute_guid -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row,
    prefix: Option[String] = None
  ): AttributeValue = {
    val p = prefix.map( _ + "_").getOrElse("")

    AttributeValue(
      guid = row[UUID](s"${p}guid"),
      attribute = AttributeSummary(
        guid = row[UUID](s"${p}attribute_guid"),
        name = row[String](s"${p}attribute_name")
      ),
      value = row[String](s"${p}value"),
      audit = AuditsDao.fromRow(row, prefix)
    )
  }

}
