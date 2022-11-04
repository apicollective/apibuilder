package builder.api_json.templates

import builder.api_json.{InternalOperationForm, InternalParameterForm, InternalResourceForm, InternalResponseForm, InternalTemplateDeclarationForm}

case class ResourceMergeData(resources: Seq[InternalResourceForm])

case class ResourceMerge(templates: Seq[InternalResourceForm]) extends TemplateMerge[InternalResourceForm](templates) with HeaderMerge {

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
    println(s"Resource merge. original type: ${original.datatype.label} / tpl: ${tpl.datatype.label}")
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

  private[this] def mergeOperations(original: Seq[InternalOperationForm], template: Seq[InternalOperationForm]): Seq[InternalOperationForm] = {
    new ArrayMerge[InternalOperationForm]() {
      override def uniqueIdentifier(i: InternalOperationForm): String = pathLabel(i)
      override def merge(original: InternalOperationForm, tpl: InternalOperationForm): InternalOperationForm = {
        InternalOperationForm(
          method = original.method,
          path = original.path,
          description = original.description.orElse(tpl.description),
          deprecation = original.deprecation.orElse(tpl.deprecation),
          parameters = mergeParameters(original.parameters, tpl.parameters),
          body = original.body.orElse(tpl.body),
          declaredResponses = mergeResponses(original.declaredResponses, tpl.declaredResponses),
          attributes = mergeAttributes(original.attributes, tpl.attributes),
          warnings = union(original.warnings, tpl.warnings)
        )
      }
    }.merge(original, template)
  }

  private[this] def mergeParameters(original: Seq[InternalParameterForm], template: Seq[InternalParameterForm]): Seq[InternalParameterForm] = {
    new ArrayMerge[InternalParameterForm]() {
      override def uniqueIdentifier(i: InternalParameterForm): String = i.name.get

      override def merge(original: InternalParameterForm, tpl: InternalParameterForm): InternalParameterForm = {
        InternalParameterForm(
          name = original.name,
          datatype = original.datatype,
          location = original.location.orElse(tpl.location),
          description = original.description.orElse(tpl.description),
          deprecation = original.deprecation.orElse(tpl.deprecation),
          required = original.required,
          default = original.default.orElse(tpl.default),
          minimum = original.minimum.orElse(tpl.minimum),
          maximum = original.maximum.orElse(tpl.maximum),
          example = original.example.orElse(tpl.example),
          attributes = mergeAttributes(original.attributes, tpl.attributes),
          warnings = original.warnings ++ tpl.warnings
        )
      }
    }.merge(original, template)
  }

  private[this] def mergeResponses(original: Seq[InternalResponseForm], template: Seq[InternalResponseForm]): Seq[InternalResponseForm] = {
    new ArrayMerge[InternalResponseForm]() {
      override def uniqueIdentifier(i: InternalResponseForm): String = i.code

      override def merge(original: InternalResponseForm, tpl: InternalResponseForm): InternalResponseForm = {
        InternalResponseForm(
          code = original.code,
          datatype = original.datatype,
          headers = mergeHeaders(original.headers, tpl.headers),
          description = original.description.orElse(tpl.description),
          deprecation = original.deprecation.orElse(tpl.deprecation),
          attributes = mergeAttributes(original.attributes, tpl.attributes),
          warnings = original.warnings ++ tpl.warnings
        )
      }
    }.merge(original, template)
  }
}