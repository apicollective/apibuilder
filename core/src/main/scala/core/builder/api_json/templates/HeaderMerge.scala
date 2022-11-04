package builder.api_json.templates

import builder.api_json.InternalHeaderForm

trait HeaderMerge extends AttributeMerge {
  private[this] val merger = new ArrayMerge[InternalHeaderForm] {
    override def uniqueIdentifier(i: InternalHeaderForm): String = i.name.get

    override def merge(original: InternalHeaderForm, tpl: InternalHeaderForm): InternalHeaderForm = {
      InternalHeaderForm(
        name = original.name,
        datatype = original.datatype,
        required = original.required,
        description = original.description.orElse(tpl.description),
        deprecation = original.deprecation.orElse(tpl.deprecation),
        default = original.default.orElse(tpl.default),
        attributes = mergeAttributes(original.attributes, tpl.attributes),
        warnings = original.warnings ++ tpl.warnings
      )
    }
  }

  def mergeHeaders(original: Seq[InternalHeaderForm], tpl: Seq[InternalHeaderForm]): Seq[InternalHeaderForm] = {
    merger.merge(original, tpl)
  }

}