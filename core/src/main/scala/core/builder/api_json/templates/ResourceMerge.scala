package builder.api_json.templates

import builder.api_json.{InternalOperationForm, InternalParameterForm, InternalResourceForm, InternalResponseForm, InternalTemplateDeclarationForm}

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
      warnings = union(original.warnings, tpl.warnings)
    )
  }

  private[this] def pathLabel(op: InternalOperationForm): String = {
    op.method.getOrElse("") + ":" + op.path
  }

  def mergeOperations(original: Seq[InternalOperationForm], template: Seq[InternalOperationForm]): Seq[InternalOperationForm] = {
    new ArrayMerge[InternalOperationForm]() {
      override def label(i: InternalOperationForm): String = pathLabel(i)
      override def merge(original: InternalOperationForm, tpl: InternalOperationForm): InternalOperationForm = {
        InternalOperationForm(
          method = original.method,
          path = original.path,
          description = original.description.orElse(tpl.description),
          deprecation = original.deprecation.orElse(tpl.deprecation),
          parameters = mergeParameters(original.parameters, tpl.parameters),
          body = original.body.orElse(tpl.body),
          responses = mergeResponses(original.responses, tpl.responses),
          attributes = mergeAttributes(original.attributes, tpl.attributes),
          warnings = union(original.warnings, tpl.warnings)
        )
      }
    }.merge(original, template)
  }

  private[this] def mergeParameters(original: Seq[InternalParameterForm], template: Seq[InternalParameterForm]): Seq[InternalParameterForm] = {
    new ArrayMerge[InternalParameterForm]() {
      override def label(i: InternalParameterForm): String = i.name.get

      override def merge(original: InternalParameterForm, tpl: InternalParameterForm): InternalParameterForm = {
        println(s"TODO: Merge Parameter:")
        println(s"   - original: $original")
        println(s"   -      tpl: $tpl")
        original
      }
    }.merge(original, template)
  }

  private[this] def mergeResponses(original: Seq[InternalResponseForm], template: Seq[InternalResponseForm]): Seq[InternalResponseForm] = {
    new ArrayMerge[InternalResponseForm]() {
      override def label(i: InternalResponseForm): String = i.code

      override def merge(original: InternalResponseForm, tpl: InternalResponseForm): InternalResponseForm = {
        println(s"TODO: Merge Response:")
        println(s"   - original: $original")
        println(s"   -      tpl: $tpl")
        original
      }
    }.merge(original, template)
  }
}