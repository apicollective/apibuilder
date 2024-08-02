package lib

import db.{InternalAttributesDao, InternalOrganizationAttributeValuesDao}
import io.apibuilder.common.v0.models.Reference
import io.apibuilder.generator.v0.models.Attribute
import models.OrganizationAttributeValuesModel

import java.util.UUID
import javax.inject.Inject

class OrgAttributeUtil @Inject() (
  organizationAttributeValuesDao: InternalOrganizationAttributeValuesDao,
  attributesDao: InternalAttributesDao,
  model: OrganizationAttributeValuesModel,
) {

  def merge(
    org: Reference,
    attributeNames: Seq[String],
    attributes: Seq[Attribute],
  ): Seq[Attribute] = {
    attributes ++ getAllAttributes(org.guid, attributeNames).filterNot { a =>
      attributes.map(_.name).contains(a.name)
    }
  }

  /**
   * Fetch all attribute values specified for this organization,
   * filtered by those matching names.
   */
  private def getAllAttributes(organizationGuid: UUID, names: Seq[String]): Seq[Attribute] = {
    names match {
      case Nil => Nil
      case _ => {
        model.toModels(organizationAttributeValuesDao.findAll(
          organizationGuid = Some(organizationGuid),
          attributeNames = Some(names),
          limit = None,
        )).map { v =>
          Attribute(v.attribute.name, v.value)
        }
      }
    }
  }
}
