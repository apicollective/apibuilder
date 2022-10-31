package builder.api_json.templates

import builder.api_json.{InternalFieldForm, InternalInterfaceForm, InternalModelForm, InternalTemplateDeclarationForm}

import scala.annotation.tailrec

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

  override def applyTemplate(model: InternalModelForm, tpl: InternalModelForm): InternalModelForm = {
    val templates = mergeTemplates(model.templates, tpl.templates)
    InternalModelForm(
      name = model.name,
      plural = model.plural,
      description = model.description.orElse(tpl.description),
      deprecation = model.deprecation.orElse(tpl.deprecation),
      fields = mergeFields(model, tpl),
      attributes = mergeAttributes(model.attributes, tpl.attributes),
      templates = Nil,
      interfaces = union(model.interfaces, tpl.interfaces, templates.flatMap(_.name)),
      warnings = model.warnings ++ tpl.warnings
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

  @tailrec
  private[this] def union(first: Seq[String], remaining: Seq[String]*): Seq[String] = {
    remaining.toList match {
      case Nil => first
      case one :: rest => union((first ++ one).distinct, rest: _*)
    }
  }

  def mergeTemplates(model: Seq[InternalTemplateDeclarationForm], tpl: Seq[InternalTemplateDeclarationForm]): Seq[InternalTemplateDeclarationForm] = {
    val modelTemplatesByName = model.map { f => f.name -> f }.toMap
    val tplTemplateNames = tpl.flatMap(_.name).toSet
    tpl.map { t =>
      modelTemplatesByName.get(t.name) match {
        case None => t
        case Some(a) => mergeTemplate(a, t)
      }
    } ++ model.filterNot { a => tplTemplateNames.contains(a.name.get) }
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

  private[this] def mergeTemplate(model: InternalTemplateDeclarationForm, tpl: InternalTemplateDeclarationForm): InternalTemplateDeclarationForm = {
    InternalTemplateDeclarationForm(
      name = model.name,
      warnings = model.warnings ++ tpl.warnings
    )
  }

}