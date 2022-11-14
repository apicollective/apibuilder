package builder.api_json.templates

import io.apibuilder.api.json.v0.models.Header

trait HeaderMerge extends AttributeMerge {
  private[this] val merger = new ArrayMerge[Header] {
    override def uniqueIdentifier(i: Header): String = i.name

    override def merge(original: Header, tpl: Header): Header = {
      Header(
        name = original.name,
        `type` = original.`type`,
        required = original.required,
        description = original.description.orElse(tpl.description),
        attributes = mergeAttributes(original.attributes, tpl.attributes),
        deprecation = original.deprecation.orElse(tpl.deprecation),
      )
    }
  }

  def mergeHeaders(original: Option[Seq[Header]], tpl: Option[Seq[Header]]): Option[Seq[Header]] = {
    merger.merge(original, tpl)
  }

}