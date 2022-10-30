package builder.api_json.templates

import builder.api_json.{InternalAttributeForm, InternalFieldForm, InternalModelForm, InternalTemplateDeclarationForm}
import play.api.libs.json.Json

import scala.annotation.tailrec

case class ModelMerge(templates: Seq[InternalModelForm]) {
  private[this] def format(value: String): String = value.toLowerCase().trim

  private[this] val templatesByName: Map[String, InternalModelForm] = templates.map { t =>
    format(t.name) -> t
  }.toMap

  def merge(models: Seq[InternalModelForm]): Seq[InternalModelForm] = {
    models.map { model =>
      applyTemplates(model, allTemplates(model))
    }
  }

  private[this] def allTemplates(form: InternalModelForm): Seq[InternalTemplateDeclarationForm] = {
    form.templates.flatMap { tpl =>
      templatesByName.get(tpl.name.get) match {
        case None => Nil
        case Some(model) => Seq(tpl) ++ model.templates
      }
    }
  }

  @tailrec
  private[this] def applyTemplates(model: InternalModelForm, remaining: Seq[InternalTemplateDeclarationForm]): InternalModelForm = {
    remaining.toList match {
      case Nil => model
      case one :: rest => {
        applyTemplates(
          applyTemplate(model, one),
          rest
        )
      }
    }
  }

  private[this] def applyTemplate(model: InternalModelForm, declaration: InternalTemplateDeclarationForm): InternalModelForm = {
    val tpl = templatesByName.getOrElse(declaration.name.get, sys.error(s"Cannot find template named '${declaration.name}'"))
    InternalModelForm(
      name = model.name,
      plural = model.plural,
      description = model.description.orElse(tpl.description),
      deprecation = model.deprecation.orElse(tpl.deprecation),
      fields = mergeFields(model, tpl),
      attributes = mergeAttributes(model.attributes, tpl.attributes),
      templates = mergeTemplates(model.templates, tpl.templates),
      interfaces = union(model.interfaces, tpl.interfaces),
      warnings = model.warnings ++ tpl.warnings
    )
  }

  private[this] def union(model: Seq[String], tpl: Seq[String]): Seq[String] = {
    (tpl ++ model).distinct
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
  
  def mergeAttributes(model: Seq[InternalAttributeForm], tpl: Seq[InternalAttributeForm]): Seq[InternalAttributeForm] = {
    val modelAttributesByName = model.map { f => f.name -> f }.toMap
    val tplAttributeNames = tpl.flatMap(_.name).toSet
    tpl.map { tplAttr =>
      modelAttributesByName.get(tplAttr.name) match {
        case None => tplAttr
        case Some(a) => mergeAttribute(a, tplAttr)
      }
    } ++ model.filterNot { a => tplAttributeNames.contains(a.name.get) }
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


  private[this] def mergeAttribute(model: InternalAttributeForm, tpl: InternalAttributeForm): InternalAttributeForm = {
    InternalAttributeForm(
      name = model.name,
      value = Some(tpl.value.getOrElse(Json.obj()) ++ model.value.getOrElse(Json.obj())),
      description = model.description.orElse(tpl.description),
      deprecation = model.deprecation.orElse(tpl.deprecation)
    )
  }

  private[this] def mergeTemplate(model: InternalTemplateDeclarationForm, tpl: InternalTemplateDeclarationForm): InternalTemplateDeclarationForm = {
    InternalTemplateDeclarationForm(
      name = model.name,
      warnings = model.warnings ++ tpl.warnings
    )
  }

}