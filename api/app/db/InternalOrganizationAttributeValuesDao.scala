package db

import cats.implicits.*
import cats.data.ValidatedNec
import db.generated.OrganizationAttributeValuesDao
import io.apibuilder.api.v0.models.*
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}
import io.flow.postgresql.Query
import lib.Validation
import play.api.db.*
import util.OptionalQueryFilter

import java.util.UUID
import javax.inject.{Inject, Singleton}

case class InternalOrganizationAttributeValue(db: generated. OrganizationAttributeValue) {
  val guid: UUID = db.guid
}

case class ValidatedOrganizationAttributeValue(
  value: String,
)

class InternalOrganizationAttributeValuesDao @Inject()(
  dao: OrganizationAttributeValuesDao,
  attributesDao: InternalAttributesDao
) {

  private def validateValue(
    org: InternalOrganization,
    attributeGuid: UUID,
    value: String,
    existing: Option[InternalOrganizationAttributeValue]
  ): ValidatedNec[Error, String] = {
    val trimmed = value.trim
    if (trimmed.isEmpty) {
      Validation.singleError("Value is required").invalidNec
    } else {
      findByOrganizationGuidAndAttributeGuid(org.guid, attributeGuid) match {
        case None => trimmed.validNec
        case Some(found) if existing.map(_.guid).contains(found.guid) => trimmed.validNec
        case Some(_) => Validation.singleError("Value for this attribute already exists").invalidNec
      }
    }
  }

  private def validate(
                        org: InternalOrganization,
                        attributeGuid: UUID,
                        form: AttributeValueForm,
                        existing: Option[InternalOrganizationAttributeValue]
                      ): ValidatedNec[io.apibuilder.api.v0.models.Error, ValidatedOrganizationAttributeValue] = {
    validateValue(org, attributeGuid, form.value, existing).map { vValue =>
      ValidatedOrganizationAttributeValue(value = vValue)
    }
  }

  def upsert(user: InternalUser, org: InternalOrganization, attribute: InternalAttribute, form: AttributeValueForm): ValidatedNec[Error, InternalOrganizationAttributeValue] = {
    findByOrganizationGuidAndAttributeGuid(org.guid, attribute.guid) match {
      case None => create(user, org, attribute, form)
      case Some(existing) => update(user, org, existing, form)
    }
  }

  def create(user: InternalUser, organization: InternalOrganization, attribute: InternalAttribute, form: AttributeValueForm): ValidatedNec[Error, InternalOrganizationAttributeValue] = {
    validate(organization, attribute.guid, form, existing = None).map { vForm =>
      val guid = dao.insert(user.guid, generated.OrganizationAttributeValueForm(
        organizationGuid = organization.guid,
        attributeGuid = attribute.guid,
        value = vForm.value
      ))

      findByGuid(guid).getOrElse {
        sys.error("Failed to create attribute_value")
      }
    }
  }

  def update(user: InternalUser, organization: InternalOrganization, existing: InternalOrganizationAttributeValue, form: AttributeValueForm): ValidatedNec[Error, InternalOrganizationAttributeValue] = {
    validate(organization, existing.db.attributeGuid, form, existing = Some(existing)).map { vForm =>
      dao.update(user.guid, existing.db, existing.db.form.copy(
        value = vForm.value
      ))

      findByGuid(existing.guid).getOrElse {
        sys.error("Failed to update attribute_value")
      }
    }
  }

  def softDelete(deletedBy: InternalUser, value: InternalOrganizationAttributeValue): Unit = {
    dao.delete(deletedBy.guid, value.db)
  }

  def findByGuid(guid: UUID): Option[InternalOrganizationAttributeValue] = {
    findAll(guid = Some(guid), limit = Some(1)).headOption
  }

  def findByOrganizationGuidAndAttributeName(organizationGuid: UUID, attributeName: String): Option[InternalOrganizationAttributeValue] = {
    findAll(organizationGuid = Some(organizationGuid), attributeName = Some(attributeName), limit = Some(1)).headOption
  }

  private def findByOrganizationGuidAndAttributeGuid(organizationGuid: UUID, attributeGuid: UUID): Option[InternalOrganizationAttributeValue] = {
    findAll(organizationGuid = Some(organizationGuid), attributeGuid = Some(attributeGuid), limit = Some(1)).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    organizationGuid: Option[UUID] = None,
    attributeName: Option[String] = None,
    attributeNames: Option[Seq[String]] = None,
    attributeGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Option[Long],
    offset: Long = 0
  ): Seq[InternalOrganizationAttributeValue] = {
    val filters = List(
      new OptionalQueryFilter(attributeName) {
        override def filter(q: Query, name: String): Query = {
          q.in("attribute_guid", Query("select guid from attributes").equals("name", name))
        }
      },
      new OptionalQueryFilter(attributeNames) {
        override def filter(q: Query, names: Seq[String]): Query = {
          q.in("attribute_guid", Query("select guid from attributes").in("name", names))
        }
      },
    )

    dao.findAll(
      guid = guid,
      organizationGuid = organizationGuid,
      attributeGuid = attributeGuid,
      limit = limit,
      offset = offset,
    )( using (q: Query) => {
      filters.foldLeft(q) { case (q, f) => f.filter(q) }
        .and(isDeleted.map(Filters.isDeleted("organization_attribute_values", _)))
    }).map(InternalOrganizationAttributeValue(_))
  }
}
