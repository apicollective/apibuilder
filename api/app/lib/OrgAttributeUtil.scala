package lib

import db.OrganizationAttributeValuesDao
import io.apibuilder.common.v0.models.Reference
import io.apibuilder.generator.v0.models.Attribute

import java.util.UUID
import javax.inject.Inject
import scala.collection.mutable.ListBuffer

class OrgAttributeUtil @Inject() (
  organizationAttributeValuesDao: OrganizationAttributeValuesDao,
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
        val all = ListBuffer[Attribute]()

        Pager.eachPage { offset =>
          organizationAttributeValuesDao.findAll(
            organizationGuid = Some(organizationGuid),
            attributeNames = Some(names),
            limit = 1000,
            offset = offset,
          )
        } { av =>
          all += Attribute(av.attribute.name, av.value)
        }

        all.toSeq
      }
    }
  }
}
