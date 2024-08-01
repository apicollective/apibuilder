package models

import db.{Authorization, InternalAttribute, InternalAttributesDao}
import io.apibuilder.api.v0.models.Attribute
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}

import java.util.UUID
import javax.inject.Inject

class AttributesModel @Inject()(
  attributesDao: InternalAttributesDao,
) {
  def toModel(v: InternalAttribute): Attribute = {
    toModels(Seq(v)).head
  }

  def toModels(attributes: Seq[InternalAttribute]): Seq[Attribute] = {
    attributes.map { attr =>
      Attribute(
        guid = attr.guid,
        name = attr.name,
        description = attr.description,
        audit = Audit(
          createdAt = attr.db.createdAt,
          createdBy = ReferenceGuid(attr.db.createdByGuid),
          updatedAt = attr.db.updatedAt,
          updatedBy = ReferenceGuid(attr.db.updatedByGuid),
        )
      )
    }
  }
}