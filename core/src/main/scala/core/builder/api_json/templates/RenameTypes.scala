package builder.api_json.templates

import io.apibuilder.api.json.v0.models._
import lib.TextDatatype

/**
 * @param data we rename from key => value
 */
case class RenameTypes(data: Map[String, String]) {
  def rename(apiJson: ApiJson): ApiJson = {
    apiJson.copy(
      models = apiJson.models.map { case (t, o) =>
        renameType(t) -> renameModel(o)
      },
      resources = apiJson.resources.map { case (t, o) =>
        renameType(t) -> renameResource(o)
      },
      interfaces = apiJson.interfaces.map { case (t, o) =>
        renameType(t) -> renameInterface(o)
      }
    )
  }

  private[this] def renameInterface(interface: Interface): Interface = {
    interface
  }

  private[this] def renameResource(res: Resource): Resource = {
    res.copy(
      operations = res.operations.map(renameOperation)
    )
  }

  private[this] def renameOperation(op: Operation): Operation = {
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

  private[this] def renameModel(m: Model): Model = {
    m.copy(
      fields = m.fields.map(renameField)
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


  private[this] def renameField(field: Field): Field = {
    field.copy(
      `type` = renameType(field.`type`)
    )
  }

  private[this] def renameType(typ: String): String = {
    TextDatatype.label(
      TextDatatype.parse(typ).map {
        case TextDatatype.Map => TextDatatype.Map
        case TextDatatype.List => TextDatatype.List
        case TextDatatype.Singleton(name) => TextDatatype.Singleton(
          data.getOrElse(name, name)
        )
      }
    )
  }
}