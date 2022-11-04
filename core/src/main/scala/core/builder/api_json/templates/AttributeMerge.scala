package builder.api_json.templates

import builder.api_json.InternalAttributeForm
import play.api.libs.json.Json

trait AttributeMerge {
  private[this] val merger = new ArrayMerge[InternalAttributeForm] {
    override def uniqueIdentifier(i: InternalAttributeForm): String = i.name.get

    override def merge(original: InternalAttributeForm, tpl: InternalAttributeForm): InternalAttributeForm = {
      InternalAttributeForm(
        name = original.name,
        value = Some(tpl.value.getOrElse(Json.obj()) ++ original.value.getOrElse(Json.obj())),
        description = original.description.orElse(tpl.description),
        deprecation = original.deprecation.orElse(tpl.deprecation)
      )
    }
  }

  def mergeAttributes(original: Seq[InternalAttributeForm], tpl: Seq[InternalAttributeForm]): Seq[InternalAttributeForm] = {
    merger.merge(original, tpl)
  }

}