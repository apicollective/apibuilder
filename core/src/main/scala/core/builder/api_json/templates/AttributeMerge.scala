package builder.api_json.templates

import builder.api_json.InternalAttributeForm
import play.api.libs.json.Json

trait AttributeMerge {
  def mergeAttributes(original: Seq[InternalAttributeForm], tpl: Seq[InternalAttributeForm]): Seq[InternalAttributeForm] = {
    val modelAttributesByName = original.map { f => f.name -> f }.toMap
    val tplAttributeNames = tpl.flatMap(_.name).toSet
    tpl.map { tplAttr =>
      modelAttributesByName.get(tplAttr.name) match {
        case None => tplAttr
        case Some(a) => mergeAttribute(a, tplAttr)
      }
    } ++ original.filterNot { a => tplAttributeNames.contains(a.name.get) }
  }

  private[this] def mergeAttribute(original: InternalAttributeForm, tpl: InternalAttributeForm): InternalAttributeForm = {
    InternalAttributeForm(
      name = original.name,
      value = Some(tpl.value.getOrElse(Json.obj()) ++ original.value.getOrElse(Json.obj())),
      description = original.description.orElse(tpl.description),
      deprecation = original.deprecation.orElse(tpl.deprecation)
    )
  }
}