package builder.api_json.templates

import builder.api_json.{InternalResourceForm, InternalTemplateDeclarationForm}

import scala.annotation.tailrec

case class ResourceMergeData(resources: Seq[InternalResourceForm])

case class ResourceMerge(templates: Seq[InternalResourceForm]) {
  private[this] def format(value: String): String = value.toLowerCase().trim

  private[this] val templatesByName: Map[String, InternalResourceForm] = templates.map { t =>
    format(t.datatype.label) -> t
  }.toMap

  def merge(data: ResourceMergeData): ResourceMergeData = {
    ResourceMergeData(
      resources = data.resources.map { resource =>
        applyTemplates(resource, allTemplates(resource.templates))
      }
    )
  }

  private[this] def allTemplates(templates: Seq[InternalTemplateDeclarationForm]): Seq[InternalTemplateDeclarationForm] = {
    templates.flatMap { tpl =>
      templatesByName.get(tpl.name.get) match {
        case None => Nil
        case Some(resource) => Seq(tpl) ++ allTemplates(resource.templates)
      }
    }
  }

  @tailrec
  private[this] def applyTemplates(resource: InternalResourceForm, remaining: Seq[InternalTemplateDeclarationForm]): InternalResourceForm = {
    remaining.toList match {
      case Nil => resource
      case one :: rest => {
        applyTemplates(
          applyTemplate(resource, one),
          rest
        )
      }
    }
  }

  private[this] def applyTemplate(model: InternalResourceForm, declaration: InternalTemplateDeclarationForm): InternalResourceForm = {
    val tpl = templatesByName.getOrElse(declaration.name.get, sys.error(s"Cannot find template named '${declaration.name}'"))
    sys.error(s"TODO: Apply Resource Template: ${tpl.datatype.label}")
  }

}