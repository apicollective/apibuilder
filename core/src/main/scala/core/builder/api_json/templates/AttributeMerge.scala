package builder.api_json.templates

import io.apibuilder.api.json.v0.models.Attribute

trait AttributeMerge {
  private[this] val merger = new ArrayMerge[Attribute] {
    override def uniqueIdentifier(i: Attribute): String = i.name

    override def merge(original: Attribute, tpl: Attribute): Attribute = {
      Attribute(
        name = original.name,
        value = tpl.value ++ original.value
      )
    }
  }

  def mergeAttributes(original: Option[Seq[Attribute]], tpl: Option[Seq[Attribute]]): Option[Seq[Attribute]] = {
    merger.merge(original, tpl)
  }

}