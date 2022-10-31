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
      override def label(i: InternalTemplateDeclarationForm): String = i.name.get

      override def merge(original: InternalTemplateDeclarationForm, tpl: InternalTemplateDeclarationForm): InternalTemplateDeclarationForm = {
        InternalTemplateDeclarationForm(
          name = original.name,
          warnings = union(original.warnings, tpl.warnings)
        )
      }
    }.merge(original, template)
  }

  private[this] def mergeFields(model: InternalModelForm, tpl: InternalModelForm): Seq[InternalFieldForm] = {
    val modelFieldsByName = model.fields.map { f => f.name -> f }.toMap
    val tplFieldNames = tpl.fields.flatMap(_.name).toSet
    tpl.fields.map { tplField =>
      modelFieldsByName.get(tplField.name) match {
        case None => tplField
        case Some(f) => mergeField(f, tplField)
      }
    } ++ model.fields.filterNot { f => tplFieldNames.contains(f.name.get) }
  }

  private[this] def mergeField(model: InternalFieldForm, tpl: InternalFieldForm): InternalFieldForm = {
    InternalFieldForm(
      name = model.name,
      datatype = model.datatype,
      description = model.description.orElse(tpl.description),
      deprecation = model.deprecation.orElse(tpl.deprecation),
      default = model.default.orElse(tpl.default),
      example = model.example.orElse(tpl.example),
      minimum = model.minimum.orElse(tpl.minimum),
      maximum = model.maximum.orElse(tpl.maximum),
      attributes = mergeAttributes(model.attributes, tpl.attributes),
      annotations = union(model.annotations, tpl.annotations)
    )
  }

}