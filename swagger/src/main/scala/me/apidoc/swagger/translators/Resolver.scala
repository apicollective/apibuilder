package me.apidoc.swagger.translators

import com.gilt.apidoc.spec.v0.{ models => apidoc }
import com.wordnik.swagger.models.RefModel

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

}
