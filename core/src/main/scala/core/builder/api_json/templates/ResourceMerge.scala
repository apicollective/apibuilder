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
    val originalByPath = original.map { op => pathLabel(op) -> op }.toMap
    val tplByPath = template.map { op => pathLabel(op) -> op }.toMap
    template.map { tplOp =>
      originalByPath.get(pathLabel(tplOp)) match {
        case None => tplOp
        case Some(op) => mergeOperation(op, tplOp)
      }
    } ++ original.filterNot { op => tplByPath.contains(pathLabel(op)) }
  }

  private[this] def mergeOperation(original: InternalOperationForm, tpl: InternalOperationForm): InternalOperationForm = {
    InternalOperationForm(
      method = original.method,
      path = original.path,
      description = original.description.orElse(tpl.description),
      deprecation = original.deprecation.orElse(tpl.deprecation),
      namedPathParameters = union(original.namedPathParameters, tpl.namedPathParameters),
      parameters = mergeParameters(original.parameters, tpl.parameters),
      body = original.body.orElse(tpl.body),
      responses = mergeResponses(original.responses, tpl.responses),
      attributes = mergeAttributes(original.attributes, tpl.attributes),
      warnings = union(original.warnings, tpl.warnings)
    )
  }
  private[this] def mergeParameters(original: Seq[InternalParameterForm], tpl: Seq[InternalParameterForm]): Seq[InternalParameterForm] = {
    println(s"TODO: Merge Parameters")
    original ++ tpl
  }

  private[this] def mergeResponses(original: Seq[InternalResponseForm], tpl: Seq[InternalResponseForm]): Seq[InternalResponseForm] = {
    println(s"TODO: Merge Responses")
    original ++ tpl
  }
}