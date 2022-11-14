package builder.api_json.templates

import io.apibuilder.api.json.v0.models._
import lib.TextDatatype

case class Renaming(from: String, to: String)

/**
 * @param data we rename from key => value
 */
case class RenameTypes(data: Seq[Renaming]) {

  private[this] val byTypeName: Map[String, Seq[Renaming]] = data.groupBy(_.to)
  private[this] val singletons: Map[String, String] = data.groupBy(_.from).flatMap { case (from, to) =>
    to.toList match {
      case one :: Nil => Some(from -> one.to)
      case _ => None
    }
  }

  def rename(apiJson: ApiJson): ApiJson = {
    apiJson.copy(
      models = apiJson.models.map { case (t, o) =>
        t -> renameModel(t, o)
      },
      resources = apiJson.resources.map { case (t, o) =>
        t -> renameResource(t, o)
      },
      interfaces = apiJson.interfaces.map { case (t, o) =>
        t -> renameInterface(t, o)
      }
    )
  }

  private[this] def renameInterface(context: String, interface: Interface): Interface = {
    interface.copy(
      fields = interface.fields.map(_.map(renameField(context, _)))
    )
  }

  private[this] def renameResource(context: String, res: Resource): Resource = {
    res.copy(
      operations = res.operations.map(renameOperation(context, _))
    )
  }

  private[this] def renameOperation(context: String, op: Operation): Operation = {
    op.copy(
      body = op.body.map(renameBody(context, _)),
      parameters = op.parameters.map { p =>
        p.map(renameParameter(context, _))
      },
      responses = op.responses.map { r =>
        r.view.mapValues(renameResponse(context, _)).toMap
      }
    )
  }

  private[this] def renameModel(context: String, m: Model): Model = {
    m.copy(
      fields = m.fields.map(renameField(context, _))
    )
  }

  private[this] def renameBody(context: String, body: Body): Body = {
    body.copy(
      `type` = renameType(context, body.`type`)
    )
  }

  private[this] def renameResponse(context: String, response: Response): Response = {
    response.copy(
      `type` = renameType(context, response.`type`)
    )
  }
  private[this] def renameParameter(context: String, param: Parameter): Parameter = {
    param.copy(
      `type` = renameType(context, param.`type`)
    )
  }


  private[this] def renameField(context: String, field: Field): Field = {
    field.copy(
      `type` = renameType(context, field.`type`)
    )
  }

  private[this] def renameType(context: String, typ: String): String = {
    TextDatatype.label(
      TextDatatype.parse(typ).map {
        case TextDatatype.Map => TextDatatype.Map
        case TextDatatype.List => TextDatatype.List
        case TextDatatype.Singleton(name) => TextDatatype.Singleton(
          byTypeName.getOrElse(context, Nil).filter(_.from == name).map(_.to).distinct.toList match {
            case Nil => singletons.getOrElse(name, name)
            case one :: Nil => one
            case multiple => {
              sys.error(s"Multiple rename options for type[$typ] context[$context]: $multiple")
            }
          }
        )
      }
    )
  }
}