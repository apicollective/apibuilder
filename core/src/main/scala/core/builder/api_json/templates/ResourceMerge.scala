package core.builder.api_json.templates

import builder.api_json.InternalResourceForm

case class ResourceMerge(templates: Seq[InternalResourceForm]) {
  private[this] val templatesByLabel: Map[String, InternalResourceForm] = templates.map { t =>
    t.datatype.label -> t
  }.toMap

  def merge(resources: Seq[InternalResourceForm]): Seq[InternalResourceForm] = {
    resources.map { resource =>
      templatesByLabel.get(resource.datatype.label) match {
        case None => resource
        case Some(tpl) => {
          println(s"Found template for resource named ${tpl.datatype.label}")
          resource
        }
      }
    }
  }

}