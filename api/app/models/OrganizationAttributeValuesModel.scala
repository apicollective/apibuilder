package models

import db.{Authorization, InternalAttributesDao, InternalOrganizationAttributeValue, InternalOrganizationAttributeValuesDao}
import io.apibuilder.api.v0.models.{AttributeSummary, AttributeValue}
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}

import java.util.UUID
import javax.inject.Inject

class OrganizationAttributeValuesModel @Inject()(
  attributesDao: InternalAttributesDao
) {

  def toModel(v: InternalOrganizationAttributeValue): AttributeValue = {
    toModels(Seq(v)).head
  }

  def toModels(values: Seq[InternalOrganizationAttributeValue]): Seq[AttributeValue] = {
    val attributes = attributesDao.findAll(
      guids = Some(values.map(_.db.attributeGuid).distinct),
      limit = None
    ).map { a => a.guid -> a }.toMap

    values.flatMap { v =>
      attributes.get(v.db.attributeGuid).map { attr =>
        AttributeValue(
          guid = v.guid,
          attribute = AttributeSummary(guid = attr.guid, name = attr.name),
          value = v.db.value,
          audit = Audit(
            createdAt = v.db.createdAt,
            createdBy = ReferenceGuid(v.db.createdByGuid),
            updatedAt = v.db.updatedAt,
            updatedBy = ReferenceGuid(v.db.updatedByGuid),
          )
        )
      }
    }
  }
}