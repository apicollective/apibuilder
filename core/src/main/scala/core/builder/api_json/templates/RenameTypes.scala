package builder.api_json.templates

import io.apibuilder.api.json.v0.models._
import lib.TextDatatype

case class RenameTypes(from: String, to: String) {
  def rename(op: Operation): Operation = {
    op.copy(
      body = op.body.map(renameBody),
      parameters = op.parameters.map { p =>
        p.map(renameParameter)
      },
      responses = op.responses.map { r =>
        r.view.mapValues(renameResponse).toMap
      }
    )
  }

  private[this] def renameBody(body: Body): Body = {
    body.copy(
      `type` = renameType(body.`type`)
    )
  }

  private[this] def renameResponse(response: Response): Response = {
    response.copy(
      `type` = renameType(response.`type`)
    )
  }
  private[this] def renameParameter(param: Parameter): Parameter = {
    param.copy(
      `type` = renameType(param.`type`)
    )
  }

  private[this] def renameType(typ: String): String = {
    TextDatatype.label(
      TextDatatype.parse(typ).map {
        case TextDatatype.Map => TextDatatype.Map
        case TextDatatype.List => TextDatatype.List
        case TextDatatype.Singleton(name) => TextDatatype.Singleton(
          if (name == from) { to } else { name }
        )
      }
    )
  }
}