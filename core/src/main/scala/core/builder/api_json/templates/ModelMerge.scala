package builder.api_json.templates

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
          InternalModelForm(
            name = model.name,
            plural = model.plural,
            description = model.description.orElse(tpl.description),
            deprecation = model.deprecation.orElse(tpl.deprecation),
            fields = mergeFields(model, tpl),
            attributes = mergeAttributes(model.attributes, tpl.attributes),
            interfaces = union(model.interfaces, tpl.interfaces),
            warnings = model.warnings ++ tpl.warnings
          )
        }
      }
    }
  }

  private[this] def union(model: Seq[String], tpl: Seq[String]): Seq[String] = {
    (tpl ++ model).distinct
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
      value = model.value.orElse(tpl.value),
      description = model.description.orElse(tpl.description),
      deprecation = model.deprecation.orElse(tpl.deprecation)
    )
  }

}