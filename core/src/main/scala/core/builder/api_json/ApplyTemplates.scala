package builder.api_json

import io.apibuilder.spec.v0.models.Service

case class ApplyTemplates(templates: InternalTemplateForm) {

  def apply(service: Service): Service = {
    println(s"ApplyTemplates for service: ${templates}")
    resources(models(service))
  }

  private[this] def models(service: Service): Service = {
    templates.models.foldLeft(service) { case (s, tpl) =>
      println(s"Applying model template: ${tpl.name}")
      s
    }
  }

  private[this] def resources(service: Service): Service = {
    templates.resources.foldLeft(service) { case (s, tpl) =>
      println(s"Applying resource template: ${tpl.datatype.label}")
      s
    }
  }
}