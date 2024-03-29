package io.apibuilder.swagger.translators

import io.apibuilder.swagger.{SchemaType, Util}
import io.apibuilder.spec.v0.{ models => apidoc }
import io.swagger.models.RefModel
import io.swagger.models.properties.{ArrayProperty, Property, RefProperty}

case class Resolver(
  models: Seq[apidoc.Model], enums: Seq[apidoc.Enum]
) {

  def resolve(
    rm: RefModel
  ): Option[apidoc.Model] = {
    // Lookup reference. need to make method iterative
    val name = rm.getSimpleRef
    models.find(_.name == name)
  }

  def resolveWithError(
    rm: RefModel
  ): apidoc.Model = {
    resolve(rm).getOrElse {
      sys.error(s"Failed to find a model with name[${rm.getSimpleRef}]")
    }
  }

  def findModelByUrl(
    url: String
  ): Option[apidoc.Model] = {
    val normalized = Util.normalizeUrl(url)
    models.find { m =>
      val modelUrl = Util.normalizeUrl(s"/${m.plural}")
      normalized == modelUrl || normalized.startsWith(modelUrl + "/")
    }
  }

  def findModelByOkResponseSchema(
    model: String
  ): Option[apidoc.Model] = {
    models.find (_.name == model)
  }

  def schemaType(
    prop: Property
  ): String = {
    prop match {
      case p: ArrayProperty => {
        val schema = schemaType(p.getItems)
        val isUnique = Option(p.getUniqueItems) // TODO
        s"[$schema]"
      }
      case p: RefProperty => {
        models.find(_.name == p.getSimpleRef()).map(_.name)
          .getOrElse(enums.find(_.name == p.getSimpleRef()).map(_.name)
            .getOrElse(p.getSimpleRef)) //we don't error out here to support recursive model defs: the error will be thrown down the line if the ref does not exist
      }
      case _ => {
        if (prop.getType == null) {
          sys.error(s"Property[${prop}] has no type")
        }
        SchemaType.fromSwaggerWithError(prop.getType, Option(prop.getFormat))
      }
    }
  }


}
