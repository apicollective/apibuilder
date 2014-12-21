package models

import com.gilt.apidocgenerator.models.Container
import lib.Text._
import generator.ScalaModel

case class Play2Json(serviceName: String) {

  def generate(model: ScalaModel): String = {
    readers(model) + "\n\n" + writers(model)
  }

  def readers(model: ScalaModel): String = {
    Seq(
      s"implicit def jsonReads${serviceName}${model.name}: play.api.libs.json.Reads[${model.name}] = {",
      fieldReaders(model).indent(2),
      s"}"
    ).mkString("\n")
  }

  def fieldReaders(model: ScalaModel): String = {
    val serializations = model.fields.map { field =>
      field.`type`.container match {
        case Container.Singleton => {
          if (field.isOption) {
            s"""(__ \\ "${field.originalName}").readNullable[${field.datatype.name}]"""
          } else {
            s"""(__ \\ "${field.originalName}").read[${field.datatype.name}]"""
          }
        }

        case c => {
          val nilValue = field.datatype.nilValue(field.`type`)
          s"""(__ \\ "${field.originalName}").readNullable[${field.datatype.name}].map(_.getOrElse($nilValue))"""
        }
      }
    }

    model.fields match {
      case field :: Nil => {
        serializations.head + s""".map { x => new ${model.name}(${field.name} = x) }"""
      }
      case fields => {
        Seq(
          "(",
          serializations.mkString(" and\n").indent(2),
          s")(${model.name}.apply _)"
        ).mkString("\n")
      }
    }
  }

  def writers(model: ScalaModel): String = {
    model.fields match {
      case field :: Nil => {
        Seq(
          s"implicit def jsonWrites${serviceName}${model.name}: play.api.libs.json.Writes[${model.name}] = new play.api.libs.json.Writes[${model.name}] {",
          s"  def writes(x: ${model.name}) = play.api.libs.json.Json.obj(",
          s"""    "${field.originalName}" -> play.api.libs.json.Json.toJson(x.${field.name})""",
          "  )",
          "}"
        ).mkString("\n")
      }

      case fields => {
        Seq(
          s"implicit def jsonWrites${serviceName}${model.name}: play.api.libs.json.Writes[${model.name}] = {",
          s"  (",
          model.fields.map { field =>
            if (field.isOption) {
              field.`type`.container match {
                case Container.Singleton => {
                  s"""(__ \\ "${field.originalName}").write[scala.Option[${field.datatype.name}]]"""
                }
                case Container.List | Container.Map | Container.Option | Container.Union => {
                  s"""(__ \\ "${field.originalName}").write[${field.datatype.name}]"""
                }
                case Container.UNDEFINED(container) => {
                  sys.error(s"Unknown container[$container]")
                }
              }
            } else {
              s"""(__ \\ "${field.originalName}").write[${field.datatype.name}]"""
            }
          }.mkString(" and\n").indent(4),
          s"  )(unlift(${model.name}.unapply _))",
          s"}"
        ).mkString("\n")
      }
    }
  }

}
