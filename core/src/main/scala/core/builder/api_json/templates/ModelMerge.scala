package builder.api_json.templates

import builder.api_json.{InternalFieldForm, InternalInterfaceForm, InternalModelForm, InternalTemplateDeclarationForm}

case class ModelMergeData(
  interfaces: Seq[InternalInterfaceForm],
  models: Seq[InternalModelForm]
)

case class ModelMerge(templates: Seq[InternalModelForm]) extends TemplateMerge[InternalModelForm](templates) with AttributeMerge {

  def merge(data: ModelMergeData): ModelMergeData = {
    ModelMergeData(
      models = data.models.map { model =>
        applyTemplates(model, allTemplates(model.templates))
      },
      interfaces = data.interfaces ++ buildInterfaces(data.interfaces)
    )
  }

  override def label(model: InternalModelForm): String = model.name

  override def templateDeclarations(model: InternalModelForm): Seq[InternalTemplateDeclarationForm] = {
    model.templates
  }

  override def applyTemplate(original: InternalModelForm, tpl: InternalModelForm): InternalModelForm = {
    val templates = mergeTemplates(original.templates, tpl.templates)
    InternalModelForm(
      name = original.name,
      plural = original.plural,
      description = original.description.orElse(tpl.description),
      deprecation = original.deprecation.orElse(tpl.deprecation),
      fields = mergeFields(original, tpl),
      attributes = mergeAttributes(original.attributes, tpl.attributes),
      templates = Nil,
      interfaces = union(original.interfaces, tpl.interfaces, templates.flatMap(_.name)),
      warnings = union(original.warnings, tpl.warnings)
    )
  }

  private[this] def buildInterfaces(defined: Seq[InternalInterfaceForm]): Seq[InternalInterfaceForm] = {
    val definedByName = defined.map(_.name).toSet
    templates.filterNot { t => definedByName.contains(t.name) }.map { t =>
      InternalInterfaceForm(
        name = t.name,
        plural = t.plural,
        description = t.description,
        deprecation = t.deprecation,
        fields = t.fields,
        attributes = t.attributes,
        warnings = Nil
      )
    }
  }

  def mergeTemplates(original: Seq[InternalTemplateDeclarationForm], template: Seq[InternalTemplateDeclarationForm]): Seq[InternalTemplateDeclarationForm] = {
    new ArrayMerge[InternalTemplateDeclarationForm]() {
      override def uniqueIdentifier(i: InternalTemplateDeclarationForm): String = i.name.get

      override def merge(original: InternalTemplateDeclarationForm, tpl: InternalTemplateDeclarationForm): InternalTemplateDeclarationForm = {
        InternalTemplateDeclarationForm(
          name = original.name,
          warnings = union(original.warnings, tpl.warnings)
        )
      }
    }.merge(original, template)
  }

  private[this] def mergeFields(model: InternalModelForm, tpl: InternalModelForm): Seq[InternalFieldForm] = {
    new ArrayMerge[InternalFieldForm] {
      override def uniqueIdentifier(f: InternalFieldForm): String = f.name.get

      override def merge(original: InternalFieldForm, tpl: InternalFieldForm): InternalFieldForm = {
        InternalFieldForm(
          name = original.name,
          datatype = original.datatype,
          description = original.description.orElse(tpl.description),
          deprecation = original.deprecation.orElse(tpl.deprecation),
          default = original.default.orElse(tpl.default),
          example = original.example.orElse(tpl.example),
          minimum = original.minimum.orElse(tpl.minimum),
          maximum = original.maximum.orElse(tpl.maximum),
          attributes = mergeAttributes(original.attributes, tpl.attributes),
          annotations = union(original.annotations, tpl.annotations)
        )
      }
    }.merge(model.fields, tpl.fields)
  }

}