package core.generator

import core.Text._
import core.Model

case class Play2Json(serviceName: String) {

  def generate(model: ScalaModel): String = {
    readers(model) + "\n\n" + writers(model)
  }

  def readers(model: ScalaModel): String = {
    Seq(
      s"implicit def jsonReads${serviceName}${model.name}: play.api.libs.json.Reads[${model.name}] = {",
      s"  import play.api.libs.json._",
      s"  import play.api.libs.functional.syntax._",
      fieldReaders(model).indent(2),
      s"}"
    ).mkString("\n")
  }

  private def fieldReaders(model: ScalaModel): String = {
    val serializations = model.fields.map { field =>
      if (field.multiple) {
        s"""(__ \\ "${field.originalName}").readNullable[scala.collection.Seq[${field.baseType.name}]].map(_.getOrElse(Nil))"""
      } else if (field.isOption) {
        s"""(__ \\ "${field.originalName}").readNullable[${field.baseType.name}]"""
      } else {
        s"""(__ \\ "${field.originalName}").read[${field.baseType.name}]"""
      }
    }

    if (serializations.size == 1) {
      val field = model.fields.head
      serializations.head + s""".map { x => new ${model.name}(${field.name} = x) }"""
    } else {
      Seq(
        "(",
        serializations.mkString(" and\n").indent(2),
        s")(${model.name}.apply _)"
      ).mkString("\n")
    }
  }

  def writers(model: ScalaModel): String = {
    if (model.fields.size == 1) {
      val field = model.fields.head
      Seq(
        s"implicit def jsonWrites${serviceName}${model.name}: play.api.libs.json.Writes[${model.name}] = new play.api.libs.json.Writes[${model.name}] {",
        s"  def writes(x: ${model.name}) = play.api.libs.json.Json.obj(",
        s"""    "${field.originalName}" -> play.api.libs.json.Json.toJson(x.${field.name})""",
        "  )",
        "}"
      ).mkString("\n")

    } else {
      Seq(
        s"implicit def jsonWrites${serviceName}${model.name}: play.api.libs.json.Writes[${model.name}] = {",
        s"  import play.api.libs.json._",
        s"  import play.api.libs.functional.syntax._",
        s"  (",
        model.fields.map { field =>
          s"""(__ \\ "${field.originalName}").write[${field.datatype.name}]"""
        }.mkString(" and\n").indent(4),
        s"  )(unlift(${model.name}.unapply _))",
        s"}"
      ).mkString("\n")
    }
  }

}
