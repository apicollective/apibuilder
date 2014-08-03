package core.generator

import core.Text._
import core.Model

case class Play2Json(serviceName: String) {

  def generate(model: ScalaModel): String = {
    readers(model) + "\n" + writers(model)
  }

  private def fieldReaders(model: ScalaModel): String = {
    val serializations = model.fields.map { field =>
      val base = s"""(__ \\ "${field.originalName}").read%s[${field.baseType.name}]"""

      if (field.isOption) {
        base.format("Nullable")
      } else if (field.multiple) {
        base.format("Nullable") + ".map(_.getOrElse(Nil))"
      } else {
        base.format("")
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

  def readers(model: ScalaModel): String = {
    Seq(
      s"implicit def jsonReads${serviceName}${model.name}: play.api.libs.json.Reads[${model.name}] = {",
      s"  import play.api.libs.json._",
      s"  import play.api.libs.functional.syntax._",
      fieldReaders(model).indent(2),
      s"}"
    ).mkString("\n")
  }

  def writers(model: ScalaModel): String = {
    Seq(
      s"implicit def jsonWrites${serviceName}${model.name}: play.api.libs.json.Writes[${model.name}] = {",
      s"  import play.api.libs.json._",
      s"  import play.api.libs.functional.syntax._",
      s"  (",
      model.fields.map { field =>
        s"""    (__ \\ "${field.originalName}").write[${field.datatype.name}]"""
      }.mkString(" and\n"),
      s"  )(unlift(${model.name}.unapply _))",
      s"}"
    ).mkString("\n")
  }

}
