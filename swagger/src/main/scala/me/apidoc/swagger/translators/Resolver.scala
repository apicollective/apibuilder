package me.apidoc.swagger.translators

import me.apidoc.swagger.Util
import com.bryzek.apidoc.spec.v0.{ models => apidoc }
import com.wordnik.swagger.models.RefModel
import com.wordnik.swagger.models.properties.{ArrayProperty, Property, RefProperty}

case class Resolver(
  models: Seq[apidoc.Model]
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
        val model = models.find(_.name == p.getSimpleRef()).getOrElse {
          sys.error("Cannot find model for reference: " + p.get$ref())
        }
        model.name
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
