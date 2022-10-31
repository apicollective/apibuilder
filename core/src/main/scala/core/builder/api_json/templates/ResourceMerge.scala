package builder.api_json.templates

import builder.api_json.{InternalOperationForm, InternalResourceForm, InternalTemplateDeclarationForm}

case class ResourceMergeData(resources: Seq[InternalResourceForm])

case class ResourceMerge(templates: Seq[InternalResourceForm]) extends TemplateMerge[InternalResourceForm](templates) with AttributeMerge {

  def merge(data: ResourceMergeData): ResourceMergeData = {
    ResourceMergeData(
      resources = data.resources.map { resource =>
        applyTemplates(resource, allTemplates(resource.templates))
      }
    )
  }

  override def label(resource: InternalResourceForm): String = resource.datatype.label

  override def templateDeclarations(resource: InternalResourceForm): Seq[InternalTemplateDeclarationForm] = {
    resource.templates
  }

  override def applyTemplate(original: InternalResourceForm, tpl: InternalResourceForm): InternalResourceForm = {
    InternalResourceForm(
      datatype = original.datatype,
      description = original.description.orElse(tpl.description),
      deprecation = original.deprecation.orElse(tpl.deprecation),
      path = original.path.orElse(tpl.path),
      operations = mergeOperations(original.operations, tpl.operations),
      attributes = mergeAttributes(original.attributes, tpl.attributes),
      templates = Nil,
      warnings = original.warnings ++ tpl.warnings
    )
  }

  def mergeOperations(original: Seq[InternalOperationForm], template: Seq[InternalOperationForm]): Seq[InternalOperationForm] = {
    println(s"TODO: Merge operations")
    original ++ template
  }
}