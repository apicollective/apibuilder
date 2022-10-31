package builder.api_json.templates

import builder.api_json.InternalAttributeForm
import play.api.libs.json.Json

trait AttributeMerge {
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

  private[this] def mergeAttribute(model: InternalAttributeForm, tpl: InternalAttributeForm): InternalAttributeForm = {
    InternalAttributeForm(
      name = model.name,
      value = Some(tpl.value.getOrElse(Json.obj()) ++ model.value.getOrElse(Json.obj())),
      description = model.description.orElse(tpl.description),
      deprecation = model.deprecation.orElse(tpl.deprecation)
    )
  }
}