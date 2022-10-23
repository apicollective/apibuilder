package core.builder.api_json.templates

import builder.api_json.{InternalAttributeForm, InternalFieldForm, InternalModelForm}

case class ModelMerge(templates: Seq[InternalModelForm]) {
  private[this] val templatesByName: Map[String, InternalModelForm] = templates.map { t =>
    t.name.toLowerCase().trim -> t
  }.toMap

  def merge(models: Seq[InternalModelForm]): Seq[InternalModelForm] = {
    models.map { model =>
      templatesByName.get(model.name.toLowerCase().trim) match {
        case None => model
        case Some(tpl) => {
          println(s"Found template form model named ${model.name}")
          InternalModelForm(
            name = model.name,
            plural = model.plural,
            description = model.description.orElse(tpl.description),
            deprecation = model.deprecation.orElse(tpl.deprecation),
            fields = mergeFields(model, tpl),
            attributes = mergeAttributes(model, tpl),
            interfaces = mergeInterfaces(model, tpl),
            warnings = Nil
          )
        }
      }
    }
  }

  private[this] def mergeInterfaces(model: InternalModelForm, tpl: InternalModelForm): Seq[String] = {
    (tpl.interfaces ++ model.interfaces).distinct
  }

  def mergeAttributes(model: InternalModelForm, tpl: InternalModelForm): Seq[InternalAttributeForm] = {
    val modelAttributesByName = model.attributes.map { f => f.name -> f }.toMap
    tpl.attributes.map { tplAttr =>
      modelAttributesByName.get(tplAttr.name) match {
        case None => tplAttr
        case Some(a) => mergeAttribute(a, tplAttr)
      }
    }

  }

  private[this] def mergeFields(model: InternalModelForm, tpl: InternalModelForm): Seq[InternalFieldForm] = {
    val modelFieldsByName = model.fields.map { f => f.name -> f }.toMap
    tpl.fields.map { tplField =>
      modelFieldsByName.get(tplField.name) match {
        case None => tplField
        case Some(f) => mergeField(f, tplField)
      }
    }
  }

  private[this] def mergeField(modelField: InternalFieldForm, tpl: InternalFieldForm): InternalFieldForm = {
    println(s"TODO: MErge field: ${modelField.name} / ${tpl.name}")
    modelField
  }


  private[this] def mergeAttribute(modelAttribute: InternalAttributeForm, tpl: InternalAttributeForm): InternalAttributeForm = {
    println(s"TODO: MErge Attribute: ${modelAttribute.name} / ${tpl.name}")
    modelAttribute
  }

}